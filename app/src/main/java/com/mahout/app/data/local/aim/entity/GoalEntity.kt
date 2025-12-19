package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.model.GoalStatus
import java.time.Instant
import java.time.LocalDate

/**
 * Goal storage table.
 * We keep optional parentGoalId to support laddering.
 */
@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["status"]),
        Index(value = ["horizon"]),
        Index(value = ["parentGoalId"])
    ]
)
data class GoalEntity(
    @PrimaryKey val goalId: String,

    val title: String,
    val why: String?,

    val horizon: GoalHorizon,
    val status: GoalStatus,

    /**
     * Priority is a stable "ordering" int; UI can sort by it.
     */
    val priority: Int,

    val targetDate: LocalDate?,
    val parentGoalId: String?,

    val createdAt: Instant,
    val updatedAt: Instant,

    /**
     * Soft delete is safer than hard delete early on.
     * You can still "archive" too; delete is a stronger action.
     */
    val deletedAt: Instant? = null
)
