package com.mahout.app.domain.mahout.model

/**
 * V1 journaling types.
 *
 * IMPORTANT:
 * - Your UI spec uses: New entry / Feeling / Plan / Gratitude.
 * - This enum is stored in Room as a STRING via TypeConverters (value.name).
 * - To avoid breaking early dev builds that may already have old values stored,
 *   we KEEP the legacy values at the bottom.
 */
enum class JournalEntryType {
    // ✅ V1 Mahout tab “modes”
    NEW_ENTRY,
    JOURNAL_FEELING,
    PLAN_NEXT_STEP,
    GRATITUDE,

    // --- Legacy / reserved (keep for safety in early builds) ---
    FREE_WRITE,
    PROMPT,
    EMOTION,
    LETTER
}
