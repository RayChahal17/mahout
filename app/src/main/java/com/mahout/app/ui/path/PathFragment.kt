package com.mahout.app.ui.path

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
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
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathViewModel by viewModels()

    private var latestActions: List<Action> = emptyList()
    private var latestTimerState: TimerState? = null
    private var isActionsMode: Boolean = true

    private var uiTickerJob: Job? = null

    // Used only for notif permission request flow
    private var pendingStartActionId: String? = null

    // Day 13 totals: actionId -> total millis in cadence window
    private var latestTotalsByActionId: Map<String, Long> = emptyMap()

    private lateinit var adapter: ActionListAdapter

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val actionId = pendingStartActionId
            pendingStartActionId = null
            if (granted && actionId != null) {
                startTimerService(actionId)
            } else {
                binding.root.showSnackbar("Notifications are required to show timer controls.")
            }
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

        val today = actions.filter { it.cadence != ActionCadence.WEEKLY }
        val week = actions.filter { it.cadence == ActionCadence.WEEKLY }

        val rows = mutableListOf<PathRow>()

        rows += PathRow.HeaderRow(title = "Today’s actions", showSizeToggle = true)
        rows += today.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "This week’s actions", showSizeToggle = false)
        if (week.isEmpty()) rows += PathRow.MessageRow("No weekly actions yet.")
        else rows += week.map { action -> buildActionRow(action, timerState) }

        rows += PathRow.HeaderRow(title = "Archived actions", showSizeToggle = false)
        rows += PathRow.MessageRow("Archived actions will appear here (next).")

        adapter.submitList(rows)
    }

    private fun buildActionRow(action: Action, timerState: TimerState?): PathRow.ActionRow {
        val isActiveForThisAction =
            timerState != null &&
                    timerState.actionId == action.id &&
                    timerState.status != TimerStatus.STOPPED

        // Day 13: show TRUE totals for every card
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
            } else {
                0
            }

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

        tray.btnTrayStop.setOnClickListener { sendTimerCommand(TimerServiceContract.ACTION_STOP) }

        when (state.status) {
            TimerStatus.RUNNING -> {
                tray.btnTrayPauseResume.text = "Pause"
                tray.btnTrayPauseResume.setOnClickListener {
                    sendTimerCommand(TimerServiceContract.ACTION_PAUSE)
                }
            }

            TimerStatus.PAUSED -> {
                tray.btnTrayPauseResume.text = "Resume"
                tray.btnTrayPauseResume.setOnClickListener {
                    sendTimerCommand(TimerServiceContract.ACTION_RESUME)
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

        // If notifications are disabled, starting the service will feel like "nothing happens".
        val ctxApp = requireContext().applicationContext
        val notificationsEnabled = NotificationManagerCompat.from(ctxApp).areNotificationsEnabled()
        if (!notificationsEnabled) {
            Log.w("PathFragment", "Notifications disabled; cannot show timer notification.")
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctxApp.packageName)
            }
            startActivity(intent)
            return
        }

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

        if (granted) {
            startTimerService(actionId)
        } else {
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
