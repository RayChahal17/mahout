package com.mahout.app.ui.path.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mahout.app.R
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.usecase.timer.ObserveTimerStateUseCase
import com.mahout.app.domain.path.usecase.timer.PauseTimerUseCase
import com.mahout.app.domain.path.usecase.timer.ResumeTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StartTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StopTimerUseCase
import com.mahout.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : android.app.Service() {

    companion object {
        private const val TAG = "TimerFGS"
        private const val START_GRACE_MS = 2000L
        private const val NOTIFICATION_TICK_MS = 1000L
        private const val TARGET_REACHED_NOTIFICATION_ID = 9911
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
    private var latestTargetMinutes: Int? = null

    private var isInForeground: Boolean = false

    private var pendingStartActionId: String? = null
    private var pendingStartTimeoutJob: Job? = null

    // Drives progress bar updates while RUNNING
    private var notificationTickerJob: Job? = null

    // Prevent congrats spam (per-process lifetime)
    private var lastActionIdForCongrats: String? = null
    private var hasNotifiedTargetReached: Boolean = false

    // Makes notification recover after process death (state exists, but metadata not loaded yet)
    private var loadedMetaForActionId: String? = null
    private var loadMetaJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        notificationFactory = TimerNotificationFactory(this)
        ensureNotificationChannel()

        serviceScope.launch {
            observeTimerStateUseCase().collectLatest { state ->
                latestState = state
                Log.d(TAG, "TimerState update: status=${state.status} actionId=${state.actionId}")

                // Make sure we have title/target even after process death
                state.actionId?.let { ensureActionMetaLoaded(it) }

                // Reset congrats state when action changes or timer stops.
                val currentActionId = state.actionId
                if (state.status == TimerStatus.STOPPED || currentActionId == null) {
                    lastActionIdForCongrats = null
                    hasNotifiedTargetReached = false
                } else if (lastActionIdForCongrats != currentActionId) {
                    lastActionIdForCongrats = currentActionId
                    hasNotifiedTargetReached = false
                }

                when (state.status) {
                    TimerStatus.RUNNING -> {
                        pendingStartActionId = null
                        pendingStartTimeoutJob?.cancel()
                        pendingStartTimeoutJob = null

                        ensureForeground(state)
                        startNotificationTicker()
                        // also check here (and in ticker)
                        maybeNotifyTargetReached(state)
                        postNotificationUpdate(state)
                    }

                    TimerStatus.PAUSED -> {
                        pendingStartActionId = null
                        pendingStartTimeoutJob?.cancel()
                        pendingStartTimeoutJob = null

                        ensureForeground(state)
                        stopNotificationTicker()
                        maybeNotifyTargetReached(state)
                        postNotificationUpdate(state)
                    }

                    TimerStatus.STOPPED -> {
                        stopNotificationTicker()

                        if (pendingStartActionId != null) {
                            Log.d(TAG, "Ignoring STOPPED because start is pending for actionId=$pendingStartActionId")
                            ensureForeground(state)
                            postNotificationUpdate(state)
                            return@collectLatest
                        }

                        stopForegroundCompat()
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action extras=${intent?.extras}")

        when (action) {
            TimerServiceContract.ACTION_TOGGLE -> {
                val current = latestState
                if (current != null && current.status != TimerStatus.STOPPED) {
                    Log.d(TAG, "TOGGLE -> stopTimerUseCase()")
                    serviceScope.launch {
                        runCatching { stopTimerUseCase() }
                            .onFailure { Log.e(TAG, "stop failed", it) }
                    }
                    return START_STICKY
                }

                val actionId = intent.getStringExtra(TimerServiceContract.EXTRA_ACTION_ID)
                if (actionId.isNullOrBlank()) {
                    Log.e(TAG, "TOGGLE requested start but EXTRA_ACTION_ID missing")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startWithGrace(actionId)
            }

            TimerServiceContract.ACTION_PAUSE -> {
                Log.d(TAG, "PAUSE")
                serviceScope.launch {
                    runCatching { pauseTimerUseCase() }
                        .onFailure { Log.e(TAG, "pause failed", it) }
                }
            }

            TimerServiceContract.ACTION_RESUME -> {
                Log.d(TAG, "RESUME")
                serviceScope.launch {
                    runCatching { resumeTimerUseCase() }
                        .onFailure { Log.e(TAG, "resume failed", it) }
                }
            }

            TimerServiceContract.ACTION_STOP -> {
                Log.d(TAG, "STOP")
                serviceScope.launch {
                    runCatching { stopTimerUseCase() }
                        .onFailure { Log.e(TAG, "stop failed", it) }
                }
            }

            TimerServiceContract.ACTION_START -> {
                val actionId = intent.getStringExtra(TimerServiceContract.EXTRA_ACTION_ID)
                if (actionId.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startWithGrace(actionId)
            }

            else -> {
                val state = latestState
                if (state == null || state.status == TimerStatus.STOPPED) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureForeground(state)
                postNotificationUpdate(state)
            }
        }

        return START_STICKY
    }

    private fun startWithGrace(actionId: String) {
        Log.d(TAG, "Start requested for actionId=$actionId")

        pendingStartActionId = actionId

        pendingStartTimeoutJob?.cancel()
        pendingStartTimeoutJob = serviceScope.launch {
            delay(START_GRACE_MS)
            val state = latestState
            val stillStopped = (state == null || state.status == TimerStatus.STOPPED)
            if (stillStopped && pendingStartActionId == actionId) {
                Log.e(TAG, "Start grace expired: still STOPPED, stopping service.")
                pendingStartActionId = null
                stopForegroundCompat()
                stopSelf()
            }
        }

        // Placeholder so Android doesn't kill the service before DB emits.
        val placeholder = createPlaceholderState(TimerStatus.RUNNING, actionId)
        ensureForeground(placeholder)
        postNotificationUpdate(placeholder)

        // Load action metadata early (title/target for progress)
        ensureActionMetaLoaded(actionId, force = true)

        serviceScope.launch {
            try {
                Log.d(TAG, "Calling startTimerUseCase(actionId=$actionId)")
                startTimerUseCase(actionId)
                Log.d(TAG, "startTimerUseCase returned normally")
            } catch (t: Throwable) {
                Log.e(TAG, "startTimerUseCase failed", t)
            }
        }
    }

    private fun ensureActionMetaLoaded(actionId: String, force: Boolean = false) {
        if (!force && loadedMetaForActionId == actionId && (latestActionTitle != null || latestTargetMinutes != null)) return
        if (loadMetaJob?.isActive == true) return

        loadMetaJob = serviceScope.launch {
            try {
                val action = withContext(Dispatchers.IO) { actionRepository.getAction(actionId) }
                latestActionTitle = action?.title
                latestTargetMinutes = action?.targetValue?.toInt()
                loadedMetaForActionId = actionId
                // Push an update so progress bar appears ASAP after metadata arrives
                latestState?.let { postNotificationUpdate(it) }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load action meta for $actionId", t)
            }
        }
    }

    /**
     * Notification progress bar needs explicit notify() calls to animate.
     * We run a lightweight ticker only while RUNNING.
     *
     * Also: congrats needs to be checked while time passes (state may not emit).
     */
    private fun startNotificationTicker() {
        if (notificationTickerJob?.isActive == true) return

        notificationTickerJob = serviceScope.launch {
            while (isActive) {
                delay(NOTIFICATION_TICK_MS)
                val s = latestState ?: continue
                if (s.status != TimerStatus.RUNNING) break
                postNotificationUpdate(s)
                maybeNotifyTargetReached(s)
            }
        }
    }

    private fun stopNotificationTicker() {
        notificationTickerJob?.cancel()
        notificationTickerJob = null
    }

    private fun maybeNotifyTargetReached(state: TimerState) {
        val actionId = state.actionId ?: return
        val targetMin = (latestTargetMinutes ?: 0)
        if (targetMin <= 0) return
        if (hasNotifiedTargetReached) return
        if (lastActionIdForCongrats != actionId) return

        val elapsedMs = computeElapsedMs(state)
        val targetMs = targetMin * 60_000L

        if (elapsedMs >= targetMs) {
            hasNotifiedTargetReached = true
            showTargetReachedNotification(
                title = latestActionTitle ?: "Action",
                overMs = (elapsedMs - targetMs).coerceAtLeast(0L)
            )
        }
    }

    private fun showTargetReachedNotification(title: String, overMs: Long) {
        if (!canPostNotificationsInline()) return

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = if (overMs > 0L) {
            "Target hit for $title. Extra: ${formatElapsed(overMs)}"
        } else {
            "Target hit for $title."
        }

        val notif = NotificationCompat.Builder(this, TimerServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Target achieved 🎉")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        safeNotify(TARGET_REACHED_NOTIFICATION_ID, notif)
    }

    private fun computeElapsedMs(state: TimerState): Long {
        val base = state.accumulatedMillis.coerceAtLeast(0L)
        return if (state.status == TimerStatus.RUNNING) {
            val now = System.currentTimeMillis()
            val last = state.updatedAt.toEpochMilli()
            base + (now - last).coerceAtLeast(0L)
        } else {
            base
        }
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        stopNotificationTicker()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createPlaceholderState(status: TimerStatus, actionId: String): TimerState {
        return TimerState(
            status = status,
            actionId = actionId,
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

        val notification = notificationFactory.build(
            state = state,
            actionTitle = latestActionTitle,
            targetMinutes = latestTargetMinutes
        )

        try {
            startForegroundCompat(TimerServiceContract.NOTIFICATION_ID, notification)
            isInForeground = true
            Log.d(TAG, "Entered foreground")
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            stopSelf()
        }
    }


    /**
     * For targetSdk 34+ (you’re on 36), you must specify a foreground service TYPE.
     * Ensure Manifest service has android:foregroundServiceType="dataSync"
     * and you have permissions:
     * - android.permission.FOREGROUND_SERVICE
     * - android.permission.FOREGROUND_SERVICE_DATA_SYNC
     */
    private fun startForegroundCompat(notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            // MUST match AndroidManifest.xml foregroundServiceType="dataSync"
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(notificationId, notification)
        }
    }


    private fun postNotificationUpdate(state: TimerState) {
        if (!canPostNotificationsInline()) return

        val notification = notificationFactory.build(
            state = state,
            actionTitle = latestActionTitle,
            targetMinutes = latestTargetMinutes
        )

        safeNotify(TimerServiceContract.NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission") // we check permission inline before notify()
    private fun safeNotify(id: Int, notification: Notification) {
        if (!canPostNotificationsInline()) return
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (se: SecurityException) {
            Log.e(TAG, "notify() SecurityException (POST_NOTIFICATIONS denied?)", se)
        } catch (t: Throwable) {
            Log.e(TAG, "notify() failed", t)
        }
    }

    /**
     * Inline check so Lint is happy (it doesn't trust helper methods).
     */
    private fun canPostNotificationsInline(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false

        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
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
