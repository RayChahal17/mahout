package com.mahout.app.ui.path

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
import com.mahout.app.domain.path.model.ActionTrackingType
import com.mahout.app.domain.path.usecase.ArchiveActionUseCase
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import com.mahout.app.domain.path.usecase.UpsertActionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Day 12 ViewModel responsibilities:
 * - Observe active actions for the list
 * - Provide active goals snapshot for the "link to goal" dropdown
 * - Save Action (upsert) + optionally set goal link
 * - Archive Action
 *
 * IMPORTANT:
 * We keep this separate from Aim (Day 11) so we don’t risk breaking working code.
 */
@HiltViewModel
class PathViewModel @Inject constructor(
    observeActiveActionsUseCase: ObserveActiveActionsUseCase,
    observeActiveGoalsUseCase: ObserveActiveGoalsUseCase,
    private val upsertActionUseCase: UpsertActionUseCase,
    private val archiveActionUseCase: ArchiveActionUseCase,
    private val setGoalForActionUseCase: SetGoalForActionUseCase,
    private val actionGoalLinkRepository: ActionGoalLinkRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _events = MutableSharedFlow<PathEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /**
     * Active actions list for the Path screen.
     */
    val actions: StateFlow<List<Action>> =
        observeActiveActionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * CRITICAL FIX:
     * Make goals eager so they load even if nobody is "collecting" them.
     * The dialog reads activeGoals.value for dropdown content.
     */
    val activeGoals: StateFlow<List<Goal>> =
        observeActiveGoalsUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Prefill helper for edit dialog:
     * Return current linked goalId (if any) so the dropdown shows the right selection.
     */
    suspend fun getLinkedGoalId(actionId: String): String? {
        return actionGoalLinkRepository.observeGoalForAction(actionId)
            .first()
            ?.id
    }

    fun archiveAction(actionId: String) {
        viewModelScope.launch {
            runCatching { archiveActionUseCase(actionId) }
                .onSuccess { _events.tryEmit(PathEvent.ShowSnackbar("Archived")) }
                .onFailure { _events.tryEmit(PathEvent.ShowSnackbar("Could not archive. Try again.")) }
        }
    }

    /**
     * Save = upsert Action + set (or clear) the goal link.
     *
     * Data rule:
     * Linking/unlinking must NOT delete sessions.
     * Our link table uses intervals; we only close/open link rows.
     */
    fun saveAction(
        existing: Action?,
        title: String,
        cadence: ActionCadence,
        targetMinutes: Int?,
        linkedGoalId: String?
    ) {
        viewModelScope.launch {
            val now = timeProvider.nowInstant()

            val actionToSave = if (existing == null) {
                // New action
                Action(
                    id = idProvider.newId(),
                    title = title,
                    description = null, // required by domain model
                    trackingType = ActionTrackingType.TIME, // V1 scope lock
                    cadence = cadence,
                    targetValue = targetMinutes,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now,
                    archivedAt = null
                )
            } else {
                // Edit existing; preserve fields we don’t edit today
                existing.copy(
                    title = title,
                    cadence = cadence,
                    targetValue = targetMinutes,
                    updatedAt = now
                )
            }

            runCatching {
                // 1) Persist action
                upsertActionUseCase(actionToSave)

                // 2) Persist link (or clear link if linkedGoalId == null)
                setGoalForActionUseCase(
                    actionId = actionToSave.id,
                    goalId = linkedGoalId
                )
            }.onSuccess {
                _events.tryEmit(PathEvent.ShowSnackbar("Saved"))
            }.onFailure {
                _events.tryEmit(PathEvent.ShowSnackbar("Could not save. Try again."))
            }
        }
    }
}
