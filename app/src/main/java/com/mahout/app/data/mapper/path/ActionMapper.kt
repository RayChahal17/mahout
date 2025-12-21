package com.mahout.app.data.mapper.path

import com.mahout.app.data.local.aim.entity.ActionEntity
import com.mahout.app.domain.path.model.Action

/**
 * Converts Room entity <-> domain model.
 *
 * Domain model = what the app uses everywhere else.
 * Entity = what Room stores in the DB.
 */
fun ActionEntity.toDomain(): Action {
    return Action(
        id = actionId,
        title = title,
        description = description,
        trackingType = trackingType,
        cadence = cadence,
        targetValue = targetValue,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt
    )
}

fun Action.toEntity(): ActionEntity {
    return ActionEntity(
        actionId = id,
        title = title,
        description = description,
        trackingType = trackingType,
        cadence = cadence,
        targetValue = targetValue,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt
    )
}
