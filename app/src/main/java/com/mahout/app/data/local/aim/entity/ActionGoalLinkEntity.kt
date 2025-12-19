package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * IMPORTANT: This is NOT a simple many-to-many join table.
 *
 * Your spec requires:
 * - Unlinking does not delete history,
 * - But it removes the relationship for future stats.
 *
 * So we model links as "time intervals":
 * - linkedAt = when the action started contributing to the goal
 * - unlinkedAt = when it stopped (null means still linked)
 *
 * We allow multiple link intervals between the same action & goal over time (link/unlink/relink).
 * That's why we use a separate linkId primary key instead of composite PK(actionId, goalId).
 */
@Entity(
    tableName = "action_goal_links",
    indices = [
        Index(value = ["actionId"]),
        Index(value = ["goalId"]),
        Index(value = ["unlinkedAt"])
    ]
)
data class ActionGoalLinkEntity(
    @PrimaryKey val linkId: String,

    val actionId: String,
    val goalId: String,

    val linkedAt: Instant,
    val unlinkedAt: Instant? = null
)
