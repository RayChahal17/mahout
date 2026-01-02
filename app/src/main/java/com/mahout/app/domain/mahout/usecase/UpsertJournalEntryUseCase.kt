package com.mahout.app.domain.mahout.usecase

import com.mahout.app.domain.mahout.model.JournalEntry
import com.mahout.app.domain.mahout.repository.JournalEntryRepository
import javax.inject.Inject

class UpsertJournalEntryUseCase @Inject constructor(
    private val repo: JournalEntryRepository
) {
    suspend operator fun invoke(entry: JournalEntry) = repo.upsert(entry)
}
