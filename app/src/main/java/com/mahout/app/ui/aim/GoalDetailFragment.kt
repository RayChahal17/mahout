package com.mahout.app.ui.aim

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.R
import com.mahout.app.databinding.FragmentGoalDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalDetailFragment : Fragment(R.layout.fragment_goal_detail) {

    private var _binding: FragmentGoalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalDetailViewModel by viewModels()

    private lateinit var linkedAdapter: LinkedActionsAdapter
    private lateinit var receiptAdapter: GoalReceiptAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGoalDetailBinding.bind(view)

        // Ensure toolbar back button is enabled
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Handle system back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })

        linkedAdapter = LinkedActionsAdapter { actionId, title ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Unlink action?")
                .setMessage("Unlink \"$title\" from this goal? You can link it again later.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unlink") { _, _ -> viewModel.unlinkAction(actionId) }
                .show()
        }
        binding.rvLinkedActions.adapter = linkedAdapter

        receiptAdapter = GoalReceiptAdapter()
        binding.rvReceipts.adapter = receiptAdapter

        binding.btnLinkAction.setOnClickListener {
            val options = viewModel.linkableActions.value
            if (options.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No actions available")
                    .setMessage("Create an action in Path first, then link it here.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            val titles = options.map { it.title }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Link an action")
                .setItems(titles) { _, which ->
                    viewModel.linkAction(options[which].actionId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvGoalWhy.text = state.goalWhy ?: ""
                        binding.tvGoalWhy.visibility = if (state.goalWhy.isNullOrBlank()) View.GONE else View.VISIBLE
                        linkedAdapter.submitList(state.linkedActions)
                        receiptAdapter.submitList(state.receipts)
                        
                        // Update toolbar title (removed duplicate title from fragment content)
                        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = state.goalTitle
                    }
                }
                launch {
                    viewModel.events.collect { msg ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show()
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
