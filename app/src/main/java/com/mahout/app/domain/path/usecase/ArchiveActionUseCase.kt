package com.mahout.app.domain.path.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.repository.ActionRepository
import javax.inject.Inject

/**
 * Day 12:
 * Archive (soft) an Action so it no longer appears in "active actions".
 */
class ArchiveActionUseCase @Inject constructor(
    private val repo: ActionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(actionId: String) {
        repo.archiveAction(
            actionId = actionId,
            archivedAt = timeProvider.nowInstant()
        )
    }
}
