package com.mahout.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MahoutApplication is the global Application class.
 *
 * @HiltAndroidApp is REQUIRED for Hilt:
 * - It generates the base dependency container used across the whole app.
 * - Without it, @AndroidEntryPoint Activities/Fragments will crash at runtime.
 */
@HiltAndroidApp
class MahoutApplication : Application()
