package com.mahout.app.ui.path.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mahout.app.R
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.ui.MainActivity
import java.util.concurrent.TimeUnit

/**
 * Foreground timer notification builder.
 *
 * Why your "progress line" only changed on pause/start:
 * - Notification progress bars update ONLY when we post a new notification (notify()).
 * - Your TimerState flow emits only on state changes, not every second.
 *
 * Fix is in TimerForegroundService: a lightweight ticker calls notify() every second while RUNNING.
 */
class TimerNotificationFactory(
    private val context: Context
) {

    fun build(
        state: TimerState,
        actionTitle: String?,
        targetMinutes: Int?
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = actionTitle ?: "Timer"
        val elapsedMs = computeElapsedMs(state)

        val targetMin = (targetMinutes ?: 0).coerceAtLeast(0)
        val targetMs = if (targetMin > 0) TimeUnit.MINUTES.toMillis(targetMin.toLong()) else 0L

        val contentText = buildProgressText(elapsedMs, targetMs)

        val builder = NotificationCompat.Builder(context, TimerServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // always exists
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(state.status != TimerStatus.STOPPED)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // ✅ Progress "line" (Notification progress bar)
        // - Before target: grows from 0 -> 100%
        // - After target: stays full, while we show (+extra) in text
        if (targetMs > 0L) {
            val max = 1000
            val progress = ((elapsedMs.toDouble() / targetMs.toDouble()) * max)
                .toInt()
                .coerceIn(0, max)
            builder.setProgress(max, progress, false)
        } else {
            // No target => no progress bar
            builder.setProgress(0, 0, false)
        }

        when (state.status) {
            TimerStatus.RUNNING -> {
                // ✅ Chronometer ticks without notify() calls.
                // We still call notify() periodically to update the progress bar.
                builder
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis() - elapsedMs)
                    .setUsesChronometer(true)

                builder.addAction(0, "Pause", servicePendingIntent(TimerServiceContract.ACTION_PAUSE))
                builder.addAction(0, "Stop", servicePendingIntent(TimerServiceContract.ACTION_STOP))
            }

            TimerStatus.PAUSED -> {
                builder
                    .setUsesChronometer(false)
                    .setShowWhen(false)

                builder.addAction(0, "Resume", servicePendingIntent(TimerServiceContract.ACTION_RESUME))
                builder.addAction(0, "Stop", servicePendingIntent(TimerServiceContract.ACTION_STOP))
            }

            TimerStatus.STOPPED -> {
                builder
                    .setUsesChronometer(false)
                    .setShowWhen(false)
                    .setContentText("Stopped")
                    .setProgress(0, 0, false)
            }
        }

        return builder.build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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

    private fun buildProgressText(elapsedMs: Long, targetMs: Long): String {
        val elapsedStr = formatElapsed(elapsedMs)
        if (targetMs <= 0L) return elapsedStr

        val targetStr = formatElapsed(targetMs)
        return if (elapsedMs < targetMs) {
            "$elapsedStr / $targetStr"
        } else {
            val over = (elapsedMs - targetMs).coerceAtLeast(0L)
            "$elapsedStr / $targetStr (+${formatElapsed(over)})"
        }
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }
}
