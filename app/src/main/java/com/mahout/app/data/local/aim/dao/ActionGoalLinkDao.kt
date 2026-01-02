package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Action <-> Goal linking.
 *
 * Rows represent time intervals:
 * - linkedAt = start
 * - unlinkedAt = end (null means still active)
 */
@Dao
interface ActionGoalLinkDao {

    @Query(
        """
        SELECT * FROM action_goal_links
        WHERE actionId = :actionId
          AND unlinkedAt IS NULL
        LIMIT 1
        """
    )
    fun observeActiveLinkForAction(actionId: String): Flow<ActionGoalLinkEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActionGoalLinkEntity)

    @Query(
        """
        UPDATE action_goal_links
        SET unlinkedAt = :unlinkedAt
        WHERE actionId = :actionId
          AND unlinkedAt IS NULL
        """
    )
    suspend fun closeActiveLinkForAction(actionId: String, unlinkedAt: Instant)

    @Query(
        """
        UPDATE action_goal_links
        SET unlinkedAt = :unlinkedAt
        WHERE goalId = :goalId
          AND unlinkedAt IS NULL
        """
    )
    suspend fun closeActiveLinksForGoal(goalId: String, unlinkedAt: Instant)

    // ------------------------------
    // Aim Receipts additions
    // ------------------------------

    @Query(
        """
        SELECT * FROM action_goal_links
        WHERE unlinkedAt IS NULL
        """
    )
    fun observeActiveLinks(): Flow<List<ActionGoalLinkEntity>>

    @Query(
        """
        SELECT actionId FROM action_goal_links
        WHERE goalId = :goalId
          AND unlinkedAt IS NULL
        """
    )
    fun observeActiveActionIdsForGoal(goalId: String): Flow<List<String>>

    /**
     * Overlap condition for [from, to):
     * - linkedAt < to
     * - (unlinkedAt IS NULL OR unlinkedAt > from)
     */
    @Query(
        """
        SELECT * FROM action_goal_links
        WHERE linkedAt < :to
          AND (unlinkedAt IS NULL OR unlinkedAt > :from)
        """
    )
    suspend fun getLinksOverlapping(from: Instant, to: Instant): List<ActionGoalLinkEntity>

    @Query(
        """
        SELECT * FROM action_goal_links
        WHERE goalId = :goalId
          AND linkedAt < :to
          AND (unlinkedAt IS NULL OR unlinkedAt > :from)
        """
    )
    suspend fun getLinksOverlappingForGoal(
        goalId: String,
        from: Instant,
        to: Instant
    ): List<ActionGoalLinkEntity>
}
