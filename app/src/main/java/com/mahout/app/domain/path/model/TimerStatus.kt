package com.mahout.app.domain.path.model

/**
 * Represents the "global" Path timer state.
 *
 * Why do we need this (instead of only looking at sessions)?
 * - Sessions are the *log of work* (they can be many rows).
 * - The timer is a *single* UX object the user expects to be either Running, Paused, or Stopped.
 * - We persist this so the UI can recover after process death / configuration changes.
 */
enum class TimerStatus {
    RUNNING,
    PAUSED,
    STOPPED
}
