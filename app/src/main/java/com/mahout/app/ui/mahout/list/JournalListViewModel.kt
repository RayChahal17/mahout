package com.mahout.app.ui.mahout.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.mahout.model.JournalEntryType
import com.mahout.app.domain.mahout.usecase.ObserveJournalEntriesUseCase
import com.mahout.app.domain.mahout.usecase.SoftDeleteJournalEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val observeJournalEntriesUseCase: ObserveJournalEntriesUseCase,
    private val softDeleteJournalEntryUseCase: SoftDeleteJournalEntryUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    data class RowUi(
        val id: String,
        val type: JournalEntryType,
        val typeLabel: String,
        val dateLabel: String,
        val preview: String
    )

    data class UiState(
        val rows: List<RowUi> = emptyList()
    ) {
        val isEmpty: Boolean get() = rows.isEmpty()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

    init {
        viewModelScope.launch {
            observeJournalEntriesUseCase().collect { entries ->
                val zone = ZoneId.systemDefault()
                val rows = entries.map { e ->
                    val dt = e.createdAt.atZone(zone).format(dateFormatter)
                    RowUi(
                        id = e.id,
                        type = e.type,
                        typeLabel = e.type.toUiLabel(),
                        dateLabel = dt,
                        preview = e.body
                            .trim()
                            .replace("\n", " ")
                            .take(140)
                            .ifBlank { "—" }
                    )
                }
                _state.update { it.copy(rows = rows) }
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch(dispatchers.io) {
            softDeleteJournalEntryUseCase(id, timeProvider.nowInstant())
        }
    }

    private fun JournalEntryType.toUiLabel(): String =
        when (this) {
            JournalEntryType.NEW_ENTRY -> "New entry"
            JournalEntryType.JOURNAL_FEELING -> "Feeling"
            JournalEntryType.PLAN_NEXT_STEP -> "Plan"
            JournalEntryType.GRATITUDE -> "Gratitude"
            else -> "Journal"
        }
}
