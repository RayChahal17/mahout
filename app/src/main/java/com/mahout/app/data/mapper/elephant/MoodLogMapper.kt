package com.mahout.app.data.mapper.elephant

import com.mahout.app.data.local.elephant.entity.MoodLogEntity
import com.mahout.app.domain.elephant.model.MoodLog

fun MoodLogEntity.toDomain(): MoodLog =
    MoodLog(
        id = moodLogId,
        feeling = feeling,
        intensity = intensity,
        note = note,
        at = at,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun MoodLog.toEntity(): MoodLogEntity =
    MoodLogEntity(
        moodLogId = id,
        feeling = feeling,
        intensity = intensity,
        note = note,
        at = at,
        createdAt = createdAt,
        updatedAt = updatedAt
    )



