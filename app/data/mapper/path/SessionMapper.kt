package com.mahout.app.data.mapper.path

import com.mahout.app.data.local.path.entity.SessionEntity
import com.mahout.app.domain.path.model.Session

fun SessionEntity.toDomain(): Session {
    return Session(
        sessionId = sessionId,
        actionId = actionId,
        startAt = startAt,
        endAt = endAt,
        durationMillis = durationMillis,
        source = source,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Session.toEntity(): SessionEntity {
    return SessionEntity(
        sessionId = sessionId,
        actionId = actionId,
        startAt = startAt,
        endAt = endAt,
        durationMillis = durationMillis,
        source = source,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
