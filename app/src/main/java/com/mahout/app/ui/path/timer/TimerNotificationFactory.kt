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
 * Fixes:
 * - Progress "line" visible immediately while RUNNING (never stuck at 0 for big targets).
 * - Smooth progress updates (second-based instead of coarse 0..1000 slices).
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(state.status != TimerStatus.STOPPED)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // ✅ Progress bar
        // Use SECONDS for smooth updates, and never be "0" while running so it's visible immediately.
        if (targetMs > 0L && state.status != TimerStatus.STOPPED) {
            val targetSec = (targetMs / 1000L).coerceAtLeast(1L)
            val elapsedSec = (elapsedMs / 1000L).coerceAtLeast(0L)

            val max = targetSec.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val rawProgress = elapsedSec.coerceAtMost(targetSec).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            val progress = when {
                state.status == TimerStatus.RUNNING && rawProgress == 0 -> 1
                else -> rawProgress
            }

            builder.setProgress(max, progress.coerceIn(0, max), false)
        } else {
            builder.setProgress(0, 0, false)
        }

        when (state.status) {
            TimerStatus.RUNNING -> {
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
