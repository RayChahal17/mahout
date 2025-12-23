package com.mahout.app.data.repository.aim

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.data.local.aim.dao.ActionGoalLinkDao
import com.mahout.app.data.local.aim.dao.GoalDao
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import com.mahout.app.data.mapper.aim.toDomain
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation for Action <-> Goal linking.
 *
 * Key behavior:
 * - observeGoalForAction(actionId) reacts to BOTH:
 *   - link changes, and
 *   - goal changes
 */
@Singleton
class RoomActionGoalLinkRepository @Inject constructor(
    private val linkDao: ActionGoalLinkDao,
    private val goalDao: GoalDao,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ActionGoalLinkRepository {

    override fun observeGoalForAction(actionId: String): Flow<Goal?> {
        return linkDao.observeActiveLinkForAction(actionId)
            .flatMapLatest { link ->
                if (link == null) {
                    flowOf(null)
                } else {
                    goalDao.observeGoal(link.goalId)
                        .map { entity -> entity?.toDomain() }
                }
            }
    }

    override suspend fun setGoalForAction(actionId: String, goalId: String?) {
        withContext(dispatchers.io) {
            val now = timeProvider.nowInstant()

            // 1) close any existing active links for this action
            linkDao.closeActiveLinksForAction(actionId, now)

            // 2) open a new link interval if goalId provided
            if (goalId != null) {
                linkDao.insertLink(
                    ActionGoalLinkEntity(
                        linkId = idProvider.newId(),
                        actionId = actionId,
                        goalId = goalId,
                        linkedAt = now,
                        unlinkedAt = null
                    )
                )
            }
        }
    }

    override suspend fun unlinkActionsForGoal(goalId: String, unlinkedAt: Instant) {
        withContext(dispatchers.io) {
            linkDao.closeActiveLinksForGoal(goalId, unlinkedAt)
        }
    }
}
