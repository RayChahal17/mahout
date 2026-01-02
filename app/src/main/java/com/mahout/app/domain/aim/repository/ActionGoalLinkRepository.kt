package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.ActionGoalLinkInterval
import com.mahout.app.domain.aim.model.Goal
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Links Actions <-> Goals with the constraint:
 * - each Action can have 0 or 1 ACTIVE goal link at a time
 *
 * IMPORTANT:
 * - We model links as time intervals (linkedAt .. unlinkedAt).
 * - Unlinking MUST NOT delete history. It only stops future attribution.
 */
interface ActionGoalLinkRepository {

    /**
     * Observe the currently ACTIVE goal for an action (null if unlinked).
     * Used mostly by Action edit screens / UI badges.
     */
    fun observeGoalForAction(actionId: String): Flow<Goal?>

    /**
     * Set or clear the goal link for an Action.
     * Passing null means "unlink".
     *
     * Implementation must:
     * - close any existing active link row for this action
     * - if goalId != null, insert a new active link row
     */
    suspend fun setGoalForAction(actionId: String, goalId: String?)

    /**
     * When a Goal is deleted, unlink any Actions currently pointing to it.
     * This must NOT delete sessions. It only closes active link intervals.
     */
    suspend fun unlinkActionsForGoal(goalId: String, unlinkedAt: Instant)

    // ------------------------------
    // Aim Receipts additions
    // ------------------------------

    /**
     * Observe ALL currently ACTIVE links (unlinkedAt == null).
     * Used for:
     * - building "linkable actions" lists
     * - showing linked summaries on Goal cards
     */
    fun observeActiveLinks(): Flow<List<ActionGoalLinkInterval>>

    /**
     * Observe actionIds CURRENTLY linked to this goal (active links only).
     * Used by Goal Detail "Linked actions" list.
     */
    fun observeActiveActionIdsForGoal(goalId: String): Flow<List<String>>

    /**
     * One-shot: get all link intervals that overlap [from, to).
     */
    suspend fun getLinksOverlapping(from: Instant, to: Instant): List<ActionGoalLinkInterval>

    /**
     * One-shot: get link intervals for a specific goal that overlap [from, to).
     * Used by Goal Detail receipts.
     */
    suspend fun getLinksOverlappingForGoal(
        goalId: String,
        from: Instant,
        to: Instant
    ): List<ActionGoalLinkInterval>
}
