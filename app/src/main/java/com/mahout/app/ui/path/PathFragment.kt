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

    // We cache latest data so we can render consistently when either "mode" or "data" changes.
    private var latestActions: List<Action> = emptyList()
    private var latestTimerState: TimerState? = null

    // This is the key UI mode state: Actions tab vs Checklist tab.
    private var isActionsMode: Boolean = true

    private var uiTickerJob: Job? = null

    private var pendingStartActionId: String? = null

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val actionId = pendingStartActionId
            pendingStartActionId = null

            if (granted && actionId != null) startTimerService(actionId)
            else binding.root.showSnackbar("Notifications are required to show timer controls.")
        }

    private val adapter = ActionListAdapter(
        onClick = { action -> showEditActionDialog(existing = action) },
        onLongClick = { action -> confirmArchiveAction(action) },
        onTimerClick = { action -> onActionTimerClick(action) }
    )

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

        // Grid like your mock (2 columns)
        binding.rvActions.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvActions.adapter = adapter

        // Default selection = Actions
        binding.toggleMode.check(binding.btnModeActions.id)
        isActionsMode = true
        renderContent()

        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isActionsMode = (checkedId == binding.btnModeActions.id)
            renderContent()
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

    /**
     * ✅ Single source of truth for visibility.
     * This prevents the classic bug: RV becomes invisible and never comes back.
     */
    private fun renderContent() {
        val hasActions = latestActions.isNotEmpty()

        // Actions mode: show either RV or empty state.
        binding.rvActions.isVisible = isActionsMode && hasActions
        binding.emptyState.root.isVisible = isActionsMode && !hasActions

        // Checklist mode: hide action views, show placeholder.
        binding.tvChecklistPlaceholder.isVisible = !isActionsMode

        // Timer tray is independent of mode.
        latestTimerState?.let { renderTimerTray(it) }
    }

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.actions.collect { list ->
                        latestActions = list
                        adapter.submitList(list)

                        // IMPORTANT: Always re-render visibility using latest data.
                        renderContent()
                    }
                }

                launch {
                    viewModel.timerState.collect { state ->
                        latestTimerState = state
                        adapter.updateTimerState(state)

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

    /**
     * TimerState doesn't emit every second. We update elapsed locally while RUNNING.
     */
    private fun startUiTicker() {
        if (uiTickerJob?.isActive == true) return
        uiTickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                latestTimerState?.let { updateTrayElapsedOnly(it) }
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

        updateTrayElapsedOnly(state)
    }

    private fun updateTrayElapsedOnly(state: TimerState) {
        val tray = binding.timerTray

        val runningExtraMillis = if (state.status == TimerStatus.RUNNING) {
            val now = System.currentTimeMillis()
            (now - state.updatedAt.toEpochMilli()).coerceAtLeast(0L)
        } else 0L

        val totalMillis = (state.accumulatedMillis + runningExtraMillis).coerceAtLeast(0L)
        val d = Duration.ofMillis(totalMillis)

        val hours = d.toHours()
        val minutes = (d.toMinutes() % 60)
        val seconds = (d.seconds % 60)

        tray.tvTrayElapsed.text = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun onActionTimerClick(action: Action) {
        val state = latestTimerState

        when (state?.status ?: TimerStatus.STOPPED) {
            TimerStatus.STOPPED -> ensureNotificationPermissionThenStart(action.id)

            TimerStatus.RUNNING,
            TimerStatus.PAUSED -> {
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

        if (granted) startTimerService(actionId)
        else {
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
     * Uses your dialog binding IDs:
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
