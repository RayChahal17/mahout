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
import com.mahout.app.domain.path.model.Session
import com.mahout.app.domain.path.model.SessionSource
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.SessionRepository
import com.mahout.app.domain.path.usecase.ArchiveActionUseCase
import com.mahout.app.domain.path.usecase.ObserveActiveActionsUseCase
import com.mahout.app.domain.path.usecase.UpsertActionUseCase
import com.mahout.app.domain.path.usecase.timer.ObserveTimerStateUseCase
import com.mahout.app.ui.path.timeline.ActionColors
import com.mahout.app.ui.path.timeline.TimelineBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class PathViewModel @Inject constructor(
    observeActiveActionsUseCase: ObserveActiveActionsUseCase,
    observeActiveGoalsUseCase: ObserveActiveGoalsUseCase,
    observeTimerStateUseCase: ObserveTimerStateUseCase,
    private val sessionRepository: SessionRepository,
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

    val timerState: StateFlow<TimerState> =
        observeTimerStateUseCase()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TimerState.stopped(timeProvider.nowInstant())
            )

    private val _totalsMillisByActionId = MutableStateFlow<Map<String, Long>>(emptyMap())
    val totalsMillisByActionId: StateFlow<Map<String, Long>> = _totalsMillisByActionId.asStateFlow()

    // Timeline state
    private val zone = ZoneId.systemDefault()
    private val _timelineDate = MutableStateFlow(LocalDate.now(zone))
    val timelineDate: StateFlow<LocalDate> = _timelineDate.asStateFlow()

    private val _followToday = MutableStateFlow(true)
    val followToday: StateFlow<Boolean> = _followToday.asStateFlow()

    enum class LogTimeStrategy { OVERWRITE, FIT_AROUND }

    /**
     * ✅ Ticker that drives "live" refresh.
     * Important: this is cancellation-safe (no stale date blocks overwriting newer ones).
     */
    private val timelineTicker =
        timerState
            .map { it.status == TimerStatus.RUNNING }
            .distinctUntilChanged()
            .flatMapLatest { running ->
                flow {
                    while (currentCoroutineContext().isActive) {
                        emit(Unit)
                        // Timeline is 10-min slots; we don't need 1s refresh.
                        delay(if (running) 5_000L else 30_000L)
                    }
                }
            }

    /**
     * ✅ Timeline blocks are computed reactively + mapLatest cancels stale computations.
     * This prevents "sometimes visible / wrong times / disappears then reappears" during scroll.
     */
    val timelineBlocks: StateFlow<List<TimelineBlock>> =
        combine(_timelineDate, actions, timelineTicker) { date, list, _ -> date to list }
            .mapLatest { (date, list) ->
                if (list.isEmpty()) emptyList() else computeTimelineBlocks(date, list)
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Totals refresher (your existing Day 13 logic)
        viewModelScope.launch {
            while (isActive) {
                val list = actions.value
                _totalsMillisByActionId.value = if (list.isEmpty()) emptyMap() else computeTotals(list)

                val delayMs = if (timerState.value.status == TimerStatus.RUNNING) 1_000L else 15_000L
                delay(delayMs)
            }
        }

        // Midnight rollover fixer: if followToday=true, snap to today after date changes.
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                if (_followToday.value) {
                    val today = LocalDate.now(zone)
                    if (_timelineDate.value != today) _timelineDate.value = today
                }
            }
        }
    }

    fun requestTimelineDate(date: LocalDate) {
        _followToday.value = false
        _timelineDate.value = date
    }

    fun backToToday() {
        _followToday.value = true
        _timelineDate.value = LocalDate.now(zone)
    }

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
                    trackingType = ActionTrackingType.TIME,
                    cadence = cadence,
                    targetValue = targetMinutes,
                    isArchived = false,
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

    suspend fun getManualLogConflicts(date: LocalDate, start: LocalTime, end: LocalTime): List<Session> =
        withContext(Dispatchers.IO) {
            val (fromI, toI) = dateWindow(date, start, end)
            val queryFrom = fromI.minusSeconds(24 * 3600L)
            val queryTo = toI.plusSeconds(1)
            sessionRepository.getSessionsInRange(queryFrom, queryTo)
                .filter { s ->
                    val sEnd = s.endAt ?: timeProvider.nowInstant()
                    overlaps(s.startAt, sEnd, fromI, toI)
                }
        }

    fun logManualTime(
        date: LocalDate,
        actionId: String,
        title: String,
        start: LocalTime,
        end: LocalTime,
        strategy: LogTimeStrategy
    ) {
        viewModelScope.launch {
            runCatching {
                val (fromI, toI) = dateWindow(date, start, end)
                val now = timeProvider.nowInstant()

                val queryFrom = fromI.minusSeconds(24 * 3600L)
                val queryTo = toI.plusSeconds(1)
                val existing = sessionRepository.getSessionsInRange(queryFrom, queryTo)
                val overlaps = existing.filter { s ->
                    val sEnd = s.endAt ?: now
                    overlaps(s.startAt, sEnd, fromI, toI)
                }

                when (strategy) {
                    LogTimeStrategy.OVERWRITE -> {
                        overlaps.forEach { sessionRepository.delete(it.id) }
                        upsertManualSession(actionId, fromI, toI, title)
                    }

                    LogTimeStrategy.FIT_AROUND -> {
                        val gaps = computeGaps(fromI, toI, overlaps.map {
                            val sEnd = it.endAt ?: now
                            it.startAt to sEnd
                        })
                        gaps.forEach { (a, b) ->
                            if (b.isAfter(a)) upsertManualSession(actionId, a, b, title)
                        }
                    }
                }
            }
                .onSuccess { _events.tryEmit(PathEvent.ShowSnackbar("Time logged")) }
                .onFailure { _events.tryEmit(PathEvent.ShowSnackbar(it.message ?: "Failed to log time")) }
        }
    }

    private suspend fun upsertManualSession(actionId: String, startAt: Instant, endAt: Instant, note: String?) {
        val now = timeProvider.nowInstant()
        val dur = (endAt.toEpochMilli() - startAt.toEpochMilli()).coerceAtLeast(0L)
        val session = Session(
            id = idProvider.newId(),
            actionId = actionId,
            startAt = startAt,
            endAt = endAt,
            durationMillis = dur,
            source = SessionSource.MANUAL,
            note = note,
            createdAt = now,
            updatedAt = now
        )
        sessionRepository.upsert(session)
    }

    private fun dateWindow(date: LocalDate, start: LocalTime, end: LocalTime): Pair<Instant, Instant> {
        val s = date.atTime(start).atZone(zone).toInstant()
        val e = date.atTime(end).atZone(zone).toInstant()
        val from = minOf(s, e)
        val to = maxOf(s, e)
        return from to to
    }

    private fun overlaps(aStart: Instant, aEnd: Instant, bStart: Instant, bEnd: Instant): Boolean {
        val start = maxOf(aStart, bStart)
        val end = minOf(aEnd, bEnd)
        return end.isAfter(start)
    }

    private fun computeGaps(from: Instant, to: Instant, intervals: List<Pair<Instant, Instant>>): List<Pair<Instant, Instant>> {
        if (intervals.isEmpty()) return listOf(from to to)

        val sorted = intervals
            .map { (a, b) -> (minOf(a, b) to maxOf(a, b)) }
            .sortedBy { it.first }

        // Merge overlaps
        val merged = mutableListOf<Pair<Instant, Instant>>()
        for (i in sorted) {
            val last = merged.lastOrNull()
            if (last == null) merged += i
            else {
                if (!i.first.isAfter(last.second)) {
                    merged[merged.lastIndex] = last.first to maxOf(last.second, i.second)
                } else merged += i
            }
        }

        // Clamp + compute gaps
        val clamped = merged.mapNotNull { (a, b) ->
            val s = maxOf(a, from)
            val e = minOf(b, to)
            if (e.isAfter(s)) s to e else null
        }.sortedBy { it.first }

        val gaps = mutableListOf<Pair<Instant, Instant>>()
        var cursor = from
        for ((s, e) in clamped) {
            if (s.isAfter(cursor)) gaps += cursor to s
            cursor = maxOf(cursor, e)
        }
        if (to.isAfter(cursor)) gaps += cursor to to
        return gaps
    }

    /**
     * ✅ FIXES INCLUDED:
     * - Cancellation-safe publishing handled by mapLatest upstream
     * - Rounds to minutes (no second jitter / “uncertain blocks”)
     * - Keeps midnight end consistent (DayTimelineView will treat end=00:00 as day-end when appropriate)
     */
    private suspend fun computeTimelineBlocks(date: LocalDate, actions: List<Action>): List<TimelineBlock> =
        withContext(Dispatchers.IO) {

            val dayStart = date.atStartOfDay(zone).toInstant()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
            val now = timeProvider.nowInstant()

            val queryFrom = dayStart.minusSeconds(24 * 3600L)
            val queryTo = dayEnd.plusSeconds(1)

            val sessions = sessionRepository.getSessionsInRange(queryFrom, queryTo)
            val titleById = actions.associate { it.id to it.title }

            // prime colors once per compute (stable)
            ActionColors.prime(actions.map { it.id })

            fun floorToMinute(i: Instant): LocalTime {
                val t = i.atZone(zone).toLocalTime()
                return t.withSecond(0).withNano(0)
            }

            fun ceilToMinute(i: Instant): LocalTime {
                val t = i.atZone(zone).toLocalTime()
                val flo = t.withSecond(0).withNano(0)
                return if (t == flo) flo else flo.plusMinutes(1)
            }

            // Clamp + map
            val rawBlocks = sessions.mapNotNull { s ->
                val endAt = s.endAt ?: now
                if (!overlaps(s.startAt, endAt, dayStart, dayEnd)) return@mapNotNull null

                val clampedStart = maxOf(s.startAt, dayStart)
                val clampedEnd = minOf(endAt, dayEnd)
                if (!clampedEnd.isAfter(clampedStart)) return@mapNotNull null

                val actionTitle = titleById[s.actionId] ?: "Action"
                val startLocal = floorToMinute(clampedStart)
                val endLocal = ceilToMinute(clampedEnd)

                TimelineBlock(
                    id = s.id,
                    actionId = s.actionId,
                    title = actionTitle,
                    start = startLocal,
                    end = endLocal,
                    color = ActionColors.forActionId(s.actionId)
                )
            }

            // Merge adjacent blocks of same action if they touch
            rawBlocks
                .sortedWith(compareBy({ it.actionId }, { it.start }))
                .groupBy { it.actionId }
                .values
                .flatMap { list ->
                    val sorted = list.sortedBy { it.start }
                    val merged = mutableListOf<TimelineBlock>()
                    for (b in sorted) {
                        val last = merged.lastOrNull()
                        if (last == null) merged += b
                        else {
                            if (last.end == b.start) {
                                merged[merged.lastIndex] = last.copy(end = b.end)
                            } else merged += b
                        }
                    }
                    merged
                }
                .sortedBy { it.start }
        }

    private suspend fun computeTotals(actions: List<Action>): Map<String, Long> = withContext(Dispatchers.IO) {
        val now = timeProvider.nowInstant()
        val zone = ZoneId.systemDefault()

        val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val startOfWeek = now.atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()

        val weekQueryFrom = startOfWeek.minusSeconds(7 * 24 * 3600L)
        val to = now.plusSeconds(1)

        val sessionsWeek = sessionRepository.getSessionsInRange(weekQueryFrom, to)
        val needsLifetime = actions.any { it.cadence == ActionCadence.ONE_TIME }
        val sessionsLifetime = if (needsLifetime) sessionRepository.getSessionsInRange(Instant.EPOCH, to) else emptyList()

        fun windowFrom(cadence: ActionCadence): Instant = when (cadence) {
            ActionCadence.DAILY -> startOfDay
            ActionCadence.WEEKLY -> startOfWeek
            ActionCadence.ONE_TIME -> Instant.EPOCH
        }

        fun overlapMillis(s: Session, from: Instant, toI: Instant): Long {
            val end = (s.endAt ?: now)
            val start = maxOf(s.startAt, from)
            val stop = minOf(end, toI)
            val dur = stop.toEpochMilli() - start.toEpochMilli()
            return dur.coerceAtLeast(0L)
        }

        actions.associate { action ->
            val from = windowFrom(action.cadence)
            val list = if (action.cadence == ActionCadence.ONE_TIME) sessionsLifetime else sessionsWeek
            val total = list
                .asSequence()
                .filter { it.actionId == action.id }
                .sumOf { overlapMillis(it, from, to) }

            action.id to total
        }
    }
}
