package com.mahout.app.domain.elephant.usecase

import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.repository.MoodLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMoodLogsUseCase @Inject constructor(
    private val repo: MoodLogRepository
) {
    operator fun invoke(): Flow<List<MoodLog>> = repo.observeMoodLogs()
}



