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

@HiltViewModel
class AimViewModel @Inject constructor(
    observeChiefAimUseCase: ObserveChiefAimUseCase,
    private val upsertChiefAimUseCase: UpsertChiefAimUseCase,
    private val getWeeklySessionStatsUseCase: GetWeeklySessionStatsUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<AimEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val statsState = MutableStateFlow(WeeklyStatsUi.zero())

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

    fun refreshWeeklyStats() {
        viewModelScope.launch {
            runCatching { getWeeklySessionStatsUseCase() }
                .onSuccess { s ->
                    statsState.value = WeeklyStatsUi(
                        goalTimeLabel = formatMillis(s.totalMillis),
                        sessionsLabel = s.sessionCount.toString(),
                        activeDaysLabel = "${s.activeDays}/7"
                    )
                }
                .onFailure {
                    // soft fail
                    statsState.value = WeeklyStatsUi.zero()
                }
        }
    }

    fun saveChiefAim(title: String, description: String?, targetDate: LocalDate?) {
        viewModelScope.launch {
            runCatching { upsertChiefAimUseCase(title, description, targetDate) }
                .onSuccess { _events.tryEmit(AimEvent.ShowSnackbar("Saved")) }
                .onFailure { _events.tryEmit(AimEvent.ShowSnackbar("Could not save. Try again.")) }
        }
    }

    fun onAddGoalClicked() {
        _events.tryEmit(AimEvent.ShowSnackbar("Coming soon (Day 11)"))
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
    data class Empty(val stats: WeeklyStatsUi) : AimUiState
    data class Content(val chiefAim: ChiefAimUiModel, val stats: WeeklyStatsUi) : AimUiState
}

data class ChiefAimUiModel(
    val title: String,
    val description: String?,
    val targetDate: LocalDate?
)

data class WeeklyStatsUi(
    val goalTimeLabel: String,
    val sessionsLabel: String,
    val activeDaysLabel: String
) {
    companion object {
        fun zero() = WeeklyStatsUi("0m", "0", "0/7")
    }
}

sealed interface AimEvent {
    data class ShowSnackbar(val message: String) : AimEvent
}
