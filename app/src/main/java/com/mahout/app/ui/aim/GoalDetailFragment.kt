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
import androidx.navigation.fragment.findNavController
import com.mahout.app.R
import com.mahout.app.databinding.FragmentGoalDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class GoalDetailFragment : Fragment() {

    private var _binding: FragmentGoalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalDetailViewModel by viewModels()

    private val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use AppCompat built-in back icon (no new drawable needed).
        binding.toolbar.title = getString(R.string.goal_detail_title)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        binding.progress.isVisible = state is GoalDetailUiState.Loading
                        binding.contentGroup.isVisible = state is GoalDetailUiState.Content
                        binding.notFoundGroup.isVisible = state is GoalDetailUiState.NotFound

                        when (state) {
                            GoalDetailUiState.Loading -> Unit
                            GoalDetailUiState.NotFound -> Unit
                            is GoalDetailUiState.Content -> {
                                val g = state.goal
                                binding.tvTitle.text = g.title
                                binding.tvWhy.isVisible = !g.why.isNullOrBlank()
                                binding.tvWhy.text = g.why.orEmpty()

                                binding.tvMeta.text = buildString {
                                    append(g.horizon.name.replace('_', ' '))
                                    if (g.targetDate != null) {
                                        append(" • Target ")
                                        append(g.targetDate.format(formatter))
                                    }
                                    if (g.status.name == "ARCHIVED") {
                                        append(" • Archived")
                                    }
                                }

                                // Placeholder receipts panel (Day 11 requirement)
                                binding.tvReceiptsTitle.text = getString(R.string.goal_detail_receipts_title)
                                binding.tvReceiptsBody.text = getString(R.string.goal_detail_receipts_body)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
