package com.mahout.app.ui.mahout.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.mahout.app.R
import com.mahout.app.databinding.FragmentJournalEditorBinding
import com.mahout.app.ui.common.showConfirmDialog
import com.mahout.app.ui.mahout.MahoutPromptAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalEditorFragment : Fragment() {

    private var _binding: FragmentJournalEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalEditorViewModel by viewModels()

    private lateinit var promptAdapter: MahoutPromptAdapter

    private companion object {
        private const val RESULT_CLEAR_HUB_DRAFT = "result_clear_hub_draft"
    }

    override fun onResume() {
        super.onResume()
        // Full-screen premium editor (like your Mahout hub): hide the Activity toolbar.
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
        _binding = FragmentJournalEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button (custom, premium)
        binding.btnBack.setOnClickListener { attemptClose() }

        // Save / Publish (private save)
        binding.btnSave.setOnClickListener { viewModel.onSaveClicked() }

        // Prompts list (same concept you already implemented)
        promptAdapter = MahoutPromptAdapter { prompt ->
            insertPromptIntoEditor(prompt.text)
        }
        binding.rvPrompts.adapter = promptAdapter
        binding.rvPrompts.itemAnimator = null
        binding.rvPrompts.isNestedScrollingEnabled = false

        // Body → ViewModel
        binding.etBody.doAfterTextChanged { editable ->
            viewModel.onBodyChanged(editable?.toString().orEmpty())
        }

        // Handle system back the same way as our UI back
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    attemptClose()
                }
            }
        )

        // Collect state/events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { render(it) }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is JournalEditorViewModel.Event.ShowSnackbar ->
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()

                            JournalEditorViewModel.Event.SavedAndClose -> {
                                Snackbar.make(binding.root, getString(R.string.mahout_saved), Snackbar.LENGTH_SHORT).show()

                                // Tell Mahout hub to clear any old draft text.
                                findNavController().previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(RESULT_CLEAR_HUB_DRAFT, true)

                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun render(state: JournalEditorViewModel.UiState) {
        // Title + type pill (premium)
        binding.tvTitle.text = state.type.toTitle()
        binding.tvTypePill.text = state.type.toPill()

        // Date chip
        binding.tvDateChip.text = state.dateChip

        // Prompts
        promptAdapter.submitList(state.prompts)
        binding.rvPrompts.isVisible = state.prompts.isNotEmpty()

        // Keep EditText stable: only set text if user isn’t actively typing.
        if (!binding.etBody.isFocused) {
            val current = binding.etBody.text?.toString().orEmpty()
            if (current != state.body) {
                binding.etBody.setText(state.body)
                binding.etBody.setSelection(state.body.length)
            }
        }

        // Save button enabled/disabled
        binding.btnSave.isEnabled = state.canSave
    }

    private fun attemptClose() {
        val state = viewModel.state.value
        val current = binding.etBody.text?.toString().orEmpty()

        // Premium discard protection: only ask if there are unsaved changes.
        val hasUnsaved = current.trimEnd() != state.initialBody.trimEnd()
        if (!hasUnsaved) {
            findNavController().popBackStack()
            return
        }

        showConfirmDialog(
            title = getString(R.string.mahout_discard_title),
            message = getString(R.string.mahout_discard_message),
            confirmText = getString(R.string.mahout_discard_confirm),
            cancelText = getString(R.string.mahout_discard_cancel),
            onConfirm = { findNavController().popBackStack() }
        )
    }

    /**
     * Inserts the prompt at cursor position (premium feel),
     * then lets doAfterTextChanged push it into the ViewModel state.
     */
    private fun insertPromptIntoEditor(promptText: String) {
        val edit = binding.etBody
        val text = edit.text ?: return

        val start = edit.selectionStart.coerceAtLeast(0)
        val end = edit.selectionEnd.coerceAtLeast(0)
        val min = minOf(start, end)
        val max = maxOf(start, end)

        val needsSpacing =
            min > 0 && text.getOrNull(min - 1) != '\n'

        val insert = (if (needsSpacing) "\n\n" else "") + promptText

        text.replace(min, max, insert)

        // Move cursor to end of inserted block (feels natural)
        val newCursor = min + insert.length
        edit.setSelection(newCursor.coerceAtMost(text.length))
    }

    private fun com.mahout.app.domain.mahout.model.JournalEntryType.toTitle(): String =
        when (this) {
            com.mahout.app.domain.mahout.model.JournalEntryType.NEW_ENTRY -> getString(R.string.mahout_type_new_entry)
            com.mahout.app.domain.mahout.model.JournalEntryType.JOURNAL_FEELING -> getString(R.string.mahout_type_feeling)
            com.mahout.app.domain.mahout.model.JournalEntryType.PLAN_NEXT_STEP -> getString(R.string.mahout_type_plan)
            com.mahout.app.domain.mahout.model.JournalEntryType.GRATITUDE -> getString(R.string.mahout_type_gratitude)
            else -> getString(R.string.mahout_type_new_entry)
        }

    private fun com.mahout.app.domain.mahout.model.JournalEntryType.toPill(): String =
        when (this) {
            com.mahout.app.domain.mahout.model.JournalEntryType.NEW_ENTRY -> "NEW"
            com.mahout.app.domain.mahout.model.JournalEntryType.JOURNAL_FEELING -> "FEELING"
            com.mahout.app.domain.mahout.model.JournalEntryType.PLAN_NEXT_STEP -> "PLAN"
            com.mahout.app.domain.mahout.model.JournalEntryType.GRATITUDE -> "GRATITUDE"
            else -> "JOURNAL"
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
