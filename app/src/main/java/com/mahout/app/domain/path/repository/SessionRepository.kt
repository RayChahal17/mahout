package com.mahout.app.domain.path.repository

import com.mahout.app.domain.path.model.Session
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SessionRepository {
    fun observeSessionsForAction(actionId: String): Flow<List<Session>>
    suspend fun getInProgressSession(): Session?
    suspend fun upsert(session: Session)
    suspend fun delete(sessionId: String)

    suspend fun getSessionsInRange(from: Instant, to: Instant): List<Session>
}
