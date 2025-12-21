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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MahoutDatabase {
        return Room.databaseBuilder(
            context,
            MahoutDatabase::class.java,
            "mahout.db"
        )
            // dev-only convenience; before shipping we’ll do real migrations
            .fallbackToDestructiveMigration()
            .build()
    }

    // Provide DAOs (so repositories can inject them)
    @Provides fun provideChiefAimDao(db: MahoutDatabase) = db.chiefAimDao()
    @Provides fun provideGoalDao(db: MahoutDatabase) = db.goalDao()
    @Provides fun provideActionDao(db: MahoutDatabase) = db.actionDao()
    @Provides fun provideActionGoalLinkDao(db: MahoutDatabase) = db.actionGoalLinkDao()

    // Path (TIME-only)
    @Provides fun provideSessionDao(db: MahoutDatabase) = db.sessionDao()

    @Provides fun provideMoodLogDao(db: MahoutDatabase) = db.moodLogDao()
    @Provides fun provideJournalEntryDao(db: MahoutDatabase) = db.journalEntryDao()
    @Provides fun provideMemorySummaryDao(db: MahoutDatabase) = db.memorySummaryDao()
    @Provides fun provideFutureProfileDao(db: MahoutDatabase) = db.futureProfileDao()
}
