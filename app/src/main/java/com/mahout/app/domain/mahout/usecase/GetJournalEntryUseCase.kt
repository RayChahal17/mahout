package com.mahout.app.domain.mahout.usecase

import com.mahout.app.domain.mahout.model.JournalEntry
import com.mahout.app.domain.mahout.repository.JournalEntryRepository
import javax.inject.Inject

class GetJournalEntryUseCase @Inject constructor(
    private val repo: JournalEntryRepository
) {
    suspend operator fun invoke(id: String): JournalEntry? = repo.getById(id)
}
