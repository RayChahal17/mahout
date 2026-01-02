package com.mahout.app.ui.path.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.domain.path.usecase.GetPathInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PathInsightsViewModel @Inject constructor(
    private val getPathInsights: GetPathInsightsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val totalText: String = "Total: 0m",
        val topTaskText: String = "Top task: —",
        val sessionsText: String = "Sessions: 0",
        val tasks: List<TaskUiModel> = emptyList(),
        val period: GetPathInsightsUseCase.TimePeriod = GetPathInsightsUseCase.TimePeriod.TODAY
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val selectedDate: LocalDate = savedStateHandle.get<String>("selectedDate")
        ?.let { LocalDate.parse(it) }
        ?: LocalDate.now()

    private var currentPeriod = GetPathInsightsUseCase.TimePeriod.TODAY

    init {
        refresh()
    }

    fun setPeriod(period: GetPathInsightsUseCase.TimePeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val result = getPathInsights(
                selectedDate = selectedDate,
                period = currentPeriod,
                filterType = GetPathInsightsUseCase.FilterType.LOGGED
            )

            val topTaskText = result.topTask?.let { tt ->
                "Top task: ${tt.actionTitle} (${formatDuration(tt.totalMillis)})"
            } ?: "Top task: —"

            val tasksUi = result.tasks.map {
                TaskUiModel(
                    actionId = it.actionId,
                    title = it.actionTitle,
                    totalMillis = it.totalMillis
                )
            }

            _uiState.value = UiState(
                totalText = "Total: ${formatDuration(result.totalMillis)}",
                topTaskText = topTaskText,
                sessionsText = "Sessions: ${result.sessionCount}",
                tasks = tasksUi,
                period = currentPeriod
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }
}

