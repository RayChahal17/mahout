package com.mahout.app.data.repository.aim

import com.mahout.app.data.local.aim.dao.GoalDao
import com.mahout.app.data.mapper.aim.toDomain
import com.mahout.app.data.mapper.aim.toEntity
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed GoalRepository.
 */
class RoomGoalRepository @Inject constructor(
    private val dao: GoalDao
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> =
        dao.observeGoals().map { it.map { e -> e.toDomain() } }

    override fun observeActiveGoals(): Flow<List<Goal>> =
        dao.observeActiveGoals().map { it.map { e -> e.toDomain() } }

    override fun observeGoal(goalId: String): Flow<Goal?> =
        dao.observeGoal(goalId).map { it?.toDomain() }

    override suspend fun getGoal(goalId: String): Goal? =
        dao.getById(goalId)?.toDomain()

    override suspend fun upsert(goal: Goal) {
        dao.upsert(goal.toEntity())
    }

    override suspend fun updateStatus(goalId: String, status: GoalStatus, updatedAt: Instant) {
        dao.updateStatus(goalId = goalId, newStatus = status, updatedAt = updatedAt)
    }

    override suspend fun softDelete(goalId: String, deletedAt: Instant) {
        dao.softDelete(goalId, deletedAt)
    }
}
