package com.mahout.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.mahout.app.BuildConfig
import com.mahout.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings (V1 fast-ship).
 *
 * Scope lock decisions reflected here:
 * - Backup/Restore is DEFERRED for Beta -> show a blunt warning.
 * - Debug tools are visible ONLY in DEBUG builds.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Debug-only button
        binding.btnDbSanity.isVisible = BuildConfig.DEBUG
        binding.tvDebugHint.isVisible = BuildConfig.DEBUG


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
