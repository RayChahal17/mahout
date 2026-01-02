package com.mahout.app.ui.elephant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.usecase.DeleteMoodLogUseCase
import com.mahout.app.domain.elephant.usecase.GetMoodTrendsUseCase
import com.mahout.app.domain.elephant.usecase.ObserveMoodLogsUseCase
import com.mahout.app.domain.elephant.usecase.SaveMoodLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

@HiltViewModel
class ElephantViewModel @Inject constructor(
    private val saveMoodLog: SaveMoodLogUseCase,
    private val deleteMoodLog: DeleteMoodLogUseCase,
    private val observeMoodLogs: ObserveMoodLogsUseCase,
    private val getMoodTrends: GetMoodTrendsUseCase,
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class ElephantUiState(
        val moods: List<MoodUi> = MoodCatalog.moods,
        val selectedMoodId: String? = null,
        val logging: Boolean = false,
        val error: String? = null,
        val last7Total: Int = 0,
        val lifetimeTotal: Int = 0,
        val historyRows: List<MoodHistoryRow> = emptyList(),
        val hasHistory: Boolean = false
    )

    sealed interface ElephantEvent {
        data object NavigateToPath : ElephantEvent
    }

    private val _state = MutableStateFlow(
        ElephantUiState(
            selectedMoodId = savedStateHandle.get<String>(KEY_SELECTED_MOOD_ID)
        )
    )
    val state: StateFlow<ElephantUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ElephantEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events: SharedFlow<ElephantEvent> = _events.asSharedFlow()

    // Red-team: prevents accidental double logs from multiple UI callbacks.
    private var lastLoggedMoodId: String? = null
    private var lastLoggedAtMillis: Long = 0L

    // We want to initialize selection from last history entry ONCE (without logging).
    private var selectionInitializedFromHistory = false

    init {
        viewModelScope.launch {
            observeMoodLogs().collect { logs ->
                val zone = ZoneId.systemDefault()
                val nowLocalDate = LocalDate.ofInstant(timeProvider.nowInstant(), zone)

                // Initialize selection to last mood (without logging) if user hasn’t picked yet.
                if (!selectionInitializedFromHistory) {
                    selectionInitializedFromHistory = true
                    val existing = savedStateHandle.get<String>(KEY_SELECTED_MOOD_ID)
                    if (existing.isNullOrBlank() && logs.isNotEmpty()) {
                        val lastMood = logs.first().feeling
                        savedStateHandle[KEY_SELECTED_MOOD_ID] = lastMood
                        _state.update { it.copy(selectedMoodId = lastMood) }
                    } else {
                        _state.update { it.copy(selectedMoodId = existing) }
                    }
                }

                val rows = buildHistoryRows(
                    logs = logs.take(MAX_HISTORY_ITEMS),
                    zone = zone,
                    nowLocalDate = nowLocalDate
                )

                _state.update {
                    it.copy(
                        historyRows = rows,
                        hasHistory = rows.any { row -> row is MoodHistoryRow.Entry }
                    )
                }

                refreshTrends()
            }
        }

        refreshTrends()
    }

    fun selectMood(id: String) {
        savedStateHandle[KEY_SELECTED_MOOD_ID] = id
        _state.update { it.copy(selectedMoodId = id, error = null) }
    }

    /**
     * Called when the user "commits" a mood:
     * - releasing thumb after vertical scroll settles
     * - lifting thumb from dot scrubber
     */
    fun logSelectedMood() {
        val moodId = _state.value.selectedMoodId ?: return
        if (_state.value.logging) return

        // ✅ Special case: user intentionally selected "Not now" => DO NOT LOG.
        if (moodId == MoodCatalog.SKIP_MOOD_ID) {
            // Clear any error and ensure we don't treat this as a "recent log".
            _state.update { it.copy(error = null) }
            lastLoggedMoodId = null
            lastLoggedAtMillis = 0L
            return
        }

        // Red-team: prevent double-logs from close-together callbacks.
        val nowMs = System.currentTimeMillis()
        if (lastLoggedMoodId == moodId && (nowMs - lastLoggedAtMillis) < 700L) return

        viewModelScope.launch {
            _state.update { it.copy(logging = true, error = null) }
            try {
                // #region agent log
                try {
                    File("c:\\Users\\raych\\AndroidStudioProjects\\Mahout\\.cursor\\debug.log")
                        .appendText(
                            """{"sessionId":"debug-session","runId":"pre-fix","hypothesisId":"H1","location":"ElephantViewModel.logSelectedMood","message":"attempting save","data":{"moodId":"$moodId"},"timestamp":${System.currentTimeMillis()}}""" + "\n"
                        )
                } catch (_: Exception) { }
                // #endregion

                saveMoodLog(
                    feeling = moodId,
                    intensity = null,
                    note = null
                )

                lastLoggedMoodId = moodId
                lastLoggedAtMillis = nowMs

                _state.update { it.copy(logging = false) }

                // Temporary behavior until North Star exists:
                _events.tryEmit(ElephantEvent.NavigateToPath)
            } catch (e: Throwable) {
                // #region agent log
                try {
                    File("c:\\Users\\raych\\AndroidStudioProjects\\Mahout\\.cursor\\debug.log")
                        .appendText(
                            """{"sessionId":"debug-session","runId":"pre-fix","hypothesisId":"H2","location":"ElephantViewModel.logSelectedMood","message":"save failed","data":{"moodId":"$moodId","error":"${e.javaClass.simpleName}:${e.message}"},"timestamp":${System.currentTimeMillis()}}""" + "\n"
                        )
                } catch (_: Exception) { }
                // #endregion

                _state.update { it.copy(logging = false, error = "Couldn’t log that. Try again.") }
            }
        }
    }


    fun delete(moodLogId: String) {
        viewModelScope.launch {
            try {
                deleteMoodLog(moodLogId)
            } catch (_: Throwable) {
                _state.update { it.copy(error = "Couldn’t delete. Try again.") }
            }
        }
    }

    private fun refreshTrends() {
        viewModelScope.launch {
            try {
                val trends = getMoodTrends(zone = ZoneId.systemDefault())
                _state.update {
                    it.copy(
                        last7Total = trends.last7Total,
                        lifetimeTotal = trends.lifetimeTotal
                    )
                }
            } catch (_: Throwable) {
                // Trends should never break logging UX.
            }
        }
    }

    private fun buildHistoryRows(
        logs: List<MoodLog>,
        zone: ZoneId,
        nowLocalDate: LocalDate
    ): List<MoodHistoryRow> {
        if (logs.isEmpty()) return emptyList()

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

        val byDay = logs.groupBy { LocalDate.ofInstant(it.at, zone) }
        val days = byDay.keys.sortedDescending()

        val rows = ArrayList<MoodHistoryRow>(logs.size + days.size)

        for (day in days) {
            val headerTitle = when (day) {
                nowLocalDate -> "Today"
                nowLocalDate.minusDays(1) -> "Yesterday"
                else -> day.format(dateFormatter)
            }
            rows.add(MoodHistoryRow.Header(headerTitle))

            val entries = byDay[day].orEmpty().sortedByDescending { it.at }
            for (log in entries) {
                val mood = MoodCatalog.toDisplayMood(log.feeling)
                val timeLabel = log.at.atZone(zone).toLocalTime().format(timeFormatter)
                rows.add(
                    MoodHistoryRow.Entry(
                        id = log.id,
                        moodId = log.feeling,
                        moodLabel = mood.label,
                        iconRes = mood.iconRes,
                        timeLabel = timeLabel,
                        note = log.note
                    )
                )
            }
        }

        return rows
    }

    private companion object {
        private const val KEY_SELECTED_MOOD_ID = "elephant_selected_mood_id"
        private const val MAX_HISTORY_ITEMS = 20
    }
}
