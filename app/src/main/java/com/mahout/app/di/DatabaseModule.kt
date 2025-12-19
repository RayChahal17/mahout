package com.mahout.app.di

import android.content.Context
import androidx.room.Room
import com.mahout.app.data.local.MahoutDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Single DB instance for the whole app process.
     *
     * Red-team note:
     * - During early development, migrations change often.
     * - We can keep destructive migrations ON for now to avoid being blocked.
     * - Before shipping, we will remove this and add proper migrations.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MahoutDatabase {
        return Room.databaseBuilder(
            context,
            MahoutDatabase::class.java,
            "mahout.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Provide DAOs (so repositories can inject them)

    @Provides fun provideChiefAimDao(db: MahoutDatabase) = db.chiefAimDao()
    @Provides fun provideGoalDao(db: MahoutDatabase) = db.goalDao()
    @Provides fun provideActionDao(db: MahoutDatabase) = db.actionDao()
    @Provides fun provideActionGoalLinkDao(db: MahoutDatabase) = db.actionGoalLinkDao()

    @Provides fun provideSessionDao(db: MahoutDatabase) = db.sessionDao()
    @Provides fun provideCheckEventDao(db: MahoutDatabase) = db.checkEventDao()

    @Provides fun provideMoodLogDao(db: MahoutDatabase) = db.moodLogDao()
    @Provides fun provideJournalEntryDao(db: MahoutDatabase) = db.journalEntryDao()

    @Provides fun provideMemorySummaryDao(db: MahoutDatabase) = db.memorySummaryDao()
    @Provides fun provideFutureProfileDao(db: MahoutDatabase) = db.futureProfileDao()
}
