package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.Goal
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Links Actions <-> Goals with the constraint:
 * - each Action can have 0 or 1 ACTIVE goal link at a time
 *
 * NOTE:
 * We also support "unlink all actions for goal" for the Goal DELETE behavior.
 * This keeps past sessions intact because we only close link intervals.
 */
interface ActionGoalLinkRepository {

    /**
     * Observe the currently linked goal for an Action (or null if none).
     */
    fun observeGoalForAction(actionId: String): Flow<Goal?>

    /**
     * Set or clear the goal link for an Action.
     * Passing null means "unlink".
     */
    suspend fun setGoalForAction(actionId: String, goalId: String?)

    /**
     * Day 11 - Delete behavior:
     * When a Goal is deleted, we must unlink any actions that currently point to it.
     *
     * IMPORTANT:
     * This must NOT delete sessions. It only closes active link rows.
     */
    suspend fun unlinkActionsForGoal(goalId: String, unlinkedAt: Instant)
}
