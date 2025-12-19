package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ActionGoalLinkDao {

    /**
     * Active links (unlinkedAt IS NULL).
     */
    @Query("SELECT * FROM action_goal_links WHERE goalId = :goalId AND unlinkedAt IS NULL")
    fun observeActiveLinksForGoal(goalId: String): Flow<List<ActionGoalLinkEntity>>

    @Query("SELECT * FROM action_goal_links WHERE actionId = :actionId AND unlinkedAt IS NULL")
    fun observeActiveLinksForAction(actionId: String): Flow<List<ActionGoalLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLink(link: ActionGoalLinkEntity)

    /**
     * Unlink an existing link interval.
     * Note: we are NOT deleting logs. We are closing the interval.
     */
    @Query("UPDATE action_goal_links SET unlinkedAt = :unlinkedAt WHERE linkId = :linkId")
    suspend fun unlink(linkId: String, unlinkedAt: Instant)

    @Query("SELECT * FROM action_goal_links WHERE linkId = :linkId LIMIT 1")
    suspend fun getLink(linkId: String): ActionGoalLinkEntity?
}
