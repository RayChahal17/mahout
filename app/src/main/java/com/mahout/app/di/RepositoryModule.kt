package com.mahout.app.di

import com.mahout.app.data.repository.aim.RoomActionGoalLinkRepository
import com.mahout.app.data.repository.path.RoomActionRepository
import com.mahout.app.data.repository.path.RoomSessionRepository
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.mahout.app.data.repository.aim.RoomChiefAimRepository
import com.mahout.app.domain.aim.repository.ChiefAimRepository
import com.mahout.app.data.repository.aim.RoomGoalRepository
import com.mahout.app.domain.aim.repository.GoalRepository


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindActionRepository(impl: RoomActionRepository): ActionRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindActionGoalLinkRepository(impl: RoomActionGoalLinkRepository): ActionGoalLinkRepository

    @Binds
    @Singleton
    abstract fun bindChiefAimRepository(impl: RoomChiefAimRepository): ChiefAimRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: RoomGoalRepository): GoalRepository


}
