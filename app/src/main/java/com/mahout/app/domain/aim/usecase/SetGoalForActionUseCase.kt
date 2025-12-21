package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import javax.inject.Inject

class SetGoalForActionUseCase @Inject constructor(
    private val linkRepository: ActionGoalLinkRepository
) {
    suspend operator fun invoke(actionId: String, goalId: String?) {
        linkRepository.setGoalForAction(actionId, goalId)
    }
}
