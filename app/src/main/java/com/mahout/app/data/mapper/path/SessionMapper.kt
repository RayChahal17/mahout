package com.mahout.app.data.mapper.path

import com.mahout.app.data.local.path.entity.SessionEntity
import com.mahout.app.domain.path.model.Session

/**
 * Converts Room entity <-> domain model.
 */
fun SessionEntity.toDomain(): Session {
    return Session(
        id = sessionId,
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
        sessionId = id,
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
