package com.mahout.app.domain.aim.model

/**
 * How far out a Goal lives. Stored in DB as enum.name (via Room TypeConverters later).
 * Keep names stable to avoid migration pain.
 */
enum class GoalHorizon {
    LONGTERM,
    MIDTERM,
    NEARTERM,
    THIS_MONTH
}
