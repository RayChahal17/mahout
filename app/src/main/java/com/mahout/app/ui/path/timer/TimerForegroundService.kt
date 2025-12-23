package com.mahout.app.ui.path.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mahout.app.R
import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.usecase.ObserveTimerStateUseCase
import com.mahout.app.domain.path.usecase.timer.PauseTimerUseCase
import com.mahout.app.domain.path.usecase.timer.ResumeTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StartTimerUseCase
import com.mahout.app.domain.path.usecase.timer.StopTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var dispatcherProvider: DispatcherProvider
    @Inject lateinit var timeProvider: TimeProvider

    @Inject lateinit var actionRepository: ActionRepository

    @Inject lateinit var observeTimerStateUseCase: ObserveTimerStateUseCase
    @Inject lateinit var startTimerUseCase: StartTimerUseCase
    @Inject lateinit var pauseTimerUseCase: PauseTimerUseCase
    @Inject lateinit var resumeTimerUseCase: ResumeTimerUseCase
    @Inject lateinit var stopTimerUseCase: StopTimerUseCase

    private val serviceJob: Job = SupervisorJob()
    private val serviceScope: CoroutineScope by lazy {
        CoroutineScope(serviceJob + dispatcherProvider.default)
    }

    private var tickerJob: Job? = null

    private var cachedActionId: String? = null
    private var cachedActionTitle: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            restoreFromPersistedState()
            return START_STICKY
        }

        when (intent.action) {
            TimerServiceContract.ACTION_START -> {
                val actionId = intent.getStringExtra(TimerServiceContract.EXTRA_ACTION_ID)
                if (actionId == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundCompat(buildNotificationPlaceholder())

                serviceScope.launch {
                    startTimerUseCase(actionId)
                    refreshNotificationOnce()
                    startTickerIfRunning()
                }
            }

            TimerServiceContract.ACTION_PAUSE -> {
                serviceScope.launch {
                    pauseTimerUseCase()
                    refreshNotificationOnce()
                    stopTicker()
                }
            }

            TimerServiceContract.ACTION_RESUME -> {
                serviceScope.launch {
                    resumeTimerUseCase()
                    refreshNotificationOnce()
                    startTickerIfRunning()
                }
            }

            TimerServiceContract.ACTION_STOP -> {
                serviceScope.launch {
                    stopTimerUseCase()
                    stopTicker()
                    stopForegroundCompat(removeNotification = true)
                    stopSelf()
                }
            }

            else -> restoreFromPersistedState()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopTicker()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restoreFromPersistedState() {
        serviceScope.launch {
            val state = observeTimerStateUseCase().first()

            if (state.status == TimerStatus.STOPPED) {
                stopForegroundCompat(removeNotification = true)
                stopSelf()
            } else {
                val actionTitle = resolveActionTitle(state.actionId)
                startForegroundCompat(buildNotificationForState(state, actionTitle))
                if (state.status == TimerStatus.RUNNING) startTickerIfRunning() else stopTicker()
            }
        }
    }

    private fun startTickerIfRunning() {
        stopTicker()
        tickerJob = serviceScope.launch {
            while (true) {
                refreshNotificationOnce()
                delay(1_000L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private suspend fun refreshNotificationOnce() {
        val state = observeTimerStateUseCase().first()
        val actionTitle = resolveActionTitle(state.actionId)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            TimerServiceContract.NOTIFICATION_ID,
            buildNotificationForState(state, actionTitle)
        )
    }

    private fun buildNotificationPlaceholder(): Notification {
        return NotificationCompat.Builder(this, TimerServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(getString(R.string.timer_notification_starting_title))
            .setContentText(getString(R.string.timer_notification_starting_body))
            .setOngoing(true)
            .build()
    }

    private fun buildNotificationForState(state: TimerState, actionTitle: String): Notification {
        val (title, body) = when (state.status) {
            TimerStatus.RUNNING ->
                getString(R.string.timer_notification_running_title, actionTitle) to formatElapsedText(state)
            TimerStatus.PAUSED ->
                getString(R.string.timer_notification_paused_title, actionTitle) to formatElapsedText(state)
            TimerStatus.STOPPED ->
                getString(R.string.timer_notification_stopped_title) to getString(R.string.timer_notification_stopped_body)
        }

        val builder = NotificationCompat.Builder(this, TimerServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(state.status != TimerStatus.STOPPED)
            .setOnlyAlertOnce(true)

        when (state.status) {
            TimerStatus.RUNNING -> {
                builder.addAction(0, getString(R.string.timer_action_pause), pendingIntentForAction(TimerServiceContract.ACTION_PAUSE))
                builder.addAction(0, getString(R.string.timer_action_stop), pendingIntentForAction(TimerServiceContract.ACTION_STOP))
            }
            TimerStatus.PAUSED -> {
                builder.addAction(0, getString(R.string.timer_action_resume), pendingIntentForAction(TimerServiceContract.ACTION_RESUME))
                builder.addAction(0, getString(R.string.timer_action_stop), pendingIntentForAction(TimerServiceContract.ACTION_STOP))
            }
            TimerStatus.STOPPED -> Unit
        }

        return builder.build()
    }

    private fun pendingIntentForAction(action: String): PendingIntent {
        return PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, TimerForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private suspend fun resolveActionTitle(actionId: String?): String {
        if (actionId.isNullOrBlank()) {
            cachedActionId = null
            cachedActionTitle = null
            return getString(R.string.timer_unknown_action)
        }

        if (actionId == cachedActionId && cachedActionTitle != null) {
            return cachedActionTitle!!
        }

        val title = runCatching { actionRepository.getAction(actionId)?.title }.getOrNull()
        val resolved = title?.takeIf { it.isNotBlank() } ?: getString(R.string.timer_unknown_action)

        cachedActionId = actionId
        cachedActionTitle = resolved
        return resolved
    }

    private fun formatElapsedText(state: TimerState): String {
        val now = timeProvider.nowInstant()
        val runningExtraMillis = if (state.status == TimerStatus.RUNNING) {
            (now.toEpochMilli() - state.updatedAt.toEpochMilli()).coerceAtLeast(0L)
        } else 0L

        val total = (state.accumulatedMillis + runningExtraMillis).coerceAtLeast(0L)
        val duration = Duration.ofMillis(total)

        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60)
        val seconds = (duration.seconds % 60)

        val formatted = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        return getString(R.string.timer_notification_elapsed, formatted)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TimerServiceContract.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(TimerServiceContract.NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            TimerServiceContract.NOTIFICATION_CHANNEL_ID,
            getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.timer_notification_channel_description)
        }

        manager.createNotificationChannel(channel)
    }
}
