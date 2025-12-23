package com.mahout.app.data.local.path.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mahout.app.domain.path.model.TimerStatus
import java.time.Instant

/**
 * Room entity for the singleton timer state.
 *
 * NOTE:
 * We store ONE row only (timerId = "timer").
 * This is how the app can survive process death and still know if we're RUNNING/PAUSED.
 */
@Entity(tableName = "timer_state")
data class TimerStateEntity(
    @PrimaryKey val timerId: String = TIMER_ID,
    val status: TimerStatus,
    val actionId: String?,
    val currentSessionId: String?,
    val accumulatedMillis: Long,
    val updatedAt: Instant
) {
    companion object {
        const val TIMER_ID: String = "timer"
    }
}
