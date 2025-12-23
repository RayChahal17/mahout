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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation for the Action<->Goal linking rule:
 * - Each Action can have 0 or 1 ACTIVE Goal link at a time.
 *
 * Important behavior:
 * - observeGoalForAction(actionId) is a Flow that updates if:
 *   - link changes (linked/unlinked), OR
 *   - the linked goal changes (title/horizon/status/etc)
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
        // Observe the active link row for this action.
        // Whenever the active link changes, switch (flatMapLatest) to observing the corresponding Goal.
        return linkDao.observeActiveLinkForAction(actionId)
            .flatMapLatest { link ->
                if (link == null) {
                    // No active link => no goal
                    flowOf(null)
                } else {
                    // Observe the linked goal entity as a Flow.
                    // If goal is deleted (deletedAt != null), DAO returns null.
                    goalDao.observeGoal(link.goalId)
                        .map { entity -> entity?.toDomain() }
                }
            }
    }

    override suspend fun setGoalForAction(actionId: String, goalId: String?) {
        withContext(dispatchers.io) {
            val now = timeProvider.nowInstant()

            // 1) Close any existing active links for this action (enforces "single active link").
            linkDao.closeActiveLinksForAction(actionId, now)

            // 2) If a new goalId is provided, open a new link interval.
            if (goalId != null) {
                val link = ActionGoalLinkEntity(
                    linkId = idProvider.newId(),
                    actionId = actionId,
                    goalId = goalId,
                    linkedAt = now,
                    unlinkedAt = null
                )
                linkDao.insertLink(link)
            }
        }
    }
}
