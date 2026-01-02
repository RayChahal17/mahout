package com.mahout.app.domain.elephant.usecase

import com.mahout.app.domain.elephant.repository.MoodLogRepository
import javax.inject.Inject

/**
 * Deletes a single mood log.
 *
 * We keep this as a UseCase (instead of calling the repository from the UI)
 * to preserve the app’s MVVM layering:
 * Fragment -> ViewModel -> UseCases -> Repository -> Room
 */
class DeleteMoodLogUseCase @Inject constructor(
    private val repo: MoodLogRepository
) {
    suspend operator fun invoke(id: String) {
        repo.deleteById(id)
    }
}
