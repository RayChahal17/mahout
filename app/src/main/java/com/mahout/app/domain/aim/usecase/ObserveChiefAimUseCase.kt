package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.model.ChiefAim
import com.mahout.app.domain.aim.repository.ChiefAimRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChiefAimUseCase @Inject constructor(
    private val repo: ChiefAimRepository
) {
    operator fun invoke(): Flow<ChiefAim?> = repo.observeChiefAim()
}
