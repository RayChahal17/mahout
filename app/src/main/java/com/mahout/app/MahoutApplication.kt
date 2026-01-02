package com.mahout.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

/**
 * Global Application class.
 *
 * Phase 1 requirement:
 * - Force dark mode ON by default for the "premium-black" feel.
 *
 * IMPORTANT:
 * - This overrides the user's system setting intentionally for Phase 1.
 * - Later we can store a user preference in DataStore.
 */
@HiltAndroidApp
class MahoutApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force night resources + night theme.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
