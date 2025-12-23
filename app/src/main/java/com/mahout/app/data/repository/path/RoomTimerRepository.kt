package com.mahout.app.data.repository.path

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.data.local.path.dao.TimerStateDao
import com.mahout.app.data.mapper.path.toDomain
import com.mahout.app.data.mapper.path.toEntity
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTimerRepository @Inject constructor(
    private val timerStateDao: TimerStateDao,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider
) : TimerRepository {

    override fun observeTimerState(): Flow<TimerState> {
        // Map null (no row) -> STOPPED state.
        return timerStateDao.observeTimerState()
            .map { entityOrNull ->
                entityOrNull?.toDomain() ?: TimerState.stopped(timeProvider.nowInstant())
            }
    }

    override suspend fun getTimerStateOrNull(): TimerState? = withContext(dispatchers.io) {
        timerStateDao.getTimerStateOrNull()?.toDomain()
    }

    override suspend fun upsert(state: TimerState) = withContext(dispatchers.io) {
        timerStateDao.upsert(state.toEntity())
    }

    override suspend fun clear() = withContext(dispatchers.io) {
        timerStateDao.clear()
    }
}
