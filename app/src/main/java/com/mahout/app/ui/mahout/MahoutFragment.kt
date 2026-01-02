package com.mahout.app.ui.mahout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mahout.app.R
import com.mahout.app.databinding.DialogMahoutEditProfileBinding
import com.mahout.app.databinding.FragmentMahoutBinding
import com.mahout.app.domain.mahout.model.JournalEntryType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MahoutFragment : Fragment() {

    private var _binding: FragmentMahoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MahoutViewModel by viewModels()

    private lateinit var promptAdapter: MahoutPromptAdapter

    /**
     * We use this nav “result key” to clear any old draft text on the hub
     * after a successful save in the editor.
     *
     * This is a premium touch: it prevents “why is my old text still here?” confusion.
     */
    private companion object {
        private const val RESULT_CLEAR_HUB_DRAFT = "result_clear_hub_draft"

        private const val ARG_ENTRY_TYPE = "entryType"
        private const val ARG_ENTRY_ID = "entryId"
        private const val ARG_PREFILL_BODY = "prefillBody"
        private const val ARG_PROMPT_ID = "promptId"
    }

    /**
     * This makes Mahout match your screenshot (no app toolbar on this tab).
     * It is scoped ONLY to this fragment, so other screens remain unchanged.
     */
    override fun onResume() {
        super.onResume()
        setTopAppBarVisible(false)
    }

    override fun onPause() {
        setTopAppBarVisible(true)
        super.onPause()
    }

    private fun setTopAppBarVisible(visible: Boolean) {
        // Safe: if the activity layout changes, this just no-ops.
        activity?.findViewById<MaterialToolbar>(R.id.topAppBar)?.isVisible = visible
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMahoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- RecyclerView (prompt suggestions) ---
        // Premium behavior NOW:
        // - tapping a prompt opens the full editor with that prompt prefilled
        promptAdapter = MahoutPromptAdapter(
            onPromptClicked = { prompt -> openEditorFromPrompt(prompt) }
        )

        binding.rvPrompts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = promptAdapter
            // This list is inside a NestedScrollView → disable nested scrolling to avoid jank.
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        // --- Segmented control (New Entry / Feeling / Plan / Gratitude) ---
        binding.tgMahoutMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnMahoutNewEntry -> viewModel.selectMode(MahoutMode.NEW_ENTRY)
                R.id.btnMahoutFeeling -> viewModel.selectMode(MahoutMode.JOURNAL_FEELING)
                R.id.btnMahoutPlan -> viewModel.selectMode(MahoutMode.PLAN_NEXT_STEP)
                R.id.btnMahoutGratitude -> viewModel.selectMode(MahoutMode.GRATITUDE)
            }
        }

        // --- Settings gear (top right) ---
        binding.btnMahoutSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // --- Edit profile (name/email) ---
        binding.tvMahoutEdit.setOnClickListener {
            showEditProfileDialog()
        }

        // --- View past entries (NOW implemented) ---
        binding.btnViewPastEntries.setOnClickListener {
            findNavController().navigate(R.id.action_mahoutFragment_to_journalListFragment)
        }

        // ✅ The notepad-looking field is now a TAP-TO-OPEN preview (non-editable).
        // We disable editing in a way that preserves the visuals (lines + hint + padding).
        binding.etJournal.apply {
            // Disable any keyboard editing while keeping the view enabled + clickable.
            keyListener = null
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false

            setOnClickListener {
                openEditorFromNotepad()
            }
        }

        // Listen for a “saved” result from editor, then clear hub draft.
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>(RESULT_CLEAR_HUB_DRAFT)
            ?.observe(viewLifecycleOwner) { shouldClear ->
                if (shouldClear == true) {
                    viewModel.onJournalTextChanged("") // clears state + SavedStateHandle
                }
            }

        // --- Collect state from VM ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { render(it) }
                }
            }
        }
    }

    private fun render(state: MahoutUiState) {
        // Header
        binding.tvMahoutTitle.text = getString(R.string.tab_mahout)
        binding.ivAvatar.setImageResource(R.drawable.logo)
        binding.tvMahoutName.text = state.profileName
        binding.tvMahoutEmail.text = state.profileEmail
        binding.tvMahoutTagline.text = state.tagline

        // Date chip
        binding.tvTodayLabel.text = state.todayLabel
        binding.tvDateChip.text = state.dateChip

        // Segmented selection (avoid loops)
        val expectedCheckedId = when (state.mode) {
            MahoutMode.NEW_ENTRY -> R.id.btnMahoutNewEntry
            MahoutMode.JOURNAL_FEELING -> R.id.btnMahoutFeeling
            MahoutMode.PLAN_NEXT_STEP -> R.id.btnMahoutPlan
            MahoutMode.GRATITUDE -> R.id.btnMahoutGratitude
        }
        if (binding.tgMahoutMode.checkedButtonId != expectedCheckedId) {
            binding.tgMahoutMode.check(expectedCheckedId)
        }

        // Notepad preview:
        // - If you had an old draft saved in SavedStateHandle, show it here.
        // - Otherwise let the hint show (“Write what’s on your mind…”).
        if (binding.etJournal.text?.toString() != state.journalText) {
            binding.etJournal.setText(state.journalText)
        }

        // Prompts
        promptAdapter.submitList(state.prompts)
        binding.rvPrompts.isVisible = state.prompts.isNotEmpty()
    }


    private fun openEditorFromNotepad() {
        val state = viewModel.state.value
        val entryType = state.mode.toJournalEntryType()

        // If there’s an old hub draft (from previous version), carry it into the editor.
        val prefill = state.journalText.takeIf { it.isNotBlank() }

        findNavController().navigate(
            R.id.action_mahoutFragment_to_journalEditorFragment,
            bundleOf(
                ARG_ENTRY_TYPE to entryType.name,
                ARG_PREFILL_BODY to prefill
            )
        )
    }

    private fun openEditorFromPrompt(prompt: MahoutPromptUi) {
        val state = viewModel.state.value
        val entryType = state.mode.toJournalEntryType()

        // Premium: combine existing hub draft + prompt if the user had something saved.
        val draft = state.journalText.trim()
        val prefill = if (draft.isBlank()) {
            prompt.text
        } else {
            "$draft\n\n${prompt.text}"
        }

        findNavController().navigate(
            R.id.action_mahoutFragment_to_journalEditorFragment,
            bundleOf(
                ARG_ENTRY_TYPE to entryType.name,
                ARG_PREFILL_BODY to prefill,
                ARG_PROMPT_ID to prompt.id
            )
        )
    }

    private fun MahoutMode.toJournalEntryType(): JournalEntryType =
        when (this) {
            MahoutMode.NEW_ENTRY -> JournalEntryType.NEW_ENTRY
            MahoutMode.JOURNAL_FEELING -> JournalEntryType.JOURNAL_FEELING
            MahoutMode.PLAN_NEXT_STEP -> JournalEntryType.PLAN_NEXT_STEP
            MahoutMode.GRATITUDE -> JournalEntryType.GRATITUDE
        }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogMahoutEditProfileBinding.inflate(layoutInflater)

        val current = viewModel.state.value
        dialogBinding.etName.setText(current.profileName)
        dialogBinding.etEmail.setText(current.profileEmail)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit profile")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etName.text?.toString().orEmpty().trim()
                val email = dialogBinding.etEmail.text?.toString().orEmpty().trim()
                viewModel.updateProfile(
                    name = name.ifBlank { "Mahout User" },
                    email = email.ifBlank { "you@example.com" }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
