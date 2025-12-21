package com.mahout.app.domain.path.usecase

import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.repository.ActionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActiveActionsUseCase @Inject constructor(
    private val actionRepository: ActionRepository
) {
    operator fun invoke(): Flow<List<Action>> = actionRepository.observeActiveActions()
}
