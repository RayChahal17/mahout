package com.mahout.app.ui.elephant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mahout.app.databinding.FragmentElephantBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ElephantFragment : Fragment() {

    private var _binding: FragmentElephantBinding? = null
    private val binding: FragmentElephantBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentElephantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = "Elephant (placeholder)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
