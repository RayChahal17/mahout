package com.mahout.app.ui.path.timer

/**
 * Single source of truth for TimerForegroundService intent actions + extras.
 *
 * Why?
 * - Avoid typos across notification PendingIntents and UI code.
 * - Central place to change if we rename.
 */
object TimerServiceContract {

    // Service actions (Intent.action)
    const val ACTION_START = "com.mahout.app.timer.ACTION_START"
    const val ACTION_PAUSE = "com.mahout.app.timer.ACTION_PAUSE"
    const val ACTION_RESUME = "com.mahout.app.timer.ACTION_RESUME"
    const val ACTION_STOP = "com.mahout.app.timer.ACTION_STOP"

    // Intent extras
    const val EXTRA_ACTION_ID = "extra_action_id"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "path_timer"
    const val NOTIFICATION_ID = 1001
}
