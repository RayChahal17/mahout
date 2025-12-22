package com.mahout.app.data.mapper.aim

import com.mahout.app.data.local.aim.entity.ChiefAimEntity
import com.mahout.app.domain.aim.model.ChiefAim

/**
 * Entity <-> Domain mappers for ChiefAim.
 */
fun ChiefAimEntity.toDomain(): ChiefAim = ChiefAim(
    title = title,
    description = description,
    targetDate = targetDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ChiefAim.toEntity(id: Int = 1): ChiefAimEntity = ChiefAimEntity(
    id = id,
    title = title,
    description = description,
    targetDate = targetDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)
