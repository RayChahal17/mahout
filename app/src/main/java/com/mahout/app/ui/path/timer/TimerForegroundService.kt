package com.mahout.app.ui.path.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.usecase.timer.ObserveTimerStateUseCase
import com.mahout.app.domain.path.usecase.timer.PauseTimerUseCase
import com.mahout.app.domain.path.usecase.timer.ResumeTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StartTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StopTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Foreground timer service.
 *
 * IMPORTANT (targetSdk 36):
 * - Manifest must declare android:foregroundServiceType="dataSync"
 * - Code must call startForeground(id, notification, TYPE_...) on API 29+
 *
 * IMPORTANT (bug fix):
 * - TimerState flow emits STOPPED immediately by default.
 * - If we stopSelf() on that initial STOPPED, the service dies before StartTimerUseCase updates state.
 * - We use a "start grace window" to prevent this race.
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    companion object {
        private const val TAG = "TimerFGS"

        /**
         * How long we wait after a START/TOGGLE-start before deciding "start failed".
         * If timer state is still STOPPED after this, we stop the service.
         */
        private const val START_GRACE_MS = 2000L
    }

    @Inject lateinit var startTimerUseCase: StartTimerUseCase
    @Inject lateinit var pauseTimerUseCase: PauseTimerUseCase
    @Inject lateinit var resumeTimerUseCase: ResumeTimerUseCase
    @Inject lateinit var stopTimerUseCase: StopTimerUseCase
    @Inject lateinit var observeTimerStateUseCase: ObserveTimerStateUseCase
    @Inject lateinit var actionRepository: ActionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var notificationFactory: TimerNotificationFactory

    private var latestState: TimerState? = null
    private var latestActionTitle: String? = null
    private var isInForeground: Boolean = false

    /**
     * If not null, we are actively trying to start a timer for this actionId.
     * During this period we ignore the initial STOPPED emission.
     */
    private var pendingStartActionId: String? = null

    /**
     * Timeout job that will stop the service if start never succeeds.
     */
    private var pendingStartTimeoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        notificationFactory = TimerNotificationFactory(this)
        ensureNotificationChannel()

        // Collect timer state and keep notification in sync.
        serviceScope.launch {
            observeTimerStateUseCase().collectLatest { state ->
                latestState = state
                Log.d(TAG, "TimerState update: status=${state.status} actionId=${state.actionId}")

                when (state.status) {
                    TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                        // ✅ Start succeeded (or timer is active). Cancel any pending start guard.
                        pendingStartActionId = null
                        pendingStartTimeoutJob?.cancel()
                        pendingStartTimeoutJob = null

                        ensureForeground(state)
                        postNotificationUpdate(state)
                    }

                    TimerStatus.STOPPED -> {
                        // 🚨 Bug fix: STOPPED is the default initial state.
                        // If we stopSelf() immediately here, the service dies before StartTimerUseCase updates state.
                        if (pendingStartActionId != null) {
                            Log.d(
                                TAG,
                                "Ignoring STOPPED because start is pending for actionId=$pendingStartActionId"
                            )

                            // Keep showing a notification (placeholder) while start is pending.
                            ensureForeground(state)
                            postNotificationUpdate(state)
                            return@collectLatest
                        }

                        // Normal STOPPED when nothing is pending -> shut down service.
                        stopForegroundCompat()
                        stopSelf()
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action extras=${intent?.extras}")

        when (action) {

            TimerServiceContract.ACTION_TOGGLE -> {
                val current = latestState

                // If running or paused -> stop
                if (current != null && current.status != TimerStatus.STOPPED) {
                    Log.d(TAG, "TOGGLE -> stopTimerUseCase()")
                    serviceScope.launch {
                        try {
                            stopTimerUseCase()
                        } catch (t: Throwable) {
                            Log.e(TAG, "stopTimerUseCase failed", t)
                        }
                    }
                    return START_STICKY
                }

                // If stopped -> start (requires actionId)
                val actionId = intent.getStringExtra(TimerServiceContract.EXTRA_ACTION_ID)
                if (actionId.isNullOrBlank()) {
                    Log.e(TAG, "TOGGLE requested start but EXTRA_ACTION_ID was missing")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startWithGrace(actionId)
            }

            TimerServiceContract.ACTION_PAUSE -> {
                Log.d(TAG, "PAUSE")
                serviceScope.launch {
                    try {
                        pauseTimerUseCase()
                    } catch (t: Throwable) {
                        Log.e(TAG, "pauseTimerUseCase failed", t)
                    }
                }
            }

            TimerServiceContract.ACTION_RESUME -> {
                Log.d(TAG, "RESUME")
                serviceScope.launch {
                    try {
                        resumeTimerUseCase()
                    } catch (t: Throwable) {
                        Log.e(TAG, "resumeTimerUseCase failed", t)
                    }
                }
            }

            TimerServiceContract.ACTION_STOP -> {
                Log.d(TAG, "STOP")
                serviceScope.launch {
                    try {
                        stopTimerUseCase()
                    } catch (t: Throwable) {
                        Log.e(TAG, "stopTimerUseCase failed", t)
                    }
                }
            }

            TimerServiceContract.ACTION_START -> {
                // Compatibility path (UI should prefer TOGGLE)
                val actionId = intent.getStringExtra(TimerServiceContract.EXTRA_ACTION_ID)
                if (actionId.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startWithGrace(actionId)
            }

            else -> {
                // system restart case
                val state = latestState
                if (state == null || state.status == TimerStatus.STOPPED) {
                    stopSelf()
                    return START_NOT_STICKY
                } else {
                    ensureForeground(state)
                    postNotificationUpdate(state)
                }
            }
        }

        return START_STICKY
    }

    /**
     * Starts a timer safely, without the race where STOPPED initial emission kills the service.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun startWithGrace(actionId: String) {
        Log.d(TAG, "Start requested for actionId=$actionId")

        // Mark start as pending so STOPPED doesn't immediately kill us.
        pendingStartActionId = actionId

        // Cancel any previous start attempt timeout and schedule a new one.
        pendingStartTimeoutJob?.cancel()
        pendingStartTimeoutJob = serviceScope.launch {
            delay(START_GRACE_MS)

            val state = latestState
            val stillStopped = (state == null || state.status == TimerStatus.STOPPED)

            if (stillStopped && pendingStartActionId == actionId) {
                Log.e(TAG, "Start grace expired: timer still STOPPED. Stopping service.")
                pendingStartActionId = null
                stopForegroundCompat()
                stopSelf()
            }
        }

        // Enter foreground immediately with placeholder so Android doesn't kill us.
        val placeholder = createPlaceholderState(
            status = TimerStatus.RUNNING,
            actionId = actionId
        )
        ensureForeground(placeholder)
        postNotificationUpdate(placeholder)

        // Do the real start work in coroutine.
        serviceScope.launch {
            try {
                // Get title for notification (IO)
                latestActionTitle = withContext(Dispatchers.IO) {
                    actionRepository.getAction(actionId)?.title
                }

                Log.d(TAG, "Calling startTimerUseCase(actionId=$actionId)")
                startTimerUseCase(actionId)
                Log.d(TAG, "startTimerUseCase returned normally")
            } catch (t: Throwable) {
                // If start fails silently, we'd otherwise wait until grace timeout.
                Log.e(TAG, "startTimerUseCase failed", t)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createPlaceholderState(status: TimerStatus, actionId: String): TimerState {
        return TimerState(
            status = status,
            actionId = actionId,
            currentSessionId = null,
            accumulatedMillis = 0L,
            updatedAt = Instant.now()
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            TimerServiceContract.NOTIFICATION_CHANNEL_ID,
            TimerServiceContract.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        mgr.createNotificationChannel(channel)
    }

    private fun ensureForeground(state: TimerState) {
        if (isInForeground) return
        val notification = notificationFactory.build(state, latestActionTitle)

        try {
            startForegroundCompat(notification)
            isInForeground = true
            Log.d(TAG, "Entered foreground")
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            stopSelf()
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TimerServiceContract.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(TimerServiceContract.NOTIFICATION_ID, notification)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postNotificationUpdate(state: TimerState) {
        val notification = notificationFactory.build(state, latestActionTitle)
        try {
            NotificationManagerCompat.from(this).notify(
                TimerServiceContract.NOTIFICATION_ID,
                notification
            )
        } catch (t: Throwable) {
            Log.e(TAG, "notify() failed", t)
        }
    }

    private fun stopForegroundCompat() {
        isInForeground = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
