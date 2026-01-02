package com.mahout.app.domain.path.model

data class TimerState(
    val status: TimerStatus,
    val actionId: String?,
    val elapsedText: String = "",
    val actionName: String = "",
    val progress: Int = 0,
    val accumulatedMillis: Long = 0,
    val updatedAt: java.time.Instant = java.time.Instant.now()
) {

    companion object {
        fun stopped(now: java.time.Instant) = TimerState(
            status = TimerStatus.STOPPED,
            actionId = null,
            elapsedText = "",
            actionName = "",
            progress = 0,
            accumulatedMillis = 0,
            updatedAt = now
        )
    }

    /** For quick access to timer condition */
    val isRunning get() = status == TimerStatus.RUNNING
    val isPaused get() = status == TimerStatus.PAUSED
}
