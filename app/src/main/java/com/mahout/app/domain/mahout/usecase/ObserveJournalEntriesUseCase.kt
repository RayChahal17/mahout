package com.mahout.app.domain.mahout.usecase

import com.mahout.app.domain.mahout.model.JournalEntry
import com.mahout.app.domain.mahout.repository.JournalEntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveJournalEntriesUseCase @Inject constructor(
    private val repo: JournalEntryRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repo.observeEntries()
}
