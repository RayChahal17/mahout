package com.mahout.app.data.repository.aim

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.data.local.aim.dao.ChiefAimDao
import com.mahout.app.data.local.aim.entity.ChiefAimEntity
import com.mahout.app.data.mapper.aim.toDomain
import com.mahout.app.domain.aim.model.ChiefAim
import com.mahout.app.domain.aim.repository.ChiefAimRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomChiefAimRepository @Inject constructor(
    private val dao: ChiefAimDao,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ChiefAimRepository {

    override fun observeChiefAim(): Flow<ChiefAim?> =
        dao.observeChiefAim().map { it?.toDomain() }

    override suspend fun upsert(title: String, description: String?, targetDate: LocalDate?) =
        withContext(dispatchers.io) {

            val now = timeProvider.nowInstant()

            // Preserve createdAt if record exists.
            val existing = dao.getChiefAim()
            val createdAt = existing?.createdAt ?: now

            dao.upsert(
                ChiefAimEntity(
                    id = 1,
                    title = title,
                    description = description,
                    targetDate = targetDate,
                    createdAt = createdAt,
                    updatedAt = now
                )
            )
        }
}
