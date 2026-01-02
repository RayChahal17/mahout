package com.mahout.app.data.repository.aim

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.data.local.aim.dao.ActionGoalLinkDao
import com.mahout.app.data.local.aim.dao.GoalDao
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import com.mahout.app.data.mapper.aim.toDomain
import com.mahout.app.domain.aim.model.ActionGoalLinkInterval
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RoomActionGoalLinkRepository @Inject constructor(
    private val actionGoalLinkDao: ActionGoalLinkDao,
    private val goalDao: GoalDao,
    private val timeProvider: TimeProvider
) : ActionGoalLinkRepository {

    override fun observeGoalForAction(actionId: String): Flow<Goal?> {
        return actionGoalLinkDao.observeActiveLinkForAction(actionId)
            .flatMapLatest { linkEntity ->
                val goalId = linkEntity?.goalId ?: return@flatMapLatest flowOf(null)
                goalDao.observeGoal(goalId).map { it?.toDomain() }
            }
    }

    override suspend fun setGoalForAction(actionId: String, goalId: String?) {
        val now = timeProvider.nowInstant()

        // Close any existing active link interval for this action
        actionGoalLinkDao.closeActiveLinkForAction(actionId, now)

        // If linking to a goal, insert new interval row
        if (goalId != null) {
            actionGoalLinkDao.insert(
                ActionGoalLinkEntity(
                    linkId = UUID.randomUUID().toString(),
                    actionId = actionId,
                    goalId = goalId,
                    linkedAt = now,
                    unlinkedAt = null
                )
            )
        }
    }

    override suspend fun unlinkActionsForGoal(goalId: String, unlinkedAt: Instant) {
        actionGoalLinkDao.closeActiveLinksForGoal(goalId, unlinkedAt)
    }

    // ------------------------------
    // Aim Receipts additions
    // ------------------------------

    override fun observeActiveLinks(): Flow<List<ActionGoalLinkInterval>> {
        return actionGoalLinkDao.observeActiveLinks()
            .map { rows -> rows.map { it.toInterval() } }
    }

    override fun observeActiveActionIdsForGoal(goalId: String): Flow<List<String>> {
        return actionGoalLinkDao.observeActiveActionIdsForGoal(goalId)
    }

    override suspend fun getLinksOverlapping(from: Instant, to: Instant): List<ActionGoalLinkInterval> {
        return actionGoalLinkDao.getLinksOverlapping(from, to).map { it.toInterval() }
    }

    override suspend fun getLinksOverlappingForGoal(
        goalId: String,
        from: Instant,
        to: Instant
    ): List<ActionGoalLinkInterval> {
        return actionGoalLinkDao.getLinksOverlappingForGoal(goalId, from, to).map { it.toInterval() }
    }

    private fun ActionGoalLinkEntity.toInterval(): ActionGoalLinkInterval =
        ActionGoalLinkInterval(
            actionId = actionId,
            goalId = goalId,
            linkedAt = linkedAt,
            unlinkedAt = unlinkedAt
        )
}
