package com.mahout.app.ui.aim

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.usecase.ObserveGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data object NotFound : GoalDetailUiState
    data class Content(val goal: Goal) : GoalDetailUiState
}

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGoalUseCase: ObserveGoalUseCase
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]

    val state: StateFlow<GoalDetailUiState> =
        if (goalId == null) {
            // No arg => not found
            kotlinx.coroutines.flow.flowOf(GoalDetailUiState.NotFound)
                .stateIn(viewModelScope, SharingStarted.Eagerly, GoalDetailUiState.NotFound)
        } else {
            observeGoalUseCase(goalId)
                .map { goal ->
                    when (goal) {
                        null -> GoalDetailUiState.NotFound
                        else -> GoalDetailUiState.Content(goal)
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState.Loading)
        }
}
