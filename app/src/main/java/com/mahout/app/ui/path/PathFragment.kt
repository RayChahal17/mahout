package com.mahout.app.ui.path

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.databinding.DialogEditActionBinding
import com.mahout.app.databinding.FragmentPathBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.ui.common.showSnackbar
import com.mahout.app.ui.path.timer.TimerForegroundService
import com.mahout.app.ui.path.timer.TimerServiceContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

@AndroidEntryPoint
class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathViewModel by viewModels()

    private var latestActions: List<Action> = emptyList()
    private var latestTimerState: TimerState? = null
    private var isActionsMode: Boolean = true

    private var uiTickerJob: Job? = null
    private var pendingStartActionId: String? = null

    /**
     * ✅ IMPORTANT FIX:
     * We do NOT use `by lazy { ... adapter ... }` because referencing adapter inside its
     * lazy initializer causes Kotlin recursive type inference errors.
     */
    private lateinit var adapter: ActionListAdapter

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val actionId = pendingStartActionId
            pendingStartActionId = null
            if (granted && actionId != null) startTimerService(actionId)
            else binding.root.showSnackbar("Notifications are required to show timer controls.")
        }

    private object CadenceLabels {
        const val DAILY = "Daily"
        const val WEEKLY = "Weekly"
        const val ONE_TIME = "One-time"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Create adapter here (no recursion possible).
        adapter = ActionListAdapter(
            onActionClick = { action -> showEditActionDialog(existing = action) },
            onActionLongClick = { action -> confirmArchiveAction(action) },
            onActionTimerClick = { action -> onActionTimerClick(action) },
            onToggleCardSize = { adapter.toggleCardSize() } // safe now
        )

        // Grid: 2 columns for cards. Headers/messages span across both columns.
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

        // Toggle: Actions vs Checklist
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
                    viewModel.events.collect { event ->
                        when (event) {
                            is PathEvent.ShowSnackbar -> binding.root.showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun rebuildRows() {
        val timerState = latestTimerState
        val actions = latestActions

        if (actions.isEmpty()) {
            adapter.submitList(emptyList())
            return
        }

        // Day 12 safe grouping:
        // - Weekly cadence => "This week's actions"
        // - Everything else => "Today's actions"
        val today = actions.filter { it.cadence != ActionCadence.WEEKLY }
        val week = actions.filter { it.cadence == ActionCadence.WEEKLY }

        val rows = mutableListOf<PathRow>()

        rows += PathRow.HeaderRow(title = "Today’s actions", showSizeToggle = true)
        rows += today.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "This week’s actions", showSizeToggle = false)
        if (week.isEmpty()) rows += PathRow.MessageRow("No weekly actions yet.")
        else rows += week.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "Archived actions", showSizeToggle = false)
        // Day 12: we’re still observing only active actions => archived list placeholder.
        rows += PathRow.MessageRow("Archived actions will appear here (Day 13).")

        adapter.submitList(rows)
    }

    /**
     * Day 12 progress rule (safe):
     * - Only the currently running/paused action shows real progress (from TimerState).
     * - Other actions show 0m until Day 13 where we sum sessions for today/week.
     */
    private fun buildActionRow(action: Action, timerState: TimerState?): PathRow.ActionRow {
        val isActiveForThisAction =
            timerState != null &&
                    timerState.actionId == action.id &&
                    timerState.status != TimerStatus.STOPPED

        val totalMillis = if (isActiveForThisAction) computeTimerTotalMillis(timerState) else 0L
        val minutesDone = (totalMillis / 60_000L).toInt().coerceAtLeast(0)

        val targetMinutes = action.targetValue ?: 0
        val percent =
            if (targetMinutes > 0) ((minutesDone * 100) / targetMinutes).coerceIn(0, 100)
            else 0

        val progressLabel =
            if (targetMinutes > 0) "${minutesDone}m / ${targetMinutes}m"
            else "${minutesDone}m"

        val cadenceText = when (action.cadence) {
            ActionCadence.DAILY -> "Repeats daily"
            ActionCadence.WEEKLY -> "Repeats weekly"
            ActionCadence.ONE_TIME -> "One-time"
        }

        val metaText =
            if (targetMinutes > 0) "$cadenceText • Target ${targetMinutes}m"
            else cadenceText

        // Button logic (safe behavior):
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
            timerButtonEnabled = timerEnabled
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
                // Also rebuild list so the running card’s progress updates.
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

        tray.btnTrayStop.setOnClickListener { sendTimerCommand(TimerServiceContract.ACTION_STOP) }

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

        // Tray progress uses running action’s targetValue if available.
        val targetMinutes = state.actionId
            ?.let { id -> latestActions.firstOrNull { it.id == id }?.targetValue }
            ?: 0

        val doneMinutes = (totalMillis / 60_000L).toInt().coerceAtLeast(0)

        val pct =
            if (targetMinutes > 0) ((doneMinutes * 100) / targetMinutes).coerceIn(0, 100)
            else 0

        tray.pbTrayProgress.progress = pct
        tray.tvTrayProgressLabel.text =
            if (targetMinutes > 0) "${doneMinutes}m / ${targetMinutes}m" else "${doneMinutes}m"
    }

    private fun onActionTimerClick(action: Action) {
        val state = latestTimerState
        when (state?.status ?: TimerStatus.STOPPED) {
            TimerStatus.STOPPED -> ensureNotificationPermissionThenStart(action.id)
            TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                if (state?.actionId == action.id) {
                    sendTimerCommand(TimerServiceContract.ACTION_STOP)
                } else {
                    binding.root.showSnackbar("Stop the current timer first.")
                }
            }
        }
    }

    private fun ensureNotificationPermissionThenStart(actionId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startTimerService(actionId)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) startTimerService(actionId) else {
            pendingStartActionId = actionId
            requestPostNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startTimerService(actionId: String) {
        val ctx = requireContext()
        val intent = Intent(ctx, TimerForegroundService::class.java).apply {
            action = TimerServiceContract.ACTION_START
            putExtra(TimerServiceContract.EXTRA_ACTION_ID, actionId)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    private fun sendTimerCommand(command: String) {
        val ctx = requireContext()
        val intent = Intent(ctx, TimerForegroundService::class.java).apply { action = command }
        ctx.startService(intent)
    }

    private fun confirmArchiveAction(action: Action) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archive action?")
            .setMessage("This will hide the action from your active list.")
            .setPositiveButton("Archive") { _, _ -> viewModel.archiveAction(action.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Dialog uses your real binding IDs:
     * - actvCadence
     * - actvGoalLink
     */
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
}
