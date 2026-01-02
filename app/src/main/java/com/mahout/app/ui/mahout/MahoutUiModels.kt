package com.mahout.app.ui.mahout

enum class MahoutMode {
    NEW_ENTRY,
    JOURNAL_FEELING,
    PLAN_NEXT_STEP,
    GRATITUDE
}

data class MahoutPromptUi(
    val id: String,
    val text: String
)

data class MahoutUiState(
    val profileName: String,
    val profileEmail: String,
    val tagline: String,
    val todayLabel: String,
    val dateChip: String,
    val mode: MahoutMode,
    val journalText: String,
    val prompts: List<MahoutPromptUi>
)
