package com.mahout.app.ui.mahout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MahoutViewModel @Inject constructor(
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // SavedState keys (rotation-safe, process-death-safe)
    private companion object {
        private const val KEY_MODE = "mahout_mode"
        private const val KEY_JOURNAL_TEXT = "mahout_journal_text"
        private const val KEY_NAME = "mahout_profile_name"
        private const val KEY_EMAIL = "mahout_profile_email"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<MahoutUiState> = _state

    private fun buildInitialState(): MahoutUiState {
        val mode = savedStateHandle.get<String>(KEY_MODE)?.let { MahoutMode.valueOf(it) } ?: MahoutMode.NEW_ENTRY
        val text = savedStateHandle.get<String>(KEY_JOURNAL_TEXT).orEmpty()
        val name = savedStateHandle.get<String>(KEY_NAME) ?: "Ray Chahal"
        val email = savedStateHandle.get<String>(KEY_EMAIL) ?: "raychahal17@gmail.com"

        val today = timeProvider.nowInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val chip = today.format(dateFormatter)

        return MahoutUiState(
            profileName = name,
            profileEmail = email,
            tagline = "Write • reflect • choose your next step",
            todayLabel = "Today",
            dateChip = chip,
            mode = mode,
            journalText = text,
            prompts = promptsFor(mode)
        )
    }

    fun selectMode(mode: MahoutMode) {
        savedStateHandle[KEY_MODE] = mode.name
        _state.update { it.copy(mode = mode, prompts = promptsFor(mode)) }
    }

    fun onJournalTextChanged(text: String) {
        savedStateHandle[KEY_JOURNAL_TEXT] = text
        _state.update { it.copy(journalText = text) }
    }

    fun onPromptClicked(prompt: MahoutPromptUi) {
        // Premium behavior: if text empty -> drop prompt as first line
        // else append prompt on new line (keeps it quick & non-destructive).
        viewModelScope.launch {
            _state.update { current ->
                val currentText = current.journalText
                val newText = if (currentText.isBlank()) {
                    prompt.text
                } else {
                    currentText.trimEnd() + "\n\n" + prompt.text
                }
                savedStateHandle[KEY_JOURNAL_TEXT] = newText
                current.copy(journalText = newText)
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        savedStateHandle[KEY_NAME] = name
        savedStateHandle[KEY_EMAIL] = email
        _state.update { it.copy(profileName = name, profileEmail = email) }
    }

    private fun promptsFor(mode: MahoutMode): List<MahoutPromptUi> {
        return when (mode) {
            MahoutMode.NEW_ENTRY -> listOf(
                MahoutPromptUi("truth", "“What’s the real truth today?” — “One honest paragraph.”"),
                MahoutPromptUi("avoid", "“What am I avoiding?” — “Name it gently.”"),
                MahoutPromptUi("win", "“What would make today a win?” — “One small thing.”")
            )

            MahoutMode.JOURNAL_FEELING -> listOf(
                MahoutPromptUi("feel1", "“What am I feeling right now?” — “Name it.”"),
                MahoutPromptUi("feel2", "“Where do I feel it in my body?” — “Describe.”"),
                MahoutPromptUi("feel3", "“What do I need?” — “One clear need.”")
            )

            MahoutMode.PLAN_NEXT_STEP -> listOf(
                MahoutPromptUi("plan1", "“What’s the next smallest step?” — “Make it tiny.”"),
                MahoutPromptUi("plan2", "“What could block me?” — “Plan a backup.”"),
                MahoutPromptUi("plan3", "“When will I do it?” — “Pick a time.”")
            )

            MahoutMode.GRATITUDE -> listOf(
                MahoutPromptUi("grat1", "“Three good things today…”"),
                MahoutPromptUi("grat2", "“Someone I appreciate…”"),
                MahoutPromptUi("grat3", "“A moment I want to remember…”")
            )
        }
    }
}
