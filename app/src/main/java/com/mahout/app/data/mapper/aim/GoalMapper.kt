package com.mahout.app.data.mapper.aim

import com.mahout.app.data.local.aim.entity.GoalEntity
import com.mahout.app.domain.aim.model.Goal

/**
 * Entity <-> Domain mapper for Goals.
 *
 * IMPORTANT:
 * Entity uses goalId
 * Domain uses id
 *
 * This mapper is the ONLY place that should “know” those names differ.
 * Everywhere else should use the domain model (Goal.id).
 */
fun GoalEntity.toDomain(): Goal = Goal(
    id = goalId,
    title = title,
    why = why,
    horizon = horizon,
    status = status,
    priority = priority,
    targetDate = targetDate,
    parentGoalId = parentGoalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    goalId = id,
    title = title,
    why = why,
    horizon = horizon,
    status = status,
    priority = priority,
    targetDate = targetDate,
    parentGoalId = parentGoalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

