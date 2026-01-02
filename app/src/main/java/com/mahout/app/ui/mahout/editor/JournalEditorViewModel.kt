package com.mahout.app.ui.mahout.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.mahout.model.JournalEntry
import com.mahout.app.domain.mahout.model.JournalEntryType
import com.mahout.app.domain.mahout.usecase.GetJournalEntryUseCase
import com.mahout.app.domain.mahout.usecase.UpsertJournalEntryUseCase
import com.mahout.app.ui.mahout.MahoutPromptUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class JournalEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
    private val getJournalEntryUseCase: GetJournalEntryUseCase,
    private val upsertJournalEntryUseCase: UpsertJournalEntryUseCase
) : ViewModel() {

    companion object {
        const val ARG_ENTRY_TYPE = "entryType"
        const val ARG_ENTRY_ID = "entryId"
        const val ARG_PREFILL_BODY = "prefillBody"
        const val ARG_PROMPT_ID = "promptId"
    }

    data class UiState(
        val entryId: String? = null,
        val type: JournalEntryType = JournalEntryType.NEW_ENTRY,

        /**
         * Used for “discard changes?” logic in the Fragment.
         * We keep it in state so it survives rotation/process death.
         */
        val initialBody: String = "",

        val body: String = "",
        val promptId: String? = null,

        val dateChip: String = "",
        val prompts: List<MahoutPromptUi> = emptyList(),

        val isSaving: Boolean = false
    ) {
        val canSave: Boolean get() = body.trim().isNotEmpty() && !isSaving
        val isEditMode: Boolean get() = !entryId.isNullOrBlank()
    }

    sealed interface Event {
        data class ShowSnackbar(val message: String) : Event
        data object SavedAndClose : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<UiState> = _state

    /**
     * We store createdAt separately so edits don’t rewrite history.
     * (Room entity has createdAt + updatedAt.)
     */
    private var loadedCreatedAt = timeProvider.nowInstant()

    private fun buildInitialState(): UiState {
        val now = timeProvider.nowInstant()
        val chip = now.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

        val entryId = savedStateHandle.get<String>(ARG_ENTRY_ID)
        val typeName = savedStateHandle.get<String>(ARG_ENTRY_TYPE) ?: JournalEntryType.NEW_ENTRY.name
        val type = runCatching { JournalEntryType.valueOf(typeName) }.getOrDefault(JournalEntryType.NEW_ENTRY)

        val prefill = savedStateHandle.get<String>(ARG_PREFILL_BODY).orEmpty()
        val promptId = savedStateHandle.get<String>(ARG_PROMPT_ID)

        return UiState(
            entryId = entryId,
            type = type,
            initialBody = prefill,
            body = prefill,
            promptId = promptId,
            dateChip = chip,
            prompts = promptsFor(type)
        )
    }

    init {
        // If editing an existing entry, load it from Room.
        val entryId = savedStateHandle.get<String>(ARG_ENTRY_ID)
        if (!entryId.isNullOrBlank()) {
            viewModelScope.launch(dispatchers.io) {
                val entry = getJournalEntryUseCase(entryId)
                if (entry != null) {
                    loadedCreatedAt = entry.createdAt
                    _state.update {
                        it.copy(
                            entryId = entry.id,
                            type = entry.type,
                            initialBody = entry.body,
                            body = entry.body,
                            promptId = entry.promptId,
                            prompts = promptsFor(entry.type)
                        )
                    }
                } else {
                    _events.tryEmit(Event.ShowSnackbar("Couldn’t load that entry."))
                }
            }
        }
    }

    fun onBodyChanged(newBody: String) {
        _state.update { it.copy(body = newBody) }
    }

    fun onSaveClicked() {
        val s = _state.value
        if (s.body.trim().isEmpty()) {
            _events.tryEmit(Event.ShowSnackbar("Write something first."))
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _state.update { it.copy(isSaving = true) }

            val now = timeProvider.nowInstant()

            val isEdit = !s.entryId.isNullOrBlank()
            val id = s.entryId ?: idProvider.newId()

            val entry = JournalEntry(
                id = id,
                type = s.type,
                title = null, // v1 list uses type + preview; title can come later
                body = s.body.trimEnd(),
                promptId = s.promptId,
                relatedMoodLogId = null, // future: link Elephant mood log → Mahout entry
                createdAt = if (isEdit) loadedCreatedAt else now,
                updatedAt = now
            )

            upsertJournalEntryUseCase(entry)

            // After saving, update initialBody so rotation doesn’t think it’s “unsaved”
            _state.update { it.copy(isSaving = false, initialBody = entry.body, body = entry.body) }

            _events.tryEmit(Event.SavedAndClose)
        }
    }

    /**
     * Prompts match your existing MahoutViewModel prompts, just keyed by JournalEntryType.
     */
    private fun promptsFor(type: JournalEntryType): List<MahoutPromptUi> {
        return when (type) {
            JournalEntryType.NEW_ENTRY, JournalEntryType.FREE_WRITE, JournalEntryType.PROMPT -> listOf(
                MahoutPromptUi("truth", "“What’s the real truth today?” — “One honest paragraph.”"),
                MahoutPromptUi("avoid", "“What am I avoiding?” — “Name it gently.”"),
                MahoutPromptUi("win", "“What would make today a win?” — “One small thing.”")
            )

            JournalEntryType.JOURNAL_FEELING, JournalEntryType.EMOTION -> listOf(
                MahoutPromptUi("feel1", "“What am I feeling right now?” — “Name it.”"),
                MahoutPromptUi("feel2", "“Where do I feel it in my body?” — “Describe.”"),
                MahoutPromptUi("feel3", "“What do I need?” — “One clear need.”")
            )

            JournalEntryType.PLAN_NEXT_STEP -> listOf(
                MahoutPromptUi("plan1", "“What’s the next smallest step?” — “Make it tiny.”"),
                MahoutPromptUi("plan2", "“What could block me?” — “Plan a backup.”"),
                MahoutPromptUi("plan3", "“When will I do it?” — “Pick a time.”")
            )

            JournalEntryType.GRATITUDE -> listOf(
                MahoutPromptUi("grat1", "“Three good things today…”"),
                MahoutPromptUi("grat2", "“Someone I appreciate…”"),
                MahoutPromptUi("grat3", "“A moment I want to remember…”")
            )

            JournalEntryType.LETTER -> listOf(
                MahoutPromptUi("let1", "“Write a letter you’ll never send…”"),
                MahoutPromptUi("let2", "“What do you wish they understood?”"),
                MahoutPromptUi("let3", "“What would closure look like?”")
            )
        }
    }
}
