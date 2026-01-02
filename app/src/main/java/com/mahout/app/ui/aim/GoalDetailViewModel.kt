package com.mahout.app.ui.aim

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.aim.model.ActionGoalLinkInterval
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.aim.usecase.GetGoalWeeklyReceiptsUseCase
import com.mahout.app.domain.aim.usecase.ObserveGoalUseCase
import com.mahout.app.domain.aim.usecase.SetGoalForActionUseCase
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import com.mahout.app.domain.path.usecase.timer.ObserveTimerStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGoalUseCase: ObserveGoalUseCase,
    observeActiveActionsUseCase: ObserveActiveActionsUseCase,
    observeTimerStateUseCase: ObserveTimerStateUseCase,
    private val actionGoalLinkRepository: ActionGoalLinkRepository,
    private val setGoalForActionUseCase: SetGoalForActionUseCase,
    private val getGoalWeeklyReceiptsUseCase: GetGoalWeeklyReceiptsUseCase
) : ViewModel() {

    private val goalId: String = requireNotNull(savedStateHandle["goalId"])
    private val zone = ZoneId.systemDefault()

    private val allActions =
        observeActiveActionsUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val activeLinks: StateFlow<List<ActionGoalLinkInterval>> =
        actionGoalLinkRepository.observeActiveLinks()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val linkableActions: StateFlow<List<ActionOptionUi>> =
        combine(allActions, activeLinks) { actions, links ->
            val linkedActionIds = links.map { it.actionId }.toSet()
            actions
                .filter { it.id !in linkedActionIds }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
                .map { ActionOptionUi(it.id, it.title) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val goalFlow =
        observeGoalUseCase(goalId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Refresh ticker (keeps receipts “live” while timer runs, without spamming)
    private val receiptsTicker =
        observeTimerStateUseCase()
            .map { it.status == TimerStatus.RUNNING }
            .distinctUntilChanged()
            .flatMapLatest { running ->
                flow {
                    while (currentCoroutineContext().isActive) {
                        emit(Unit)
                        delay(if (running) 5_000L else 30_000L)
                    }
                }
            }

    private val weeklyReceipts: StateFlow<GetGoalWeeklyReceiptsUseCase.Result> =
        combine(receiptsTicker, activeLinks) { _, _ -> Unit }
            .mapLatest { getGoalWeeklyReceiptsUseCase(goalId) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                GetGoalWeeklyReceiptsUseCase.Result(emptyList(), emptyMap())
            )

    val uiState: StateFlow<GoalDetailUiState> =
        combine(goalFlow, allActions, activeLinks, weeklyReceipts) { goal, actions, links, receipts ->
            val titleById: Map<String, String> = actions.associate { it.id to it.title }

            val linkedForThisGoal: List<Pair<String, String>> =
                links
                    .filter { it.goalId == goalId }
                    .mapNotNull { link -> titleById[link.actionId]?.let { link.actionId to it } }
                    .distinctBy { it.first }
                    .sortedBy { it.second.lowercase(Locale.getDefault()) }

            val perActionMillis = receipts.perActionWeekMillis

            val linkedCards = linkedForThisGoal.map { (actionId, title) ->
                val ms = perActionMillis[actionId] ?: 0L
                LinkedActionUi(
                    actionId = actionId,
                    title = title,
                    weekText = "This week: ${formatDuration(ms)}"
                )
            }

            val receiptRows = receipts.receipts
                .sortedByDescending { it.startAt }
                .map { r ->
                    val t = titleById[r.actionId] ?: "Action"
                    GoalReceiptUi(
                        id = "${r.actionId}_${r.startAt.epochSecond}",
                        title = t,
                        meta = "${formatReceiptTime(r.startAt)} • ${formatDuration(r.durationMillis)}"
                    )
                }

            GoalDetailUiState(
                goalTitle = goal?.title ?: "Goal",
                goalWhy = goal?.why?.takeIf { it.isNotBlank() },
                linkedActions = linkedCards,
                receipts = receiptRows
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GoalDetailUiState("Goal", null, emptyList(), emptyList())
        )

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun linkAction(actionId: String) {
        viewModelScope.launch {
            runCatching { setGoalForActionUseCase(actionId, goalId) }
                .onFailure { _events.tryEmit("Failed to link action. Try again.") }
        }
    }

    fun unlinkAction(actionId: String) {
        viewModelScope.launch {
            runCatching { setGoalForActionUseCase(actionId, null) }
                .onFailure { _events.tryEmit("Failed to unlink action. Try again.") }
        }
    }

    private fun formatReceiptTime(startAt: Instant): String {
        val dt = startAt.atZone(zone)
        val fmt = DateTimeFormatter.ofPattern("EEE h:mm a", Locale.getDefault())
        return fmt.format(dt)
    }

    private fun formatDuration(ms: Long): String {
        val totalMin = (ms / 60_000L).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h <= 0 -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

data class GoalDetailUiState(
    val goalTitle: String,
    val goalWhy: String?,
    val linkedActions: List<LinkedActionUi>,
    val receipts: List<GoalReceiptUi>
)

data class LinkedActionUi(
    val actionId: String,
    val title: String,
    val weekText: String
)

data class GoalReceiptUi(
    val id: String,
    val title: String,
    val meta: String
)

data class ActionOptionUi(
    val actionId: String,
    val title: String
)
