package com.mahout.app.data.repository.aim

import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.data.local.aim.dao.ActionGoalLinkDao
import com.mahout.app.data.local.aim.dao.GoalDao
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomActionGoalLinkRepository @Inject constructor(
    private val linkDao: ActionGoalLinkDao,
    private val goalDao: GoalDao,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ActionGoalLinkRepository {

    override fun observeGoalForAction(actionId: String): Flow<Goal?> {
        // Observe the active link, then switch to observing the goal itself.
        return linkDao.observeActiveLinkForAction(actionId)
            .flatMapLatest { link ->
                if (link == null) {
                    flowOf(null)
                } else {
                    goalDao.observeGoal(link.goalId)
                        .map { goalEntity ->
                            // We map inline to avoid needing a separate mapper file right now.
                            goalEntity?.let {
                                Goal(
                                    goalId = it.goalId,
                                    title = it.title,
                                    why = it.why,
                                    horizon = it.horizon,
                                    status = it.status,
                                    priority = it.priority,
                                    targetDate = it.targetDate,
                                    parentGoalId = it.parentGoalId,
                                    createdAt = it.createdAt,
                                    updatedAt = it.updatedAt,
                                    deletedAt = it.deletedAt
                                )
                            }
                        }
                }
            }
    }

    override suspend fun setGoalForAction(actionId: String, goalId: String?) =
        withContext(dispatchers.io) {

            val now = timeProvider.nowInstant()

            // 1) Close any existing active link(s) for this action (enforces single active)
            linkDao.closeActiveLinksForAction(actionId, now)

            // 2) If goalId is provided, open a new link interval
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
