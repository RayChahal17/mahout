package com.mahout.app.ui.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.ActionTrackingType
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import com.mahout.app.domain.path.usecase.UpsertActionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PathUiState(
    val actions: List<Action> = emptyList()
)

@HiltViewModel
class PathViewModel @Inject constructor(
    observeActiveActions: ObserveActiveActionsUseCase,
    private val upsertAction: UpsertActionUseCase,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val state: StateFlow<PathUiState> =
        observeActiveActions()
            .map { PathUiState(actions = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PathUiState())

    fun createSampleAction() {
        viewModelScope.launch {
            val now = timeProvider.nowInstant()
            val action = Action(
                id = idProvider.newId(),
                title = "Sample: Deep Work",
                description = "Created from Path screen",
                trackingType = ActionTrackingType.TIME,
                cadence = ActionCadence.DAILY,
                targetValue = 60,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
            upsertAction(action)
        }
    }
}
