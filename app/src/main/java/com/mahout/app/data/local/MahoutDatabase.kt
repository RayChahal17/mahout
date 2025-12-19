package com.mahout.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mahout.app.data.local.aim.dao.ActionDao
import com.mahout.app.data.local.aim.dao.ActionGoalLinkDao
import com.mahout.app.data.local.aim.dao.ChiefAimDao
import com.mahout.app.data.local.aim.dao.GoalDao
import com.mahout.app.data.local.aim.entity.ActionEntity
import com.mahout.app.data.local.aim.entity.ActionGoalLinkEntity
import com.mahout.app.data.local.aim.entity.ChiefAimEntity
import com.mahout.app.data.local.aim.entity.GoalEntity
import com.mahout.app.data.local.elephant.dao.MoodLogDao
import com.mahout.app.data.local.elephant.entity.MoodLogEntity
import com.mahout.app.data.local.mahout.dao.JournalEntryDao
import com.mahout.app.data.local.mahout.entity.JournalEntryEntity
import com.mahout.app.data.local.northstar.dao.FutureProfileDao
import com.mahout.app.data.local.northstar.dao.MemorySummaryDao
import com.mahout.app.data.local.northstar.entity.FutureProfileEntity
import com.mahout.app.data.local.northstar.entity.MemorySummaryEntity
import com.mahout.app.data.local.path.dao.CheckEventDao
import com.mahout.app.data.local.path.dao.SessionDao
import com.mahout.app.data.local.path.entity.CheckEventEntity
import com.mahout.app.data.local.path.entity.SessionEntity

/**
 * Single Room database for V1 (local-first).
 *
 * exportSchema=true will write schema JSON to app/schemas when Gradle runs.
 * (You already created app/schemas/.gitkeep.)
 */
@Database(
    entities = [
        // Aim
        ChiefAimEntity::class,
        GoalEntity::class,
        ActionEntity::class,
        ActionGoalLinkEntity::class,

        // Path
        SessionEntity::class,
        CheckEventEntity::class,

        // Elephant
        MoodLogEntity::class,

        // Mahout
        JournalEntryEntity::class,

        // North Star
        MemorySummaryEntity::class,
        FutureProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(MahoutTypeConverters::class)
abstract class MahoutDatabase : RoomDatabase() {

    // Aim
    abstract fun chiefAimDao(): ChiefAimDao
    abstract fun goalDao(): GoalDao
    abstract fun actionDao(): ActionDao
    abstract fun actionGoalLinkDao(): ActionGoalLinkDao

    // Path
    abstract fun sessionDao(): SessionDao
    abstract fun checkEventDao(): CheckEventDao

    // Elephant
    abstract fun moodLogDao(): MoodLogDao

    // Mahout
    abstract fun journalEntryDao(): JournalEntryDao

    // North Star
    abstract fun memorySummaryDao(): MemorySummaryDao
    abstract fun futureProfileDao(): FutureProfileDao
}
