package com.mahout.app.ui.elephant

import com.mahout.app.R

/**
 * Single source of truth for mood options in V1.
 *
 * NEW:
 * - We add a final "Not now" option so users can cancel logging intentionally
 *   without closing the overlay.
 */
object MoodCatalog {

    /**
     * Special mood id meaning: "Do NOT log anything".
     * Keep this stable (acts like an internal API contract).
     */
    const val SKIP_MOOD_ID: String = "not_now"

    val moods: List<MoodUi> = listOf(
        MoodUi("blessed", "Blessed", R.drawable.ic_mood_blessed_48),
        MoodUi("very_happy", "Very happy", R.drawable.ic_mood_very_happy_48),
        MoodUi("happy", "Happy", R.drawable.ic_mood_happy_48),
        MoodUi("calm", "Calm", R.drawable.ic_mood_calm_48),
        MoodUi("anxious", "Anxious", R.drawable.ic_mood_anxious_48),
        MoodUi("angry", "Angry", R.drawable.ic_mood_angry_48),
        MoodUi("frustrated", "Frustrated", R.drawable.ic_mood_frustrated_48),
        MoodUi("low", "Low", R.drawable.ic_mood_low_48),
        MoodUi("sad", "Sad", R.drawable.ic_mood_sad_48),

        // ✅ LAST ITEM = cancel / do not log
        // Uses the elephant logo as you requested.
        MoodUi(SKIP_MOOD_ID, "Not now", R.drawable.ic_elephant_24)
    )

    fun getOrNull(id: String?): MoodUi? {
        if (id == null) return null
        return moods.firstOrNull { it.id == id }
    }

    /**
     * Convert an arbitrary mood id into something safe for UI display.
     */
    fun toDisplayMood(id: String): MoodUi {
        return getOrNull(id) ?: MoodUi(
            id = id,
            label = id.replace('_', ' ').replaceFirstChar { it.uppercase() },
            iconRes = R.drawable.ic_elephant_24
        )
    }
}
