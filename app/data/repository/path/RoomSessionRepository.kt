package com.mahout.app.data.repository.path

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.data.local.path.dao.SessionDao
import com.mahout.app.data.mapper.path.toDomain
import com.mahout.app.data.mapper.path.toEntity
import com.mahout.app.domain.path.model.Session
import com.mahout.app.domain.path.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val dispatchers: DispatcherProvider
) : SessionRepository {

    override fun observeSessionsForAction(actionId: String): Flow<List<Session>> {
        return sessionDao.observeSessionsForAction(actionId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getInProgressSession(): Session? = withContext(dispatchers.io) {
        sessionDao.getInProgressSession()?.toDomain()
    }

    override suspend fun upsert(session: Session) = withContext(dispatchers.io) {
        sessionDao.upsert(session.toEntity())
    }

    override suspend fun delete(sessionId: String) = withContext(dispatchers.io) {
        sessionDao.delete(sessionId)
    }

    override suspend fun getSessionsInRange(from: Instant, to: Instant): List<Session> =
        withContext(dispatchers.io) {
            sessionDao.getSessionsInRange(from, to).map { it.toDomain() }
        }
}
