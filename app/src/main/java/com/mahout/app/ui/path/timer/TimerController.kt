package com.mahout.app.ui.path.timer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerController @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    sealed interface Result {
        data object Sent : Result
        data object NotificationsDisabled : Result
        data object NeedPostNotificationsPermission : Result
        data class Error(val throwable: Throwable) : Result
    }

    fun toggle(actionId: String): Result {
        return send(
            isForeground = true,
            action = TimerServiceContract.ACTION_TOGGLE
        ) {
            putExtra(TimerServiceContract.EXTRA_ACTION_ID, actionId)
        }
    }

    fun pause(): Result = send(false, TimerServiceContract.ACTION_PAUSE)

    fun resume(): Result = send(false, TimerServiceContract.ACTION_RESUME)

    fun stop(): Result = send(false, TimerServiceContract.ACTION_STOP)

    private fun send(
        isForeground: Boolean,
        action: String,
        fill: Intent.() -> Unit = {}
    ): Result {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return Result.NotificationsDisabled
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.NeedPostNotificationsPermission
        }

        return try {
            val intent = Intent(appContext, TimerForegroundService::class.java).apply {
                this.action = action
                fill()
            }

            if (isForeground) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.startService(intent)
            }

            Result.Sent
        } catch (t: Throwable) {
            Result.Error(t)
        }
    }
}
