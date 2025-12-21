package com.mahout.app.domain.path.usecase

import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.repository.ActionRepository
import javax.inject.Inject

class UpsertActionUseCase @Inject constructor(
    private val actionRepository: ActionRepository
) {
    suspend operator fun invoke(action: Action) {
        actionRepository.upsert(action)
    }
}
