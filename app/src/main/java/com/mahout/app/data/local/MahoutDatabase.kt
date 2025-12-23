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
import com.mahout.app.data.local.path.dao.TimerStateDao
import com.mahout.app.data.local.path.entity.SessionEntity
import com.mahout.app.data.local.path.entity.TimerStateEntity

/**
 * Single Room database for v1.
 *
 * We keep everything in one DB for simplicity.
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
        TimerStateEntity::class,

        // Elephant
        MoodLogEntity::class,

        // Mahout
        JournalEntryEntity::class,

        // North Star
        MemorySummaryEntity::class,
        FutureProfileEntity::class,
        NorthStarMessageEntity::class
    ],
    version = 5, // ✅ bumped: add TimerState (ForegroundService timer)
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
    abstract fun timerStateDao(): TimerStateDao

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
         * Add Chief Aim target date.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chief_aim ADD COLUMN targetDate TEXT")
            }
        }

        /**
         * Migration 4 → 5:
         * Add timer_state table (singleton timer row).
         *
         * Why a separate table?
         * - We need to represent RUNNING vs PAUSED vs STOPPED.
         * - Sessions alone can't reliably express "paused" without adding columns.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS timer_state (
                        timerId TEXT NOT NULL PRIMARY KEY,
                        status TEXT NOT NULL,
                        actionId TEXT,
                        currentSessionId TEXT,
                        accumulatedMillis INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
