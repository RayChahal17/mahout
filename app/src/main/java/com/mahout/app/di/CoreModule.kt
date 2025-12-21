package com.mahout.app.di

import com.mahout.app.core.dispatchers.DefaultDispatcherProvider
import com.mahout.app.core.dispatchers.DispatcherProvider
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.id.UuidIdProvider
import com.mahout.app.core.time.SystemTimeProvider
import com.mahout.app.core.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds interfaces to concrete implementations.
 *
 * Using @Binds avoids manual provider functions and reduces boilerplate.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindIdProvider(impl: UuidIdProvider): IdProvider
}
