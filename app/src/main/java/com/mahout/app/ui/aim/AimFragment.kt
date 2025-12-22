package com.mahout.app.ui.aim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.R
import com.mahout.app.databinding.DialogEditChiefAimBinding
import com.mahout.app.databinding.FragmentAimBinding
import com.mahout.app.ui.common.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Aim tab Fragment (Day 10)
 *
 * Responsibilities:
 * - Render UI from AimViewModel.uiState
 * - Show edit dialog for Chief Aim
 * - Trigger weekly stats refresh when screen becomes visible
 *
 * NOTE: This Fragment expects your fragment_aim.xml to have:
 * - loadingOverlay include with id loadingOverlay
 * - groups: groupChiefAimEmpty, groupChiefAimContent
 * - views: tvChiefAimTitle, tvChiefAimDescription, chipTarget, chipHeroSummary
 * - buttons: btnAddGoal, btnSetChiefAim
 * - roadmap filter buttons: btnFilter30Days, btnFilter1to6, btnFilter6to24, btnFilter2to10
 * - metric text views: tvMetricTimeValue, tvMetricSessionsValue, tvMetricActiveDaysValue
 */
@AndroidEntryPoint
class AimFragment : Fragment() {

    private var _binding: FragmentAimBinding? = null
    private val binding: FragmentAimBinding get() = _binding!!

    private val viewModel: AimViewModel by viewModels()

    // Used to prefill the edit dialog
    private var lastChiefAim: ChiefAimUiModel? = null

    private val targetFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

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

        // Top-right button (+ Add Goal) exists visually now; behavior comes Day 11.
        binding.btnAddGoal.setOnClickListener { viewModel.onAddGoalClicked() }

        // Tap hero card to edit (matches your screenshot UX)
        binding.cardChiefAim.setOnClickListener {
            showEditChiefAimDialog(existing = lastChiefAim)
        }

        // Empty state CTA
        binding.btnSetChiefAim.setOnClickListener {
            showEditChiefAimDialog(existing = null)
        }

        // Roadmap filters: UI exists Day 10, logic comes Day 11+
        binding.btnFilter30Days.setOnClickListener { viewModel.onRoadmapFilterClicked() }
        binding.btnFilter1to6.setOnClickListener { viewModel.onRoadmapFilterClicked() }
        binding.btnFilter6to24.setOnClickListener { viewModel.onRoadmapFilterClicked() }
        binding.btnFilter2to10.setOnClickListener { viewModel.onRoadmapFilterClicked() }

        collectUi()
    }

    private fun collectUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Refresh weekly stats whenever this screen becomes visible.
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
                binding.groupChiefAimEmpty.isVisible = false
                binding.groupChiefAimContent.isVisible = false
            }

            is AimUiState.Empty -> {
                lastChiefAim = null
                bindStats(state.stats)

                binding.groupChiefAimEmpty.isVisible = true
                binding.groupChiefAimContent.isVisible = false

                // Keep hero summary updated even in empty state
                binding.chipHeroSummary.text = state.stats.heroSummary
            }

            is AimUiState.Content -> {
                lastChiefAim = state.chiefAim
                bindStats(state.stats)

                binding.groupChiefAimEmpty.isVisible = false
                binding.groupChiefAimContent.isVisible = true

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

    private fun showEditChiefAimDialog(existing: ChiefAimUiModel?) {
        val dialogBinding = DialogEditChiefAimBinding.inflate(layoutInflater)

        dialogBinding.etTitle.setText(existing?.title.orEmpty())
        dialogBinding.etDescription.setText(existing?.description.orEmpty())

        // We store a LocalDate in-memory while the dialog is open.
        var selectedTargetDate: LocalDate? = existing?.targetDate

        fun renderTargetField() {
            dialogBinding.etTarget.setText(
                selectedTargetDate?.format(targetFormatter).orEmpty()
            )
        }

        renderTargetField()

        dialogBinding.etTarget.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.aim_pick_target_date))
                .build()

            picker.addOnPositiveButtonClickListener { epochMillis ->
                // MaterialDatePicker returns UTC midnight millis. Convert using UTC to preserve the chosen calendar date.
                selectedTargetDate = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

                renderTargetField()
            }

            picker.show(childFragmentManager, "chief_aim_target_picker")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.aim_dialog_title_set else R.string.aim_dialog_title_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.common_save, null) // override for validation
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
