package com.mahout.app.ui.northstar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mahout.app.databinding.FragmentNorthStarBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NorthStarFragment : Fragment() {

    private var _binding: FragmentNorthStarBinding? = null
    private val binding: FragmentNorthStarBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNorthStarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = "North Star (placeholder)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
