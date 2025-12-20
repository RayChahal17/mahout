package com.mahout.app.data.repository.path

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.data.local.aim.dao.ActionDao
import com.mahout.app.data.mapper.path.toDomain
import com.mahout.app.data.mapper.path.toEntity
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.repository.ActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomActionRepository @Inject constructor(
    private val actionDao: ActionDao,
    private val dispatchers: DispatcherProvider
) : ActionRepository {

    override fun observeActiveActions(): Flow<List<Action>> {
        // Room emits on DB updates automatically. We just map entities -> domain.
        return actionDao.observeActiveActions()
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAction(actionId: String): Action? = withContext(dispatchers.io) {
        actionDao.getAction(actionId)?.toDomain()
    }

    override suspend fun upsert(action: Action) = withContext(dispatchers.io) {
        actionDao.upsert(action.toEntity())
    }

    override suspend fun archive(actionId: String, archivedAt: Instant) = withContext(dispatchers.io) {
        actionDao.archive(actionId, archivedAt)
    }
}
