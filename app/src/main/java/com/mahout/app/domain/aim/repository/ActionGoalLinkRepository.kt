package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.Goal
import kotlinx.coroutines.flow.Flow

/**
 * Handles linking actions to goals.
 *
 * Fast-ship rule:
 * - An Action may have 0 or 1 active Goal link.
 * - Switching goals closes the previous link interval and opens a new one.
 */
interface ActionGoalLinkRepository {
    fun observeGoalForAction(actionId: String): Flow<Goal?>

    /**
     * If goalId is null => action is not linked to any goal.
     */
    suspend fun setGoalForAction(actionId: String, goalId: String?)
}
