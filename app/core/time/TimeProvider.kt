package com.mahout.app.core.time

import java.time.Instant

/**
 * A simple abstraction over time.
 *
 * Why?
 * - Deterministic tests (you can freeze time).
 * - Consistency across features (Path/Aim rollups depend on correct time).
 */
interface TimeProvider {
    fun nowInstant(): Instant
}
