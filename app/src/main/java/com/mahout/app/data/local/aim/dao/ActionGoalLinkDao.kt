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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(entity: ActionGoalLinkEntity)

    /**
     * Close any active link rows for an action (enforces single active link).
     */
    @Query(
        """
        UPDATE action_goal_links
        SET unlinkedAt = :unlinkedAt
        WHERE actionId = :actionId
          AND unlinkedAt IS NULL
        """
    )
    suspend fun closeActiveLinksForAction(actionId: String, unlinkedAt: Instant)

    /**
     * Observe the active link row for an action.
     */
    @Query(
        """
        SELECT * FROM action_goal_links
        WHERE actionId = :actionId
          AND unlinkedAt IS NULL
        LIMIT 1
        """
    )
    fun observeActiveLinkForAction(actionId: String): Flow<ActionGoalLinkEntity?>

    /**
     * Day 11:
     * If a Goal is deleted, unlink all Actions currently pointing to it.
     *
     * We DO NOT delete sessions, we just close active link intervals.
     */
    @Query(
        """
        UPDATE action_goal_links
        SET unlinkedAt = :unlinkedAt
        WHERE goalId = :goalId
          AND unlinkedAt IS NULL
        """
    )
    suspend fun closeActiveLinksForGoal(goalId: String, unlinkedAt: Instant)
}
