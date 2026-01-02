package com.mahout.app.ui.elephant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mahout.app.R
import com.mahout.app.databinding.BottomSheetElephantHistoryBinding
import com.mahout.app.ui.common.showConfirmDialog
import kotlinx.coroutines.launch

class ElephantHistoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetElephantHistoryBinding? = null
    private val binding: BottomSheetElephantHistoryBinding get() = _binding!!

    // Share the SAME ViewModel instance as ElephantFragment.
    private val viewModel: ElephantViewModel by viewModels({ requireParentFragment() })

    private lateinit var historyAdapter: MoodHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetElephantHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = MoodHistoryAdapter { moodLogId ->
            showConfirmDialog(
                title = getString(R.string.elephant_delete_title),
                message = getString(R.string.elephant_delete_message),
                confirmText = getString(R.string.common_delete),
                cancelText = getString(R.string.common_cancel),
                onConfirm = { viewModel.delete(moodLogId) }
            )
        }

        binding.rvEleHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEleHistory.adapter = historyAdapter
        binding.rvEleHistory.setHasFixedSize(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.tvEleTrends.text = getString(
                        R.string.elephant_trends_summary,
                        state.last7Total,
                        state.lifetimeTotal
                    )

                    historyAdapter.submitList(state.historyRows)
                    binding.tvEleHistoryEmpty.isVisible = !state.hasHistory
                    binding.rvEleHistory.isVisible = state.hasHistory
                }
            }
        }
    }
}
