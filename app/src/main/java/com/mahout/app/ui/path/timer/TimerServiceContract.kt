package com.mahout.app.ui.path.timer

/**
 * Contract shared between:
 * - UI (PathFragment)
 * - Foreground service
 * - Notification action buttons
 */
object TimerServiceContract {

    // Service actions
    const val ACTION_START = "com.mahout.app.timer.action.START"
    const val ACTION_PAUSE = "com.mahout.app.timer.action.PAUSE"
    const val ACTION_RESUME = "com.mahout.app.timer.action.RESUME"
    const val ACTION_STOP = "com.mahout.app.timer.action.STOP"

    /**
     * ✅ New: UI can always send a single command:
     * - If timer is stopped -> start for provided actionId
     * - If timer is running/paused -> stop current timer
     */
    const val ACTION_TOGGLE = "com.mahout.app.timer.action.TOGGLE"

    // Extras
    const val EXTRA_ACTION_ID = "com.mahout.app.timer.extra.ACTION_ID"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "mahout_timer"
    const val NOTIFICATION_CHANNEL_NAME = "Timer"
    const val NOTIFICATION_ID = 1001
}
