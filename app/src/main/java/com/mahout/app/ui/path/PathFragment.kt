package com.mahout.app.ui.path

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.mahout.app.R
import com.mahout.app.databinding.FragmentPathBinding
import com.mahout.app.ui.common.showSnackbar

class PathFragment : Fragment() {

    private var _binding: FragmentPathBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // For now: pretend we have no actions yet.
        showLoading(false)
        showEmpty(
            title = getString(R.string.path_empty_title),
            message = getString(R.string.path_empty_message),
            showAction = true,
            actionText = getString(R.string.common_coming_soon)
        ) {
            binding.root.showSnackbar(getString(R.string.common_coming_soon))
        }
    }

    private fun showLoading(isLoading: Boolean, message: String? = null) {
        binding.loadingOverlay.root.isVisible = isLoading
        if (message != null) {
            binding.loadingOverlay.loadingMessage.text = message
        }
    }

    private fun showEmpty(
        title: String,
        message: String,
        showAction: Boolean,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        binding.emptyState.root.isVisible = true
        binding.emptyState.emptyStateTitle.text = title
        binding.emptyState.emptyStateMessage.text = message

        binding.emptyState.emptyStateAction.isVisible = showAction
        if (showAction && !actionText.isNullOrBlank() && onAction != null) {
            binding.emptyState.emptyStateAction.text = actionText
            binding.emptyState.emptyStateAction.setOnClickListener { onAction() }
        } else {
            binding.emptyState.emptyStateAction.setOnClickListener(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
