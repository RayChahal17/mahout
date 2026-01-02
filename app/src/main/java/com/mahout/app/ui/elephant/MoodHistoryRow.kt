package com.mahout.app.ui.elephant

/**
 * RecyclerView row models for the Elephant “Recent” list.
 *
 * We group logs by local day (Today / Yesterday / date),
 * so the list alternates between Header -> Entries.
 */
sealed interface MoodHistoryRow {

    data class Header(
        val title: String
    ) : MoodHistoryRow

    data class Entry(
        val id: String,        // MoodLog.id (primary key)
        val moodId: String,    // MoodLog.feeling
        val moodLabel: String, // display label (defensive mapping)
        val iconRes: Int,
        val timeLabel: String, // formatted local time (e.g., "9:41 PM")
        val note: String?
    ) : MoodHistoryRow
}
