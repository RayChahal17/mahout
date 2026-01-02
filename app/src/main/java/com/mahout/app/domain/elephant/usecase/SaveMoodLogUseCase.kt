package com.mahout.app.domain.elephant.usecase

import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.repository.MoodLogRepository
import java.time.Instant
import javax.inject.Inject

class SaveMoodLogUseCase @Inject constructor(
    private val repo: MoodLogRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        feeling: String,
        intensity: Int?,
        note: String?
    ) {
        val now = timeProvider.nowInstant()
        val log = MoodLog(
            id = idProvider.newId(),
            feeling = feeling,
            intensity = intensity,
            note = note,
            at = now,
            createdAt = now,
            updatedAt = now
        )
        repo.upsert(log)
    }
}



