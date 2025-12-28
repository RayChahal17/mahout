package com.mahout.app.ui.path

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.mahout.app.databinding.DialogEditActionBinding
import com.mahout.app.databinding.DialogLogTimeBinding
import com.mahout.app.databinding.FragmentPathBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.ui.common.showSnackbar
import com.mahout.app.ui.path.timeline.TimelineView
import com.mahout.app.ui.path.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathViewModel by viewModels()

    @Inject lateinit var timerController: TimerController

    private var latestActions: List<Action> = emptyList()
    private var latestTimerState: TimerState? = null
    private var isActionsMode: Boolean = true

    private var uiTickerJob: Job? = null

    // Used only for notif permission request flow
    private var pendingStartActionId: String? = null

    // Day 13 totals: actionId -> total millis in cadence window
    private var latestTotalsByActionId: Map<String, Long> = emptyMap()

    // Timeline cache
    private var latestTimelineDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    private var latestTimelineBlocks = emptyList<com.mahout.app.ui.path.timeline.TimelineBlock>()
    private var latestFollowToday: Boolean = true

    private lateinit var adapter: ActionListAdapter

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val actionId = pendingStartActionId
            pendingStartActionId = null

            if (granted && actionId != null) {
                handleTimerResult(timerController.toggle(actionId), actionIdForRetry = null)
            } else {
                binding.root.showSnackbar("Notifications are required to show timer controls.")
            }
        }

    private object CadenceLabels {
        const val DAILY = "Daily"
        const val WEEKLY = "Weekly"
        const val ONE_TIME = "One-time"
    }

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.timelineView.onHostResumed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.timelineView.setListener(object : TimelineView.Listener {
            override fun onRequestDate(date: LocalDate) = viewModel.requestTimelineDate(date)
            override fun onBackToToday() = viewModel.backToToday()
            override fun onLogTime(date: LocalDate) = showLogTimeDialog(date)
            override fun onStats(date: LocalDate) = binding.root.showSnackbar("Stats coming soon.")
        })

        adapter = ActionListAdapter(
            onActionClick = { action -> showEditActionDialog(existing = action) },
            onActionLongClick = { action -> confirmArchiveAction(action) },
            onActionTimerClick = { action -> onActionTimerClick(action) },
            onToggleCardSize = { adapter.toggleCardSize() }
        )

        val glm = GridLayoutManager(requireContext(), 2)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.currentList.getOrNull(position)) {
                    is PathRow.ActionRow -> 1
                    else -> 2
                }
            }
        }

        binding.rvActions.layoutManager = glm
        binding.rvActions.adapter = adapter

        binding.toggleMode.check(binding.btnModeActions.id)
        isActionsMode = true
        renderMode()

        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isActionsMode = (checkedId == binding.btnModeActions.id)
            renderMode()
        }

        binding.btnAddAction.setOnClickListener { showEditActionDialog(existing = null) }

        collectUi()
    }

    override fun onDestroyView() {
        uiTickerJob?.cancel()
        uiTickerJob = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderMode() {
        binding.rvActions.isVisible = isActionsMode && latestActions.isNotEmpty()
        binding.emptyState.root.isVisible = isActionsMode && latestActions.isEmpty()
        binding.tvChecklistPlaceholder.isVisible = !isActionsMode
    }

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.actions.collect { list ->
                        latestActions = list
                        rebuildRows()
                        renderMode()
                    }
                }

                launch {
                    viewModel.timerState.collect { state ->
                        latestTimerState = state
                        rebuildRows()
                        renderTimerTray(state)
                        if (state.status == TimerStatus.RUNNING) startUiTicker() else stopUiTicker()
                    }
                }

                launch {
                    viewModel.totalsMillisByActionId.collect { map ->
                        latestTotalsByActionId = map
                        rebuildRows()
                        latestTimerState?.let { renderTimerTray(it) }
                    }
                }

                // Timeline collectors
                // Timeline collectors (✅ single, consistent updates)
                launch {
                    combine(
                        viewModel.timelineDate,
                        viewModel.timelineBlocks,
                        viewModel.followToday
                    ) { d, blocks, follow -> Triple(d, blocks, follow) }
                        .collect { (d, blocks, follow) ->
                            latestTimelineDate = d
                            latestTimelineBlocks = blocks
                            latestFollowToday = follow
                            renderTimeline()
                        }
                }


                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is PathEvent.ShowSnackbar -> binding.root.showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun renderTimeline() {
        binding.timelineView.submit(
            date = latestTimelineDate,
            blocks = latestTimelineBlocks,
            followToday = latestFollowToday
        )
    }

    private fun rebuildRows() {
        val timerState = latestTimerState
        val actions = latestActions

        if (actions.isEmpty()) {
            adapter.submitList(emptyList())
            return
        }

        val today = actions.filter { it.cadence != ActionCadence.WEEKLY }
        val week = actions.filter { it.cadence == ActionCadence.WEEKLY }

        val rows = mutableListOf<PathRow>()

        rows += PathRow.HeaderRow(title = "Today’s actions", showSizeToggle = true)
        rows += today.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "This week’s actions", showSizeToggle = false)
        if (week.isNotEmpty()) rows += week.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "Archived actions", showSizeToggle = false)
        rows += PathRow.MessageRow("Archived actions will appear here (next).")

        adapter.submitList(rows)
    }

    private fun buildActionRow(action: Action, timerState: TimerState?): PathRow.ActionRow {
        val isActiveForThisAction =
            timerState != null &&
                    timerState.actionId == action.id &&
                    timerState.status != TimerStatus.STOPPED

        val persistedTotalMillis = latestTotalsByActionId[action.id] ?: 0L
        val totalMillis = if (isActiveForThisAction && timerState != null) {
            computeTimerTotalMillis(timerState)
        } else {
            persistedTotalMillis
        }.coerceAtLeast(0L)

        val targetMinutes = action.targetValue ?: 0
        val targetMs = if (targetMinutes > 0) TimeUnit.MINUTES.toMillis(targetMinutes.toLong()) else 0L
        val isOverTarget = targetMs > 0L && totalMillis > targetMs

        val percent =
            if (targetMs > 0L) {
                ((minOf(totalMillis, targetMs) * 100L) / targetMs).toInt().coerceIn(0, 100)
            } else 0

        val progressLabel = buildProgressLabel(totalMillis, targetMs)

        val cadenceText = when (action.cadence) {
            ActionCadence.DAILY -> "Repeats daily"
            ActionCadence.WEEKLY -> "Repeats weekly"
            ActionCadence.ONE_TIME -> "One-time"
        }

        val metaText =
            if (targetMinutes > 0) "$cadenceText • Target ${formatTargetMinutes(targetMinutes)}"
            else cadenceText

        val timerButtonText: String
        val timerEnabled: Boolean

        if (timerState == null || timerState.status == TimerStatus.STOPPED) {
            timerButtonText = "Start"
            timerEnabled = true
        } else {
            if (timerState.actionId == action.id) {
                timerButtonText = "Stop"
                timerEnabled = true
            } else {
                timerButtonText = "Start"
                timerEnabled = false
            }
        }

        return PathRow.ActionRow(
            action = action,
            metaText = metaText,
            progressPercent = percent,
            progressLabel = progressLabel,
            timerButtonText = timerButtonText,
            timerButtonEnabled = timerEnabled,
            isOverTarget = isOverTarget
        )
    }

    private fun computeTimerTotalMillis(state: TimerState): Long {
        val runningExtra = if (state.status == TimerStatus.RUNNING) {
            val now = System.currentTimeMillis()
            (now - state.updatedAt.toEpochMilli()).coerceAtLeast(0L)
        } else 0L
        return (state.accumulatedMillis + runningExtra).coerceAtLeast(0L)
    }

    private fun startUiTicker() {
        if (uiTickerJob?.isActive == true) return
        uiTickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                latestTimerState?.let { renderTimerTray(it) }
                rebuildRows()
                delay(1_000L)
            }
        }
    }

    private fun stopUiTicker() {
        uiTickerJob?.cancel()
        uiTickerJob = null
    }

    private fun renderTimerTray(state: TimerState) {
        val tray = binding.timerTray

        val show = state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED
        tray.root.isVisible = show
        if (!show) return

        val actionTitle = state.actionId
            ?.let { id -> latestActions.firstOrNull { it.id == id }?.title }
            ?: "Unknown action"
        tray.tvTrayActionTitle.text = actionTitle

        tray.btnTrayStop.setOnClickListener {
            handleTimerResult(timerController.stop(), actionIdForRetry = null)
        }

        when (state.status) {
            TimerStatus.RUNNING -> {
                tray.btnTrayPauseResume.text = "Pause"
                tray.btnTrayPauseResume.setOnClickListener {
                    handleTimerResult(timerController.pause(), actionIdForRetry = null)
                }
            }
            TimerStatus.PAUSED -> {
                tray.btnTrayPauseResume.text = "Resume"
                tray.btnTrayPauseResume.setOnClickListener {
                    handleTimerResult(timerController.resume(), actionIdForRetry = null)
                }
            }
            else -> Unit
        }

        val totalMillis = computeTimerTotalMillis(state)
        val d = Duration.ofMillis(totalMillis)
        val hours = d.toHours()
        val minutes = (d.toMinutes() % 60)
        val seconds = (d.seconds % 60)

        tray.tvTrayElapsed.text = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val targetMinutes = state.actionId
            ?.let { id -> latestActions.firstOrNull { it.id == id }?.targetValue }
            ?: 0
        val targetMs = if (targetMinutes > 0) TimeUnit.MINUTES.toMillis(targetMinutes.toLong()) else 0L

        val pct =
            if (targetMs > 0L) {
                ((minOf(totalMillis, targetMs) * 100L) / targetMs).toInt().coerceIn(0, 100)
            } else 0

        tray.pbTrayProgress.progress = pct
        tray.tvTrayProgressLabel.text = buildProgressLabel(totalMillis, targetMs)
    }

    private fun onActionTimerClick(action: Action) {
        val state = latestTimerState
        val currentStatus = state?.status ?: TimerStatus.STOPPED

        when (currentStatus) {
            TimerStatus.STOPPED -> {
                handleTimerResult(
                    result = timerController.toggle(action.id),
                    actionIdForRetry = action.id
                )
            }
            TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                if (state?.actionId == action.id) {
                    handleTimerResult(
                        result = timerController.toggle(action.id),
                        actionIdForRetry = null
                    )
                } else {
                    binding.root.showSnackbar("Stop the current timer first.")
                }
            }
        }
    }

    private fun showAppNotificationSettings() {
        val ctxApp = requireContext().applicationContext
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctxApp.packageName)
        }
        startActivity(intent)
    }

    private fun handleTimerResult(
        result: TimerController.Result,
        actionIdForRetry: String?
    ) {
        when (result) {
            TimerController.Result.Sent -> Unit

            TimerController.Result.NotificationsDisabled -> {
                showAppNotificationSettings()
            }

            TimerController.Result.NeedPostNotificationsPermission -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    binding.root.showSnackbar("Enable notifications to control the timer.")
                    return
                }

                val actionId = actionIdForRetry
                if (actionId == null) {
                    binding.root.showSnackbar("Enable notification permission to control the timer.")
                    return
                }

                pendingStartActionId = actionId
                requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            is TimerController.Result.Error -> {
                Log.e("PathFragment", "TimerController error", result.throwable)
                binding.root.showSnackbar("Timer failed. Try again.")
            }
        }
    }

    private fun confirmArchiveAction(action: Action) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archive action?")
            .setMessage("This will hide the action from your active list.")
            .setPositiveButton("Archive") { _, _ -> viewModel.archiveAction(action.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditActionDialog(existing: Action?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val linkedGoalId: String? = existing?.let { viewModel.getLinkedGoalId(it.id) }
            if (!isAdded) return@launch

            val dialogBinding = DialogEditActionBinding.inflate(layoutInflater)

            dialogBinding.etTitle.setText(existing?.title.orEmpty())
            dialogBinding.etTargetMinutes.setText(existing?.targetValue?.toString().orEmpty())

            val cadenceOptions = listOf(CadenceLabels.DAILY, CadenceLabels.WEEKLY, CadenceLabels.ONE_TIME)
            dialogBinding.actvCadence.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cadenceOptions)
            )

            val initialCadence = when (existing?.cadence ?: ActionCadence.DAILY) {
                ActionCadence.DAILY -> CadenceLabels.DAILY
                ActionCadence.WEEKLY -> CadenceLabels.WEEKLY
                ActionCadence.ONE_TIME -> CadenceLabels.ONE_TIME
            }
            dialogBinding.actvCadence.setText(initialCadence, false)

            val goalTitles = viewModel.activeGoals.value.map { it.title }
            dialogBinding.actvGoalLink.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, goalTitles)
            )

            linkedGoalId?.let { id ->
                val idx = viewModel.activeGoals.value.indexOfFirst { it.id == id }
                if (idx >= 0) dialogBinding.actvGoalLink.setText(goalTitles[idx], false)
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (existing == null) "Add action" else "Edit action")
                .setView(dialogBinding.root)
                .setPositiveButton("Save", null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
                .also { dialog ->
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = dialogBinding.etTitle.text?.toString()?.trim().orEmpty()
                        if (title.isBlank()) {
                            dialogBinding.tilTitle.error = "Title is required"
                            return@setOnClickListener
                        } else dialogBinding.tilTitle.error = null

                        val cadence = when (dialogBinding.actvCadence.text?.toString()?.trim()) {
                            CadenceLabels.WEEKLY -> ActionCadence.WEEKLY
                            CadenceLabels.ONE_TIME -> ActionCadence.ONE_TIME
                            else -> ActionCadence.DAILY
                        }

                        val targetMinutes = dialogBinding.etTargetMinutes.text?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?.toIntOrNull()
                            ?.takeIf { it > 0 }

                        val selectedGoalTitle = dialogBinding.actvGoalLink.text?.toString()?.trim().orEmpty()
                        val selectedGoalId = viewModel.activeGoals.value
                            .firstOrNull { it.title == selectedGoalTitle }
                            ?.id

                        viewModel.saveAction(
                            existingId = existing?.id,
                            title = title,
                            cadence = cadence,
                            targetMinutes = targetMinutes,
                            linkedGoalId = selectedGoalId
                        )

                        dialog.dismiss()
                    }
                }
        }
    }

    private fun showLogTimeDialog(date: LocalDate) {
        val dialogBinding = DialogLogTimeBinding.inflate(layoutInflater)

        val actions = latestActions
        val titles = actions.map { it.title }
        dialogBinding.actvAction.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, titles)
        )

        if (titles.isNotEmpty()) dialogBinding.actvAction.setText(titles.first(), false)

        val now = LocalTime.now()
        val roundedStart = now.withMinute((now.minute / 5) * 5).withSecond(0).withNano(0)
        var start = roundedStart
        var end = start.plusMinutes(10)

        fun renderButtons() {
            dialogBinding.btnStartTime.text = "Start: ${timeFmt.format(start)}"
            dialogBinding.btnEndTime.text = "End: ${timeFmt.format(end)}"
        }
        renderButtons()

        fun pickTime(initial: LocalTime, onPicked: (LocalTime) -> Unit) {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initial.hour)
                .setMinute(initial.minute)
                .setTitleText("Select time")
                .build()

            picker.addOnPositiveButtonClickListener {
                onPicked(LocalTime.of(picker.hour, picker.minute))
            }
            picker.show(parentFragmentManager, "timePicker")
        }

        dialogBinding.btnStartTime.setOnClickListener {
            pickTime(start) {
                start = it
                if (!end.isAfter(start)) end = start.plusMinutes(10)
                renderButtons()
            }
        }

        dialogBinding.btnEndTime.setOnClickListener {
            pickTime(end) {
                end = it
                if (!end.isAfter(start)) end = start.plusMinutes(10)
                renderButtons()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log time")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .show()
            .also { dialog ->
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val selectedTitle = dialogBinding.actvAction.text?.toString()?.trim().orEmpty()
                    val action = actions.firstOrNull { it.title == selectedTitle }

                    if (action == null) {
                        dialogBinding.tilAction.error = "Select an action"
                        return@setOnClickListener
                    } else {
                        dialogBinding.tilAction.error = null
                    }

                    if (!end.isAfter(start)) {
                        binding.root.showSnackbar("End must be after start.")
                        return@setOnClickListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        val conflicts = viewModel.getManualLogConflicts(date, start, end)
                        if (conflicts.isEmpty()) {
                            viewModel.logManualTime(
                                date = date,
                                actionId = action.id,
                                title = action.title,
                                start = start,
                                end = end,
                                strategy = PathViewModel.LogTimeStrategy.OVERWRITE
                            )
                            dialog.dismiss()
                            return@launch
                        }

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Time overlaps existing logs")
                            .setMessage("Choose how to handle the overlap.")
                            .setPositiveButton("Fit around") { _, _ ->
                                viewModel.logManualTime(
                                    date = date,
                                    actionId = action.id,
                                    title = action.title,
                                    start = start,
                                    end = end,
                                    strategy = PathViewModel.LogTimeStrategy.FIT_AROUND
                                )
                                dialog.dismiss()
                            }
                            .setNegativeButton("Overwrite") { _, _ ->
                                viewModel.logManualTime(
                                    date = date,
                                    actionId = action.id,
                                    title = action.title,
                                    start = start,
                                    end = end,
                                    strategy = PathViewModel.LogTimeStrategy.OVERWRITE
                                )
                                dialog.dismiss()
                            }
                            .setNeutralButton("Cancel", null)
                            .show()
                    }
                }
            }
    }

    private fun buildProgressLabel(elapsedMs: Long, targetMs: Long): String {
        val doneMin = (elapsedMs / 60_000L).toInt().coerceAtLeast(0)
        if (targetMs <= 0L) return "${doneMin}m"
        val targetMin = (targetMs / 60_000L).toInt().coerceAtLeast(0)
        return if (elapsedMs < targetMs) {
            "${doneMin}m / ${targetMin}m"
        } else {
            val over = (elapsedMs - targetMs).coerceAtLeast(0L)
            if (over <= 0L) "${doneMin}m / ${targetMin}m"
            else "${doneMin}m / ${targetMin}m (+${formatOverShort(over)})"
        }
    }

    private fun formatOverShort(ms: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun formatTargetMinutes(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        if (m < 60) return "${m}m"
        val h = m / 60
        val rem = m % 60
        return if (rem == 0) "${h}h" else "${h}h ${rem}m"
    }
}
