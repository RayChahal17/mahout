package com.mahout.app.ui.aim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.aim.usecase.ArchiveGoalUseCase
import com.mahout.app.domain.aim.usecase.GetGoalWeeklyReceiptsUseCase
import com.mahout.app.domain.aim.usecase.GetWeeklySessionStatsUseCase
import com.mahout.app.domain.aim.usecase.ObserveChiefAimUseCase
import com.mahout.app.domain.aim.usecase.ObserveGoalsUseCase
import com.mahout.app.domain.aim.usecase.UpsertChiefAimUseCase
import com.mahout.app.domain.aim.usecase.UpsertGoalUseCase
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AimViewModel @Inject constructor(
    observeChiefAimUseCase: ObserveChiefAimUseCase,
    observeGoalsUseCase: ObserveGoalsUseCase,
    private val upsertChiefAimUseCase: UpsertChiefAimUseCase,
    private val upsertGoalUseCase: UpsertGoalUseCase,
    private val archiveGoalUseCase: ArchiveGoalUseCase,
    getWeeklySessionStatsUseCase: GetWeeklySessionStatsUseCase,
    observeActiveActionsUseCase: ObserveActiveActionsUseCase,
    private val actionGoalLinkRepository: ActionGoalLinkRepository,
    private val getGoalWeeklyReceiptsUseCase: GetGoalWeeklyReceiptsUseCase
) : ViewModel() {

    private val chiefAim = observeChiefAimUseCase()
    private val allGoals = observeGoalsUseCase()

    private val statsState = kotlinx.coroutines.flow.flow {
        emit(getWeeklySessionStatsUseCase())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val bucketState = kotlinx.coroutines.flow.MutableStateFlow(RoadmapBucket.NEXT_30_DAYS)

    private val actions = observeActiveActionsUseCase()
    private val activeLinks = actionGoalLinkRepository.observeActiveLinks()

    private val _events = MutableSharedFlow<AimEvent>()
    val events: SharedFlow<AimEvent> = _events.asSharedFlow()

    private data class CombinedData(
        val chief: com.mahout.app.domain.aim.model.ChiefAim?,
        val goals: List<com.mahout.app.domain.aim.model.Goal>,
        val stats: com.mahout.app.domain.aim.usecase.WeeklySessionStats?,
        val bucket: RoadmapBucket,
        val actions: List<com.mahout.app.domain.path.model.Action>,
        val links: List<com.mahout.app.domain.aim.model.ActionGoalLinkInterval>
    )

    private val combinedData = combine(
        combine(chiefAim, allGoals, statsState, bucketState) { chief: com.mahout.app.domain.aim.model.ChiefAim?,
                                                               goals: List<com.mahout.app.domain.aim.model.Goal>,
                                                               stats: com.mahout.app.domain.aim.usecase.WeeklySessionStats?,
                                                               bucket: RoadmapBucket ->
            CombinedData(chief, goals, stats, bucket, emptyList(), emptyList())
        },
        combine(actions, activeLinks) { actionList: List<com.mahout.app.domain.path.model.Action>,
                                         links: List<com.mahout.app.domain.aim.model.ActionGoalLinkInterval> ->
            actionList to links
        }
    ) { data, (actionList, links) ->
        data.copy(actions = actionList, links = links)
    }

    val uiState: StateFlow<AimUiState> = combinedData.mapLatest { data ->
            val chief = data.chief
            val goals = data.goals
            val stats = data.stats
            val bucket = data.bucket
            val actionList = data.actions
            val links = data.links
            val actionTitleById = actionList.associate { it.id to it.title }
            val activeLinksByGoal = links.groupBy { it.goalId }

            val filteredGoals = goals.filter { g ->
                when (bucket) {
                    RoadmapBucket.NEXT_30_DAYS -> g.status == GoalStatus.ACTIVE && g.horizon == com.mahout.app.domain.aim.model.GoalHorizon.THIS_MONTH
                    RoadmapBucket.ONE_TO_SIX_MONTHS -> g.status == GoalStatus.ACTIVE && g.horizon == com.mahout.app.domain.aim.model.GoalHorizon.NEARTERM
                    RoadmapBucket.SIX_TO_24_MONTHS -> g.status == GoalStatus.ACTIVE && g.horizon == com.mahout.app.domain.aim.model.GoalHorizon.MIDTERM
                    RoadmapBucket.TWO_TO_TEN_YEARS -> g.status == GoalStatus.ACTIVE && g.horizon == com.mahout.app.domain.aim.model.GoalHorizon.LONGTERM
                    RoadmapBucket.ARCHIVED -> g.status == GoalStatus.ARCHIVED
                }
            }

            // Compute weekly stats for all goals asynchronously
            val goalUi = withContext(Dispatchers.Default) {
                filteredGoals.map { g ->
                    val titles = activeLinksByGoal[g.id]
                        .orEmpty()
                        .mapNotNull { actionTitleById[it.actionId] }
                        .sortedBy { it.lowercase(Locale.getDefault()) }

                    val count = titles.size
                    val summary = buildLinkedSummary(count, titles)

                    // Compute weekly stats for this goal
                    val weeklyReceipts = try {
                        getGoalWeeklyReceiptsUseCase(g.id)
                    } catch (e: Exception) {
                        GetGoalWeeklyReceiptsUseCase.Result(emptyList(), emptyMap())
                    }

                    val weeklyTimeMillis = weeklyReceipts.receipts.sumOf { it.durationMillis }
                    val weeklySessionCount = weeklyReceipts.receipts.size
                    val activeDays = weeklyReceipts.receipts
                        .map { it.startAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                        .toSet()
                        .size
                    val lastTouchedDate = weeklyReceipts.receipts
                        .maxOfOrNull { it.startAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                    val progressPercent = ((activeDays.toFloat() / 7f) * 100f).toInt().coerceIn(0, 100)

                    GoalRowUiModel(
                        id = g.id,
                        title = g.title,
                        why = g.why,
                        horizon = g.horizon,
                        targetDate = g.targetDate,
                        parentGoalId = g.parentGoalId,
                        linkedSummary = summary,
                        linkedCount = count,
                        weeklyTimeMillis = weeklyTimeMillis,
                        weeklySessionCount = weeklySessionCount,
                        activeDays = activeDays,
                        lastTouchedDate = lastTouchedDate,
                        progressPercent = progressPercent
                    )
                }
            }

            val weeklyStatsUi = stats?.let { s ->
                val hours = s.totalMillis / (1000 * 60 * 60)
                val minutes = (s.totalMillis % (1000 * 60 * 60)) / (1000 * 60)
                WeeklyStatsUi(
                    goalTimeLabel = "${hours}h ${minutes}m",
                    sessionsLabel = "${s.sessionCount}",
                    activeDaysLabel = "${s.activeDays}/7",
                    heroSummary = buildHeroSummary(s)
                )
            } ?: WeeklyStatsUi(
                goalTimeLabel = "0h 0m",
                sessionsLabel = "0",
                activeDaysLabel = "0/7",
                heroSummary = "No activity this week"
            )

            when {
                chief == null && goalUi.isEmpty() -> AimUiState.Empty(
                    stats = weeklyStatsUi,
                    goals = emptyList(),
                    bucket = bucket
                )
                else -> AimUiState.Content(
                    chiefAim = ChiefAimUiModel(
                        title = chief?.title.orEmpty(),
                        description = chief?.description,
                        targetDate = chief?.targetDate
                    ),
                    stats = weeklyStatsUi,
                    goals = goalUi,
                    bucket = bucket
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AimUiState.Loading
        )

    fun onRoadmapBucketSelected(bucket: RoadmapBucket) {
        bucketState.value = bucket
    }

    fun refreshWeeklyStats() {
        // Stats are computed reactively in statsState flow
        // This method exists for explicit refresh if needed
    }

    fun saveChiefAim(title: String, description: String?, targetDate: LocalDate?) {
        viewModelScope.launch {
            try {
                upsertChiefAimUseCase(title, description, targetDate)
            } catch (e: Exception) {
                _events.emit(AimEvent.ShowSnackbar(e.message ?: "Failed to save Chief Aim"))
            }
        }
    }

    fun saveGoal(
        goalId: String?,
        title: String,
        why: String?,
        horizon: com.mahout.app.domain.aim.model.GoalHorizon,
        targetDate: LocalDate?
    ) {
        viewModelScope.launch {
            try {
                upsertGoalUseCase(
                    goalId = goalId,
                    title = title,
                    why = why,
                    horizon = horizon,
                    targetDate = targetDate
                )
            } catch (e: Exception) {
                _events.emit(AimEvent.ShowSnackbar(e.message ?: "Failed to save Goal"))
            }
        }
    }

    fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            archiveGoalUseCase(goalId)
        }
    }

    private fun buildLinkedSummary(count: Int, titles: List<String>): String {
        if (count <= 0) return ""
        return when (count) {
            1 -> "Linked 1 • ${titles[0]}"
            2 -> "Linked 2 • ${titles[0]}, ${titles[1]}"
            else -> "Linked $count • ${titles[0]}, ${titles[1]}, +${count - 2}"
        }
    }

    private fun buildHeroSummary(stats: com.mahout.app.domain.aim.usecase.WeeklySessionStats): String {
        val hours = stats.totalMillis / (1000 * 60 * 60)
        val minutes = (stats.totalMillis % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}h ${minutes}m • ${stats.sessionCount} sessions • ${stats.activeDays}/7 days"
    }
}

sealed class AimUiState {
    object Loading : AimUiState()
    data class Empty(
        val stats: WeeklyStatsUi,
        val goals: List<GoalRowUiModel>,
        val bucket: RoadmapBucket
    ) : AimUiState()
    data class Content(
        val chiefAim: ChiefAimUiModel,
        val stats: WeeklyStatsUi,
        val goals: List<GoalRowUiModel>,
        val bucket: RoadmapBucket
    ) : AimUiState()
}
