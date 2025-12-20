package com.mahout.app.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Central place to inject dispatchers (IO/Main/Default).
 *
 * Why?
 * - Testability: unit tests can provide a TestDispatcher.
 * - Consistency: we never accidentally do DB work on Main thread.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}
