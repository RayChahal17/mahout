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
 * Room-backed implementation of GoalRepository.
 *
 * Key rule: repo methods operate on DOMAIN models (Goal),
 * and delegate persistence to DAO using ENTITY models (GoalEntity).
 */
class RoomGoalRepository @Inject constructor(
    private val dao: GoalDao
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> {
        return dao.observeGoals()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeActiveGoals(): Flow<List<Goal>> {
        return dao.observeActiveGoals()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getGoal(goalId: String): Goal? {
        return dao.getById(goalId)?.toDomain()
    }

    override suspend fun upsert(goal: Goal) {
        dao.upsert(goal.toEntity())
    }

    /**
     * Backwards-compatible implementation:
     * ArchiveGoalUseCase calls this, so it MUST exist.
     */
    override suspend fun updateStatus(goalId: String, status: GoalStatus, updatedAt: Instant) {
        dao.updateStatus(
            goalId = goalId,
            newStatus = status,
            updatedAt = updatedAt
        )
    }

    /**
     * Convenience helper (optional for new code).
     * Uses the default implementation from interface, but you can also override if you prefer.
     */
    override suspend fun archiveGoal(goalId: String, updatedAt: Instant) {
        updateStatus(goalId, GoalStatus.ARCHIVED, updatedAt)
    }
}
