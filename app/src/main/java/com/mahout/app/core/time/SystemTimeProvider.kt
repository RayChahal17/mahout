package com.mahout.app.core.time

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowInstant(): Instant = Instant.now()
}
