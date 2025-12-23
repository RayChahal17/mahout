package com.mahout.app.ui.aim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.usecase.ArchiveGoalUseCase
import com.mahout.app.domain.aim.usecase.GetWeeklySessionStatsUseCase
import com.mahout.app.domain.aim.usecase.ObserveChiefAimUseCase
import com.mahout.app.domain.aim.usecase.ObserveGoalsUseCase
import com.mahout.app.domain.aim.usecase.UpsertChiefAimUseCase
import com.mahout.app.domain.aim.usecase.UpsertGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AimViewModel @Inject constructor(
    observeChiefAimUseCase: ObserveChiefAimUseCase,
    observeGoalsUseCase: ObserveGoalsUseCase,
    private val upsertChiefAimUseCase: UpsertChiefAimUseCase,
    private val upsertGoalUseCase: UpsertGoalUseCase,
    private val archiveGoalUseCase: ArchiveGoalUseCase,
    private val getWeeklySessionStatsUseCase: GetWeeklySessionStatsUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<AimEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val statsState = MutableStateFlow(WeeklyStatsUi.zero())

    // Which timeline bucket is selected (Next 30 days, 1–6 months, etc.)
    private val bucketState = MutableStateFlow(RoadmapBucket.NEXT_30_DAYS)

    fun onRoadmapBucketSelected(bucket: RoadmapBucket) {
        bucketState.value = bucket
    }

    val uiState: StateFlow<AimUiState> =
        combine(
            observeChiefAimUseCase(),
            observeGoalsUseCase(), // IMPORTANT: includes archived goals too
            statsState,
            bucketState
        ) { chiefAim, allGoals, stats, bucket ->

            // Filter by bucket:
            // - ARCHIVED shows archived only
            // - Other buckets show non-archived goals with matching horizon
            val filteredGoals = when (bucket) {
                RoadmapBucket.ARCHIVED -> allGoals.filter { it.status == GoalStatus.ARCHIVED }

                RoadmapBucket.NEXT_30_DAYS -> allGoals.filter {
                    it.status != GoalStatus.ARCHIVED && it.horizon == GoalHorizon.THIS_MONTH
                }

                RoadmapBucket.ONE_TO_SIX_MONTHS -> allGoals.filter {
                    it.status != GoalStatus.ARCHIVED && it.horizon == GoalHorizon.NEARTERM
                }

                RoadmapBucket.SIX_TO_24_MONTHS -> allGoals.filter {
                    it.status != GoalStatus.ARCHIVED && it.horizon == GoalHorizon.MIDTERM
                }

                RoadmapBucket.TWO_TO_TEN_YEARS -> allGoals.filter {
                    it.status != GoalStatus.ARCHIVED && it.horizon == GoalHorizon.LONGTERM
                }
            }

            // Map domain -> UI rows (small objects; fast for RecyclerView)
            val goalUi = filteredGoals.map { g ->
                GoalRowUiModel(
                    id = g.id,
                    title = g.title,
                    why = g.why,
                    horizon = g.horizon,
                    targetDate = g.targetDate
                )
            }

            if (chiefAim == null) {
                AimUiState.Empty(stats = stats, goals = goalUi, bucket = bucket)
            } else {
                AimUiState.Content(
                    chiefAim = ChiefAimUiModel(
                        title = chiefAim.title,
                        description = chiefAim.description,
                        targetDate = chiefAim.targetDate
                    ),
                    stats = stats,
                    goals = goalUi,
                    bucket = bucket
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AimUiState.Loading
        )

    fun refreshWeeklyStats() {
        viewModelScope.launch {
            runCatching { getWeeklySessionStatsUseCase() }
                .onSuccess { s ->
                    val timeLabel = formatMillis(s.totalMillis)
                    statsState.value = WeeklyStatsUi(
                        goalTimeLabel = timeLabel,
                        sessionsLabel = s.sessionCount.toString(),
                        activeDaysLabel = "${s.activeDays}/7",
                        heroSummary = "This week · $timeLabel · ${s.sessionCount} sessions · Active ${s.activeDays}/7 days"
                    )
                }
                .onFailure { statsState.value = WeeklyStatsUi.zero() }
        }
    }

    fun saveChiefAim(title: String, description: String?, targetDate: LocalDate?) {
        viewModelScope.launch {
            runCatching { upsertChiefAimUseCase(title, description, targetDate) }
                .onSuccess { _events.tryEmit(AimEvent.ShowSnackbar("Chief Aim saved")) }
                .onFailure { e ->
                    // If UseCase throws IllegalArgumentException with a human message, show it.
                    val msg = e.message ?: "Could not save Chief Aim."
                    _events.tryEmit(AimEvent.ShowSnackbar(msg))
                }
        }
    }

    fun saveGoal(
        goalId: String?,
        title: String,
        why: String?,
        horizon: GoalHorizon,
        targetDate: LocalDate?
    ) {
        viewModelScope.launch {
            runCatching {
                upsertGoalUseCase(
                    goalId = goalId,
                    title = title,
                    why = why,
                    horizon = horizon,
                    targetDate = targetDate
                )
            }.onSuccess {
                _events.tryEmit(AimEvent.ShowSnackbar("Goal saved"))
            }.onFailure { e ->
                val msg = e.message ?: "Could not save goal."
                _events.tryEmit(AimEvent.ShowSnackbar(msg))
            }
        }
    }

    fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            runCatching { archiveGoalUseCase(goalId) }
                .onSuccess { _events.tryEmit(AimEvent.ShowSnackbar("Goal archived")) }
                .onFailure { _events.tryEmit(AimEvent.ShowSnackbar("Could not archive goal")) }
        }
    }

    private fun formatMillis(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

sealed interface AimUiState {
    data object Loading : AimUiState

    data class Empty(
        val stats: WeeklyStatsUi,
        val goals: List<GoalRowUiModel>,
        val bucket: RoadmapBucket
    ) : AimUiState

    data class Content(
        val chiefAim: ChiefAimUiModel,
        val stats: WeeklyStatsUi,
        val goals: List<GoalRowUiModel>,
        val bucket: RoadmapBucket
    ) : AimUiState
}

data class ChiefAimUiModel(
    val title: String,
    val description: String?,
    val targetDate: LocalDate?
)

data class WeeklyStatsUi(
    val goalTimeLabel: String,
    val sessionsLabel: String,
    val activeDaysLabel: String,
    val heroSummary: String
) {
    companion object {
        fun zero() = WeeklyStatsUi(
            goalTimeLabel = "0m",
            sessionsLabel = "0",
            activeDaysLabel = "0/7",
            heroSummary = "This week · 0m · 0 sessions · Active 0/7 days"
        )
    }
}

sealed interface AimEvent {
    data class ShowSnackbar(val message: String) : AimEvent
}
