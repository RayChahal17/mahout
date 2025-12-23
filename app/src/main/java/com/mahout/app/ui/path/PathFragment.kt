package com.mahout.app.ui.path

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.R
import com.mahout.app.databinding.DialogEditActionBinding
import com.mahout.app.databinding.FragmentPathBinding
import com.mahout.app.domain.path.model.Action
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.ui.common.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathViewModel by viewModels()

    // Day 12 interactions:
    // - Tap row => edit dialog
    // - Long press => archive confirm
    private val adapter = ActionListAdapter(
        onClick = { action -> showEditActionDialog(existing = action) },
        onLongClick = { action -> confirmArchiveAction(action) }
    )

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

        binding.rvActions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActions.adapter = adapter

        // Add Action button
        binding.btnAddAction.setOnClickListener {
            showEditActionDialog(existing = null)
        }

        collectUi()
    }

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.actions.collect { list ->
                        adapter.submitList(list)
                        binding.emptyState.root.isVisible = list.isEmpty()
                        binding.rvActions.isVisible = list.isNotEmpty()
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

    private fun confirmArchiveAction(action: Action) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.path_archive_action_title)
            .setMessage(R.string.path_archive_action_body)
            .setPositiveButton(R.string.path_archive_action_confirm) { _, _ ->
                viewModel.archiveAction(action.id)
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    /**
     * Day 12 dialog:
     * - Title (required)
     * - Cadence (dropdown)  ✅ fixed to always open
     * - Target minutes (optional)
     * - Optional link to 1 goal (dropdown) ✅ fixed to persist + prefill
     *
     * NOTE: V1 scope lock: time-only, no checklists.
     */
    private fun showEditActionDialog(existing: Action?) {
        // Prefetch the linked goal before building UI so we don’t accidentally clear it on Save.
        viewLifecycleOwner.lifecycleScope.launch {
            val linkedGoalId: String? = existing?.let { viewModel.getLinkedGoalId(it.id) }
            if (!isAdded) return@launch

            val dialogBinding = DialogEditActionBinding.inflate(layoutInflater)

            // Prefill
            dialogBinding.etTitle.setText(existing?.title.orEmpty())
            dialogBinding.etTargetMinutes.setText(existing?.targetValue?.toString().orEmpty())

            // -------------------------
            // Cadence dropdown
            // -------------------------
            val cadences = listOf(
                ActionCadence.DAILY,
                ActionCadence.WEEKLY,
                ActionCadence.ONE_TIME
            )

            fun cadenceLabel(c: ActionCadence): String = when (c) {
                ActionCadence.DAILY -> getString(R.string.action_cadence_daily)
                ActionCadence.WEEKLY -> getString(R.string.action_cadence_weekly)
                ActionCadence.ONE_TIME -> getString(R.string.action_cadence_one_time)
            }

            var selectedCadence: ActionCadence = existing?.cadence ?: ActionCadence.DAILY

            dialogBinding.actvCadence.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    cadences.map { cadenceLabel(it) }
                )
            )
            dialogBinding.actvCadence.setText(cadenceLabel(selectedCadence), false)

            // IMPORTANT UX FIX:
            // Some devices won’t show dropdown unless we explicitly call showDropDown().
            dialogBinding.actvCadence.setOnClickListener { dialogBinding.actvCadence.showDropDown() }
            dialogBinding.actvCadence.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) dialogBinding.actvCadence.showDropDown()
            }

            dialogBinding.actvCadence.setOnItemClickListener { _, _, position, _ ->
                selectedCadence = cadences[position]
            }

            // -------------------------
            // Goal link dropdown (0 or 1)
            // -------------------------
            val activeGoals = viewModel.activeGoals.value

            val goalLabels = buildList {
                add(getString(R.string.action_goal_not_linked)) // position 0
                addAll(activeGoals.map { it.title })
            }

            var selectedGoalId: String? = linkedGoalId

            dialogBinding.actvGoalLink.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    goalLabels
                )
            )

            // Prefill goal selection:
            val prefillLabel =
                if (selectedGoalId == null) {
                    getString(R.string.action_goal_not_linked)
                } else {
                    val idx = activeGoals.indexOfFirst { it.id == selectedGoalId }
                    if (idx == -1) getString(R.string.action_goal_not_linked) else activeGoals[idx].title
                }
            dialogBinding.actvGoalLink.setText(prefillLabel, false)

            dialogBinding.tilGoalLink.helperText =
                if (activeGoals.isEmpty()) getString(R.string.action_goal_no_goals_helper) else null

            // Same dropdown reliability fix as cadence:
            dialogBinding.actvGoalLink.setOnClickListener { dialogBinding.actvGoalLink.showDropDown() }
            dialogBinding.actvGoalLink.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) dialogBinding.actvGoalLink.showDropDown()
            }

            dialogBinding.actvGoalLink.setOnItemClickListener { _, _, position, _ ->
                selectedGoalId =
                    if (position == 0) null
                    else activeGoals[position - 1].id
            }

            // -------------------------
            // Build dialog safely (no resource-id-0 crashes)
            // -------------------------
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (existing == null) R.string.action_dialog_title_add else R.string.action_dialog_title_edit)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.common_save, null)   // override for validation
                .setNegativeButton(R.string.common_cancel, null) // default dismiss

            if (existing != null) {
                builder.setNeutralButton(R.string.path_archive_action_confirm, null)
            }

            val dialog = builder.create()

            dialog.setOnShowListener {
                // SAVE handler with validation
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val title = dialogBinding.etTitle.text?.toString().orEmpty().trim()
                    val targetMinutesRaw = dialogBinding.etTargetMinutes.text?.toString()?.trim()

                    if (title.isBlank()) {
                        dialogBinding.tilTitle.error = getString(R.string.action_error_title_required)
                        return@setOnClickListener
                    } else {
                        dialogBinding.tilTitle.error = null
                    }

                    val targetMinutes: Int? = when {
                        targetMinutesRaw.isNullOrBlank() -> null
                        else -> targetMinutesRaw.toIntOrNull()
                    }

                    // If they typed something non-numeric, show a clear error.
                    if (!targetMinutesRaw.isNullOrBlank() && targetMinutes == null) {
                        dialogBinding.tilTargetMinutes.error =
                            getString(R.string.action_error_target_minutes_number)
                        return@setOnClickListener
                    }

                    if (targetMinutes != null && targetMinutes <= 0) {
                        dialogBinding.tilTargetMinutes.error =
                            getString(R.string.action_error_target_minutes_positive)
                        return@setOnClickListener
                    } else {
                        dialogBinding.tilTargetMinutes.error = null
                    }

                    viewModel.saveAction(
                        existing = existing,
                        title = title,
                        cadence = selectedCadence,
                        targetMinutes = targetMinutes,
                        linkedGoalId = selectedGoalId
                    )
                    dialog.dismiss()
                }

                // ARCHIVE handler (only when editing)
                if (existing != null) {
                    dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        confirmArchiveAction(existing)
                        dialog.dismiss()
                    }
                }
            }

            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
