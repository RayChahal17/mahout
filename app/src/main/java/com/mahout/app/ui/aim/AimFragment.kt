package com.mahout.app.ui.aim

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.R
import com.mahout.app.databinding.DialogEditChiefAimBinding
import com.mahout.app.databinding.DialogEditGoalBinding
import com.mahout.app.databinding.FragmentAimBinding
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.util.GoalHorizonResolver
import com.mahout.app.ui.common.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AimFragment : Fragment() {

    private var _binding: FragmentAimBinding? = null
    private val binding: FragmentAimBinding get() = _binding!!

    private val viewModel: AimViewModel by viewModels()

    /**
     * Keep the latest Chief Aim so we can:
     * - enforce goal date <= Chief Aim target YEAR (Dec 31 of that year)
     */
    private var lastChiefAim: ChiefAimUiModel? = null

    private val targetFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    private val goalsAdapter = GoalListAdapter(
        onClick = { goal -> showEditGoalDialog(existing = goal) },
        onLongClick = { goal -> showGoalDetailDialog(goal) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Only rvGoals scrolls vertically
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = goalsAdapter

        // Add Goal: if Chief Aim target isn't set, block and explain.
        binding.btnAddGoal.setOnClickListener {
            val chiefAimTarget = lastChiefAim?.targetDate
            if (chiefAimTarget == null) {
                binding.root.showSnackbar(getString(R.string.goal_target_requires_chief_aim_target))
                showEditChiefAimDialog(existing = lastChiefAim)
                return@setOnClickListener
            }
            showEditGoalDialog(existing = null)
        }

        // Chief Aim card tap = edit (or set if empty)
        binding.cardChiefAim.setOnClickListener {
            showEditChiefAimDialog(existing = lastChiefAim)
        }

        // Empty-state CTA inside Chief Aim card
        binding.btnSetChiefAim.setOnClickListener {
            showEditChiefAimDialog(existing = null)
        }

        // Roadmap buckets (horizontal)
        binding.toggleRoadmapBuckets.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val bucket = RoadmapBucket.fromButtonId(checkedId) ?: return@addOnButtonCheckedListener
            viewModel.onRoadmapBucketSelected(bucket)
        }

        collectUi()
    }

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Refresh stats when screen becomes visible
                viewModel.refreshWeeklyStats()

                launch {
                    viewModel.uiState.collect { render(it) }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AimEvent.ShowSnackbar -> binding.root.showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: AimUiState) {
        binding.loadingOverlay.root.isVisible = state is AimUiState.Loading

        when (state) {
            AimUiState.Loading -> {
                lastChiefAim = null
                binding.groupChiefAimEmpty.isVisible = false
                binding.groupChiefAimContent.isVisible = false
                binding.tvGoalsEmpty.isVisible = false
                binding.rvGoals.isVisible = false
                binding.btnSetChiefAim.isVisible = false
            }

            is AimUiState.Empty -> {
                lastChiefAim = null
                bindStats(state.stats)
                bindGoals(state.goals, state.bucket)

                binding.groupChiefAimEmpty.isVisible = true
                binding.groupChiefAimContent.isVisible = false

                // show CTA only when no Chief Aim
                binding.btnSetChiefAim.isVisible = true

                binding.chipHeroSummary.text = state.stats.heroSummary
            }

            is AimUiState.Content -> {
                lastChiefAim = state.chiefAim
                bindStats(state.stats)
                bindGoals(state.goals, state.bucket)

                binding.groupChiefAimEmpty.isVisible = false
                binding.groupChiefAimContent.isVisible = true

                // IMPORTANT: hide the "Set Chief Aim" button once a Chief Aim exists
                binding.btnSetChiefAim.isVisible = false

                binding.tvChiefAimTitle.text = state.chiefAim.title

                val desc = state.chiefAim.description
                binding.tvChiefAimDescription.isVisible = !desc.isNullOrBlank()
                binding.tvChiefAimDescription.text = desc.orEmpty()

                val targetText = state.chiefAim.targetDate?.let { date ->
                    "Target · ${date.format(targetFormatter)}"
                }
                binding.chipTarget.isVisible = !targetText.isNullOrBlank()
                binding.chipTarget.text = targetText.orEmpty()

                binding.chipHeroSummary.text = state.stats.heroSummary
            }
        }
    }

    private fun bindStats(stats: WeeklyStatsUi) {
        binding.tvMetricTimeValue.text = stats.goalTimeLabel
        binding.tvMetricSessionsValue.text = stats.sessionsLabel
        binding.tvMetricActiveDaysValue.text = stats.activeDaysLabel
    }

    private fun bindGoals(goals: List<GoalRowUiModel>, bucket: RoadmapBucket) {
        goalsAdapter.submitList(goals)

        val isEmpty = goals.isEmpty()
        binding.tvGoalsEmpty.isVisible = isEmpty
        binding.rvGoals.isVisible = !isEmpty

        binding.tvGoalsEmpty.text = when (bucket) {
            RoadmapBucket.NEXT_30_DAYS -> getString(R.string.goals_empty_bucket_30_days)
            RoadmapBucket.ONE_TO_SIX_MONTHS -> getString(R.string.goals_empty_bucket_1_6)
            RoadmapBucket.SIX_TO_24_MONTHS -> getString(R.string.goals_empty_bucket_6_24)
            RoadmapBucket.TWO_TO_TEN_YEARS -> getString(R.string.goals_empty_bucket_2_10)
            RoadmapBucket.ARCHIVED -> getString(R.string.goals_empty_bucket_archived)
        }
    }

    // -----------------------
    // Chief Aim dialog (enforce 5–20 year rule)
    // -----------------------
    private fun showEditChiefAimDialog(existing: ChiefAimUiModel?) {
        val dialogBinding = DialogEditChiefAimBinding.inflate(layoutInflater)

        dialogBinding.etTitle.setText(existing?.title.orEmpty())
        dialogBinding.etDescription.setText(existing?.description.orEmpty())

        var selectedTargetDate: LocalDate? = existing?.targetDate

        dialogBinding.tilTarget.helperText = getString(R.string.aim_target_date_range_helper)

        fun renderTargetField() {
            dialogBinding.etTarget.setText(selectedTargetDate?.format(targetFormatter).orEmpty())
        }
        renderTargetField()

        dialogBinding.etTarget.setOnClickListener {
            val today = LocalDate.now()
            val min = today.plusYears(5)
            val max = today.plusYears(20)

            val minMillis = min.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val maxMillis = max.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val validators = listOf(
                DateValidatorPointForward.from(minMillis),
                DateValidatorPointBackward.before(maxMillis + 24 * 60 * 60 * 1000L)
            )

            val constraints = CalendarConstraints.Builder()
                .setStart(minMillis)
                .setEnd(maxMillis)
                .setValidator(CompositeDateValidator.allOf(validators))
                .build()

            val selectionMillis = selectedTargetDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
                ?: minMillis

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.aim_pick_target_date))
                .setCalendarConstraints(constraints)
                .setSelection(selectionMillis)
                .build()

            picker.addOnPositiveButtonClickListener { epochMillis ->
                selectedTargetDate = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                dialogBinding.tilTarget.error = null
                renderTargetField()
            }

            picker.show(childFragmentManager, "chief_aim_target_picker")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.aim_dialog_title_set else R.string.aim_dialog_title_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.common_save, null)
            .setNegativeButton(R.string.common_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.etTitle.text?.toString().orEmpty().trim()
                val desc = dialogBinding.etDescription.text?.toString()

                if (title.isBlank()) {
                    dialogBinding.tilTitle.error = getString(R.string.aim_error_title_required)
                    return@setOnClickListener
                }
                dialogBinding.tilTitle.error = null

                if (selectedTargetDate == null) {
                    dialogBinding.tilTarget.error = getString(R.string.aim_error_target_required)
                    return@setOnClickListener
                }
                dialogBinding.tilTarget.error = null

                viewModel.saveChiefAim(
                    title = title,
                    description = desc,
                    targetDate = selectedTargetDate
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // -----------------------
    // Goal dialog (target date limited by Chief Aim YEAR)
    // -----------------------
    private fun showEditGoalDialog(existing: GoalRowUiModel?) {
        val dialogBinding = DialogEditGoalBinding.inflate(layoutInflater)

        dialogBinding.etGoalTitle.setText(existing?.title.orEmpty())
        dialogBinding.etGoalWhy.setText(existing?.why.orEmpty())

        val horizons = listOf(
            GoalHorizon.THIS_MONTH,
            GoalHorizon.NEARTERM,
            GoalHorizon.MIDTERM,
            GoalHorizon.LONGTERM
        )

        fun horizonLabel(h: GoalHorizon): String = when (h) {
            GoalHorizon.THIS_MONTH -> getString(R.string.goal_horizon_this_month)
            GoalHorizon.NEARTERM -> getString(R.string.goal_horizon_nearterm)
            GoalHorizon.MIDTERM -> getString(R.string.goal_horizon_midterm)
            GoalHorizon.LONGTERM -> getString(R.string.goal_horizon_longterm)
        }

        var selectedTargetDate: LocalDate? = existing?.targetDate
        var selectedHorizon: GoalHorizon = existing?.horizon ?: GoalHorizon.NEARTERM

        val horizonAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            horizons.map { horizonLabel(it) }
        )
        dialogBinding.actvHorizon.setAdapter(horizonAdapter)

        fun syncHorizonUiWithTargetDate() {
            val date = selectedTargetDate
            if (date != null) {
                selectedHorizon = GoalHorizonResolver.fromTargetDate(LocalDate.now(), date)
            }

            dialogBinding.actvHorizon.setText(horizonLabel(selectedHorizon), false)

            val manualEnabled = (date == null)
            dialogBinding.tilHorizon.isEnabled = manualEnabled
            dialogBinding.actvHorizon.isEnabled = manualEnabled

            dialogBinding.tilHorizon.helperText =
                if (date != null) getString(R.string.goal_horizon_auto_from_target) else null
        }

        dialogBinding.actvHorizon.setOnItemClickListener { _, _, position, _ ->
            selectedHorizon = horizons[position]
        }

        syncHorizonUiWithTargetDate()

        val chiefAimTarget = lastChiefAim?.targetDate
        val maxGoalDate = chiefAimTarget?.let { LocalDate.of(it.year, 12, 31) }

        dialogBinding.tilGoalTarget.helperText =
            if (maxGoalDate == null) {
                getString(R.string.goal_target_requires_chief_aim_target)
            } else {
                getString(R.string.goal_target_max_helper, maxGoalDate.format(targetFormatter))
            }

        fun renderTargetField() {
            dialogBinding.etGoalTarget.setText(selectedTargetDate?.format(targetFormatter).orEmpty())
        }
        renderTargetField()

        dialogBinding.etGoalTarget.setOnClickListener {
            if (maxGoalDate == null) {
                binding.root.showSnackbar(getString(R.string.goal_target_requires_chief_aim_target))
                return@setOnClickListener
            }

            val today = LocalDate.now()
            val minMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val maxMillis = maxGoalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val validators = listOf(
                DateValidatorPointForward.from(minMillis),
                DateValidatorPointBackward.before(maxMillis + 24 * 60 * 60 * 1000L)
            )

            val constraints = CalendarConstraints.Builder()
                .setStart(minMillis)
                .setEnd(maxMillis)
                .setValidator(CompositeDateValidator.allOf(validators))
                .build()

            val selectionMillis = selectedTargetDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
                ?: minMillis

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.goal_pick_target_date))
                .setCalendarConstraints(constraints)
                .setSelection(selectionMillis)
                .build()

            picker.addOnPositiveButtonClickListener { epochMillis ->
                selectedTargetDate = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

                dialogBinding.tilGoalTarget.error = null
                renderTargetField()
                syncHorizonUiWithTargetDate()
            }

            picker.show(childFragmentManager, "goal_target_picker")
        }

        dialogBinding.etGoalTarget.setOnLongClickListener {
            selectedTargetDate = null
            dialogBinding.tilGoalTarget.error = null
            renderTargetField()
            syncHorizonUiWithTargetDate()
            true
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.goal_dialog_title_add else R.string.goal_dialog_title_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.common_save, null)
            .setNegativeButton(R.string.common_cancel, null)

        if (existing != null) {
            builder.setNeutralButton(R.string.goal_action_archive, null)
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.etGoalTitle.text?.toString().orEmpty().trim()
                val why = dialogBinding.etGoalWhy.text?.toString()

                if (title.isBlank()) {
                    dialogBinding.tilGoalTitle.error = getString(R.string.goal_error_title_required)
                    return@setOnClickListener
                }
                dialogBinding.tilGoalTitle.error = null

                if (selectedTargetDate != null && maxGoalDate != null && selectedTargetDate!!.isAfter(maxGoalDate)) {
                    dialogBinding.tilGoalTarget.error =
                        getString(R.string.goal_target_error_max, maxGoalDate.format(targetFormatter))
                    return@setOnClickListener
                }

                viewModel.saveGoal(
                    goalId = existing?.id,
                    title = title,
                    why = why,
                    horizon = selectedHorizon,
                    targetDate = selectedTargetDate
                )
                dialog.dismiss()
            }

            if (existing != null) {
                dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.goal_archive_confirm_title)
                        .setMessage(R.string.goal_archive_confirm_body)
                        .setPositiveButton(R.string.goal_action_archive) { _, _ ->
                            viewModel.archiveGoal(existing.id)
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.common_cancel, null)
                        .show()
                }
            }
        }

        dialog.show()
    }

    private fun showGoalDetailDialog(goal: GoalRowUiModel) {
        val args = Bundle().apply { putString("goalId", goal.id) }
        findNavController().navigate(R.id.action_aimFragment_to_goalDetailFragment, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
