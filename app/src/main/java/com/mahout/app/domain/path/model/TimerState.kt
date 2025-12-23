package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * Domain model for the singleton Path timer.
 *
 * Notes:
 * - We allow only ONE timer at a time in V1.
 * - We purposely store `accumulatedMillis` so Pause/Resume can work without a schema change
 *   to the Sessions table.
 *
 * How "Pause" works in V1:
 * - Each RUNNING segment is stored as a Session row with endAt=null (in-progress).
 * - When paused/stopped, we finalize that session (set endAt + durationMillis).
 * - `accumulatedMillis` keeps the sum of finalized segments in the current "run" so UI can
 *   show the total elapsed time even while the user is paused.
 *
 * In V1, a "run" may generate multiple Session rows (one per RUNNING segment).
 * That is acceptable and keeps persistence simple and reliable.
 */
data class TimerState(
    val status: TimerStatus,
    val actionId: String?,
    val currentSessionId: String?, // non-null only while RUNNING
    val accumulatedMillis: Long,
    val updatedAt: Instant
) {
    companion object {
        fun stopped(now: Instant): TimerState = TimerState(
            status = TimerStatus.STOPPED,
            actionId = null,
            currentSessionId = null,
            accumulatedMillis = 0L,
            updatedAt = now
        )
    }
}
