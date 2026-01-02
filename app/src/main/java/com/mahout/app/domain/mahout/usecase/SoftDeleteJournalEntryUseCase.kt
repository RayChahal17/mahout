package com.mahout.app.domain.mahout.usecase

import com.mahout.app.domain.mahout.repository.JournalEntryRepository
import java.time.Instant
import javax.inject.Inject

class SoftDeleteJournalEntryUseCase @Inject constructor(
    private val repo: JournalEntryRepository
) {
    suspend operator fun invoke(id: String, deletedAt: Instant) = repo.softDelete(id, deletedAt)
}
