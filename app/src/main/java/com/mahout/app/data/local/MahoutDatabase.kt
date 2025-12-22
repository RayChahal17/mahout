package com.mahout.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.mahout.app.data.local.northstar.dao.NorthStarMessageDao
import com.mahout.app.data.local.northstar.entity.FutureProfileEntity
import com.mahout.app.data.local.northstar.entity.MemorySummaryEntity
import com.mahout.app.data.local.northstar.entity.NorthStarMessageEntity
import com.mahout.app.data.local.path.dao.SessionDao
import com.mahout.app.data.local.path.entity.SessionEntity

/**
 * Single Room database for V1 (local-first).
 *
 * exportSchema=true will write schema JSON to app/schemas when Gradle runs.
 */
@Database(
    entities = [
        // Aim
        ChiefAimEntity::class,
        GoalEntity::class,
        ActionEntity::class,
        ActionGoalLinkEntity::class,

        // Path (TIME only)
        SessionEntity::class,

        // Elephant
        MoodLogEntity::class,

        // Mahout
        JournalEntryEntity::class,

        // North Star
        MemorySummaryEntity::class,
        FutureProfileEntity::class,
        NorthStarMessageEntity::class
    ],
    version = 4, // ✅ bumped: added ChiefAim.targetDate
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

    // Elephant
    abstract fun moodLogDao(): MoodLogDao

    // Mahout
    abstract fun journalEntryDao(): JournalEntryDao

    // North Star
    abstract fun memorySummaryDao(): MemorySummaryDao
    abstract fun futureProfileDao(): FutureProfileDao
    abstract fun northStarMessageDao(): NorthStarMessageDao

    companion object {
        /**
         * Migration 3 → 4:
         * Add nullable targetDate column to chief_aim table.
         *
         * Why nullable?
         * - Existing users may already have a Chief Aim saved.
         * - We don’t want to wipe their DB during dev.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chief_aim ADD COLUMN targetDate TEXT")
            }
        }
    }
}
