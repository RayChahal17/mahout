package com.mahout.app.data.mapper.path

import com.mahout.app.data.local.path.entity.TimerStateEntity
import com.mahout.app.domain.path.model.TimerState

/**
 * Converts Room TimerStateEntity <-> domain TimerState.
 *
 * We keep mapping explicit so the rest of the app doesn't import Room entities.
 */
fun TimerStateEntity.toDomain(): TimerState = TimerState(
    status = status,
    actionId = actionId,
    currentSessionId = currentSessionId,
    accumulatedMillis = accumulatedMillis,
    updatedAt = updatedAt
)

fun TimerState.toEntity(): TimerStateEntity = TimerStateEntity(
    timerId = TimerStateEntity.TIMER_ID,
    status = status,
    actionId = actionId,
    currentSessionId = currentSessionId,
    accumulatedMillis = accumulatedMillis,
    updatedAt = updatedAt
)
