package com.mahout.app.ui.path

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.mahout.app.databinding.FragmentPathBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PathViewModel by viewModels()
    private val adapter = ActionListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvActions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActions.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.actions.collect { list ->
                        adapter.submitList(list)

                        // Simple empty state behavior:
                        binding.emptyState.root.isVisible = list.isEmpty()
                        binding.rvActions.isVisible = list.isNotEmpty()
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
