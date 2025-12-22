package com.mahout.app.ui.aim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.aim.usecase.GetWeeklySessionStatsUseCase
import com.mahout.app.domain.aim.usecase.ObserveChiefAimUseCase
import com.mahout.app.domain.aim.usecase.UpsertChiefAimUseCase
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

/**
 * Aim tab ViewModel (Day 10)
 *
 * Responsibilities:
 * - Observe Chief Aim (singleton record) from Room via ObserveChiefAimUseCase
 * - Compute weekly session stats (time-only V1; no "checks") via GetWeeklySessionStatsUseCase
 * - Expose a single UI state stream for the Fragment to render
 * - Emit one-off events (snackbar messages)
 */
@HiltViewModel
class AimViewModel @Inject constructor(
    observeChiefAimUseCase: ObserveChiefAimUseCase,
    private val upsertChiefAimUseCase: UpsertChiefAimUseCase,
    private val getWeeklySessionStatsUseCase: GetWeeklySessionStatsUseCase
) : ViewModel() {

    // One-off events (snackbars, etc.)
    private val _events = MutableSharedFlow<AimEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // Weekly stats state lives separately and is combined with Chief Aim flow.
    private val statsState = MutableStateFlow(WeeklyStatsUi.zero())

    /**
     * Combined UI state:
     * - If no chief aim exists -> Empty(stats)
     * - Else -> Content(chiefAim + stats)
     */
    val uiState: StateFlow<AimUiState> =
        combine(observeChiefAimUseCase(), statsState) { chiefAim, stats ->
            if (chiefAim == null) {
                AimUiState.Empty(stats)
            } else {
                AimUiState.Content(
                    chiefAim = ChiefAimUiModel(
                        title = chiefAim.title,
                        description = chiefAim.description,
                        targetDate = chiefAim.targetDate
                    ),
                    stats = stats
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AimUiState.Loading
        )

    /**
     * Called when Aim becomes visible (Fragment STARTED).
     * This refreshes the 7-day session metrics.
     */
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
                .onFailure {
                    // Soft fail: don't crash UI if stats fail.
                    statsState.value = WeeklyStatsUi.zero()
                }
        }
    }

    /**
     * Saves the singleton Chief Aim (create or update).
     */
    fun saveChiefAim(title: String, description: String?, targetDate: LocalDate?) {
        viewModelScope.launch {
            runCatching { upsertChiefAimUseCase(title, description, targetDate) }
                .onSuccess { _events.tryEmit(AimEvent.ShowSnackbar("Saved")) }
                .onFailure { _events.tryEmit(AimEvent.ShowSnackbar("Could not save. Try again.")) }
        }
    }

    /**
     * Day 10 UI includes this button, but Goals CRUD starts Day 11.
     */
    fun onAddGoalClicked() {
        _events.tryEmit(AimEvent.ShowSnackbar("Coming soon (Day 11)"))
    }

    /**
     * Roadmap filter buttons exist in the screenshot, but logic comes later.
     */
    fun onRoadmapFilterClicked() {
        _events.tryEmit(AimEvent.ShowSnackbar("Coming soon"))
    }

    /**
     * Very small duration formatter:
     * - 0m
     * - 12m
     * - 2h 40m
     */
    private fun formatMillis(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

/** UI states the Fragment renders. */
sealed interface AimUiState {
    data object Loading : AimUiState
    data class Empty(val stats: WeeklyStatsUi) : AimUiState
    data class Content(val chiefAim: ChiefAimUiModel, val stats: WeeklyStatsUi) : AimUiState
}

/** What the Fragment needs to display for Chief Aim. */
data class ChiefAimUiModel(
    val title: String,
    val description: String?,
    val targetDate: LocalDate?
)

/** What the Fragment needs to display for weekly stats (time-only V1). */
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

/** One-off events (snackbars, etc.) */
sealed interface AimEvent {
    data class ShowSnackbar(val message: String) : AimEvent
}
