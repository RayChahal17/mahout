package com.mahout.app.ui.aim

import com.mahout.app.R

/**
 * Roadmap buckets = the horizontal headings the user scrolls left/right.
 *
 * These are UI concepts (not domain concepts).
 * Domain has GoalHorizon (this month / near / mid / long).
 *
 * RoadmapBucket is for "where does this goal show up on the Aim roadmap tab?"
 */
enum class RoadmapBucket {
    NEXT_30_DAYS,
    ONE_TO_SIX_MONTHS,
    SIX_TO_24_MONTHS,
    TWO_TO_TEN_YEARS,
    ARCHIVED;

    companion object {

        /**
         * Map the checked MaterialButton id -> RoadmapBucket.
         *
         * IMPORTANT:
         * These IDs MUST match the ones you define in fragment_aim.xml.
         */
        fun fromButtonId(buttonId: Int): RoadmapBucket? {
            return when (buttonId) {
                R.id.btnBucketNext30 -> NEXT_30_DAYS
                R.id.btnBucket1to6 -> ONE_TO_SIX_MONTHS
                R.id.btnBucket6to24 -> SIX_TO_24_MONTHS
                R.id.btnBucket2to10 -> TWO_TO_TEN_YEARS
                R.id.btnBucketArchived -> ARCHIVED
                else -> null
            }
        }
    }
}
