package com.mahout.app.domain.path.repository

import com.mahout.app.domain.path.model.TimerState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the singleton Path timer state.
 *
 * We keep this separate from SessionRepository:
 * - SessionRepository is about many time logs.
 * - TimerRepository is about the *one* currently running/paused timer UX object.
 */
interface TimerRepository {

    /**
     * Observe timer state. Implementations MUST emit "stopped" when no row exists.
     */
    fun observeTimerState(): Flow<TimerState>

    /**
     * Snapshot (suspend) access to the timer row.
     * Returns null if no row exists (meaning STOPPED).
     */
    suspend fun getTimerStateOrNull(): TimerState?

    /**
     * Insert or replace the timer row.
     */
    suspend fun upsert(state: TimerState)

    /**
     * Clear the timer row (meaning STOPPED).
     */
    suspend fun clear()
}
