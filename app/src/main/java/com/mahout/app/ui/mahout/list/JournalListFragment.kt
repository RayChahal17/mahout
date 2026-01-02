package com.mahout.app.ui.mahout.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mahout.app.R
import com.mahout.app.databinding.FragmentJournalListBinding
import com.mahout.app.ui.common.showConfirmDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalListFragment : Fragment() {

    private var _binding: FragmentJournalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalListViewModel by viewModels()

    private lateinit var adapter: JournalEntryAdapter

    private companion object {
        private const val ARG_ENTRY_TYPE = "entryType"
        private const val ARG_ENTRY_ID = "entryId"
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<MaterialToolbar>(R.id.topAppBar)?.isVisible = false
    }

    override fun onPause() {
        activity?.findViewById<MaterialToolbar>(R.id.topAppBar)?.isVisible = true
        super.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = JournalEntryAdapter { row ->
            // Open editor in edit mode
            findNavController().navigate(
                R.id.action_journalListFragment_to_journalEditorFragment,
                bundleOf(
                    ARG_ENTRY_ID to row.id,
                    ARG_ENTRY_TYPE to row.type.name
                )
            )
        }

        binding.rvEntries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEntries.adapter = adapter
        binding.rvEntries.itemAnimator = null

        attachSwipeToDelete(binding.rvEntries)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        adapter.submitList(state.rows)
                        binding.tvEmpty.isVisible = state.isEmpty
                    }
                }
            }
        }
    }

    /**
     * Swipe-to-delete with confirmation:
     * - If user cancels, we restore the row (notifyItemChanged).
     */
    private fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(pos)

                if (item == null) {
                    adapter.notifyItemChanged(pos)
                    return
                }

                showConfirmDialog(
                    title = getString(R.string.mahout_delete_title),
                    message = getString(R.string.mahout_delete_message),
                    confirmText = getString(R.string.mahout_delete_confirm),
                    cancelText = getString(R.string.mahout_delete_cancel),
                    onConfirm = { viewModel.deleteEntry(item.id) }
                )

                // Always restore the swipe visual; real deletion is handled via Room flow update.
                adapter.notifyItemChanged(pos)
            }
        })
        helper.attachToRecyclerView(recyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
