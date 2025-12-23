package com.mahout.app.ui.path.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.aim.usecase.ObserveActiveGoalsUseCase
import com.mahout.app.domain.aim.usecase.SetGoalForActionUseCase
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.ActionStatus
import com.mahout.app.domain.path.model.ActionTrackingType
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.usecase.ArchiveActionUseCase
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import com.mahout.app.domain.path.usecase.ObserveTimerStateUseCase // ✅ IMPORTANT: no ".timer" here
import com.mahout.app.domain.path.usecase.UpsertActionUseCase
import com.mahout.app.ui.path.PathEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PathViewModel @Inject constructor(
    observeActiveActionsUseCase: ObserveActiveActionsUseCase,
    observeActiveGoalsUseCase: ObserveActiveGoalsUseCase,
    observeTimerStateUseCase: ObserveTimerStateUseCase,
    private val upsertActionUseCase: UpsertActionUseCase,
    private val archiveActionUseCase: ArchiveActionUseCase,
    private val setGoalForActionUseCase: SetGoalForActionUseCase,
    private val actionGoalLinkRepository: ActionGoalLinkRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _events = MutableSharedFlow<PathEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val actions: StateFlow<List<Action>> =
        observeActiveActionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeGoals: StateFlow<List<Goal>> =
        observeActiveGoalsUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Timer state (STOPPED / RUNNING / PAUSED) observed from Room.
     */
    val timerState: StateFlow<TimerState> =
        observeTimerStateUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TimerState.stopped(timeProvider.nowInstant())
            )

    /**
     * Used by the Action dialog to pre-fill the goal link.
     * We fetch BEFORE building the dialog UI to avoid wiping link on Save.
     */
    suspend fun getLinkedGoalId(actionId: String): String? {
        return actionGoalLinkRepository.observeGoalForAction(actionId)
            .first()
            ?.id
    }

    fun archiveAction(actionId: String) {
        viewModelScope.launch {
            runCatching { archiveActionUseCase(actionId) }
                .onSuccess { _events.tryEmit(PathEvent.ShowSnackbar("Action archived")) }
                .onFailure { _events.tryEmit(PathEvent.ShowSnackbar(it.message ?: "Failed to archive")) }
        }
    }

    /**
     * Create or update an Action (TIME-only v1).
     * Also sets/clears 0/1 goal link via SetGoalForActionUseCase.
     */
    fun saveAction(
        existingId: String?,
        title: String,
        cadence: ActionCadence,
        targetMinutes: Int?,
        linkedGoalId: String?
    ) {
        viewModelScope.launch {
            runCatching {
                val now = timeProvider.nowInstant()
                val actionId = existingId ?: idProvider.newId()

                val action = Action(
                    id = actionId,
                    title = title,
                    description = null,
                    cadence = cadence,
                    trackingType = ActionTrackingType.TIME,
                    targetValue = targetMinutes,
                    status = ActionStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now
                )

                upsertActionUseCase(action)
                setGoalForActionUseCase(actionId, linkedGoalId)
            }
                .onSuccess { _events.tryEmit(PathEvent.ShowSnackbar("Saved")) }
                .onFailure { _events.tryEmit(PathEvent.ShowSnackbar(it.message ?: "Save failed")) }
        }
    }
}
