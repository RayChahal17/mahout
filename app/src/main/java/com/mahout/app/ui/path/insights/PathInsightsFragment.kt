package com.mahout.app.ui.path.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mahout.app.databinding.FragmentPathInsightsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.mahout.app.domain.path.usecase.GetPathInsightsUseCase

@AndroidEntryPoint
class PathInsightsFragment : Fragment() {

    private var _binding: FragmentPathInsightsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathInsightsViewModel by viewModels()
    private val adapter = InsightTaskAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPathInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        binding.btnPeriodToday.setOnClickListener {
            viewModel.setPeriod(GetPathInsightsUseCase.TimePeriod.TODAY)
            updatePeriodButtons(GetPathInsightsUseCase.TimePeriod.TODAY)
        }
        binding.btnPeriodWeek.setOnClickListener {
            viewModel.setPeriod(GetPathInsightsUseCase.TimePeriod.WEEK)
            updatePeriodButtons(GetPathInsightsUseCase.TimePeriod.WEEK)
        }
        binding.btnPeriodMonth.setOnClickListener {
            viewModel.setPeriod(GetPathInsightsUseCase.TimePeriod.MONTH)
            updatePeriodButtons(GetPathInsightsUseCase.TimePeriod.MONTH)
        }
        binding.btnPeriodYear.setOnClickListener {
            viewModel.setPeriod(GetPathInsightsUseCase.TimePeriod.YEAR)
            updatePeriodButtons(GetPathInsightsUseCase.TimePeriod.YEAR)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updatePeriodButtons(state.period)
                    binding.tvTotalInline.text = state.totalText
                    binding.tvTopTaskInline.text = state.topTaskText
                    binding.tvSessionsInline.text = state.sessionsText
                    adapter.submitList(state.tasks)
                    binding.tvTasksCount.text = "${state.tasks.size} tasks"
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updatePeriodButtons(active: GetPathInsightsUseCase.TimePeriod) {
        val activeColor = requireContext().getColor(android.R.color.black)
        val inactiveColor = requireContext().getColor(android.R.color.darker_gray)

        fun setState(btn: com.google.android.material.button.MaterialButton, isActive: Boolean) {
            btn.setTextColor(if (isActive) activeColor else inactiveColor)
            btn.alpha = if (isActive) 1f else 0.6f
        }

        setState(binding.btnPeriodToday, active == GetPathInsightsUseCase.TimePeriod.TODAY)
        setState(binding.btnPeriodWeek, active == GetPathInsightsUseCase.TimePeriod.WEEK)
        setState(binding.btnPeriodMonth, active == GetPathInsightsUseCase.TimePeriod.MONTH)
        setState(binding.btnPeriodYear, active == GetPathInsightsUseCase.TimePeriod.YEAR)
    }
}

