package com.mahout.app.domain.path.repository

import com.mahout.app.domain.path.model.Session
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface for persisted Sessions (time logs).
 *
 * IMPORTANT:
 * - Sessions are the durable source-of-truth for "how much time did I work?"
 * - TimerState is ONLY the current UX state (running/paused/stopped).
 */
interface SessionRepository {

    /**
     * Stream of ALL sessions for a given action (completed + in-progress).
     */
    fun observeSessionsForAction(actionId: String): Flow<List<Session>>

    /**
     * Returns the single in-progress session, if any.
     *
     * V1 assumption:
     * - Only ONE timer exists at a time, so there can only be one in-progress session.
     */
    suspend fun getInProgressSession(): Session?

    /**
     * Insert/update a session row.
     */
    suspend fun upsert(session: Session)

    /**
     * Delete a session by id.
     */
    suspend fun delete(sessionId: String)

    /**
     * Returns sessions in a time window.
     *
     * NOTE:
     * The exact semantics depend on your data layer implementation (DAO):
     * - Ideally: returns sessions that OVERLAP [from, to)
     * - Common implementation: sessions with startAt in [from, to)
     *
     * In our timer baseline logic we defensively clamp overlap durations,
     * and we also query with a small buffer for daily/weekly to catch sessions
     * that started slightly before the window.
     */
    suspend fun getSessionsInRange(from: Instant, to: Instant): List<Session>
}
