// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Hilt plugin available to modules (we apply it in :app)
    alias(libs.plugins.hilt.android) apply false
}