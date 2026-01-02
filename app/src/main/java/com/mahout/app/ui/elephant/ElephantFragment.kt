package com.mahout.app.ui.elephant

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mahout.app.R
import com.mahout.app.databinding.FragmentElephantBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import java.io.File

@AndroidEntryPoint
class ElephantFragment : Fragment() {

    private var _binding: FragmentElephantBinding? = null
    private val binding: FragmentElephantBinding get() = _binding!!

    private val viewModel: ElephantViewModel by viewModels()

    private lateinit var moodAdapter: MoodAdapter
    private lateinit var moodLayoutManager: LinearLayoutManager
    private val moods: List<MoodUi> = MoodCatalog.moods

    private val snapHelper = PagerSnapHelper()

    private var hasAppliedInitialScroll = false
    private var userDraggedCarousel = false

    // Dot scrub state
    private var isDotScrubbing = false
    private var dotTargetIndex: Int = 0
    private var dotChaseRunning = false

    // When dots release, we smooth-scroll to center and log once on idle.
    private var pendingLogAfterSmoothScroll = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElephantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEleTitle.text = getString(R.string.tab_elephant)
        binding.tvElePrompt.text = getString(R.string.elephant_prompt_default)

        binding.btnEleClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvMoodLabel.setOnClickListener {
            ElephantHistoryBottomSheet().show(childFragmentManager, "ElephantHistory")
        }

        setupCarousel()
        setupDots()
        bindViewModel()
    }

    private fun setupCarousel() {
        moodAdapter = MoodAdapter { mood ->
            // Tap: smooth scroll to it (we do NOT auto-log on tap)
            val index = moods.indexOfFirst { it.id == mood.id }
            if (index >= 0) smoothScrollToCenteredPosition(index)
        }

        moodLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMoods.layoutManager = moodLayoutManager
        binding.rvMoods.adapter = moodAdapter
        binding.rvMoods.setHasFixedSize(true)

        // ✅ Helps remove "jump" caused by item animations during rapid updates.
        binding.rvMoods.itemAnimator = null

        snapHelper.attachToRecyclerView(binding.rvMoods)

        binding.rvMoods.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    userDraggedCarousel = true
                }

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Always compute centered mood (padding-aware center).
                    val centeredPos = findCenteredAdapterPosition()
                    if (centeredPos != RecyclerView.NO_POSITION) {
                        onMoodCentered(centeredPos)
                    }

                    // While dots are actively scrubbing, NEVER log on idle.
                    if (isDotScrubbing) {
                        applyScaleAndFade(animated = true)
                        return
                    }

                    // Log after:
                    // - real list drag OR
                    // - dot release requested log after smooth scroll
                    if (pendingLogAfterSmoothScroll || userDraggedCarousel) {
                        pendingLogAfterSmoothScroll = false
                        userDraggedCarousel = false

                        val mood = moods.getOrNull(centeredPos)
                        if (mood != null && mood.id != MoodCatalog.SKIP_MOOD_ID) {
                            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }

                        viewModel.logSelectedMood()
                    }

                    applyScaleAndFade(animated = true)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                applyScaleAndFade(animated = false)
            }
        })

        moodAdapter.submitList(moods)
        applyScaleAndFade(animated = false)
    }

    private fun setupDots() {
        binding.dotScrubber.count = moods.size

        binding.dotScrubber.setListener(object : DotScrubberView.Listener {

            override fun onScrubIndexChanged(index: Int) {
                // ✅ User is actively scrubbing
                isDotScrubbing = true
                dotTargetIndex = index

                // Update label/check/dots immediately
                onMoodCentered(index)

                // Start smooth "chase" so the list GLIDES while thumb moves
                startDotChaseIfNeeded()
            }

            override fun onScrubReleased(index: Int) {
                // Done scrubbing
                isDotScrubbing = false
                dotTargetIndex = index

                // One clean smooth scroll to perfectly center, then log on idle
                pendingLogAfterSmoothScroll = true
                userDraggedCarousel = false

                smoothScrollToCenteredPosition(index)
            }

            override fun onVerticalDrag(dyFingerPx: Float) {
                // Keep your thumb on the bar and still scroll list vertically.
                val multiplier = 1.35f
                binding.rvMoods.scrollBy(0, (-dyFingerPx * multiplier).toInt())
                userDraggedCarousel = true
            }
        })
    }

    private fun startDotChaseIfNeeded() {
        if (dotChaseRunning) return
        dotChaseRunning = true

        binding.dotScrubber.postOnAnimation(object : Runnable {
            override fun run() {
                // Stop loop if view is gone
                val b = _binding ?: run {
                    dotChaseRunning = false
                    return
                }

                if (!isDotScrubbing) {
                    dotChaseRunning = false
                    return
                }

                // Move the list a little bit toward the target each frame.
                chaseListTowardIndex(dotTargetIndex)

                // Keep animating at ~60fps while user is scrubbing.
                b.dotScrubber.postOnAnimation(this)
            }
        })
    }

    /**
     * Smoothly nudges the RecyclerView so the target item moves toward the content-center.
     * This avoids "jumping" but still makes dots feel connected to the smiles list.
     */
    private fun chaseListTowardIndex(targetIndex: Int) {
        val rv = binding.rvMoods

        val targetView = moodLayoutManager.findViewByPosition(targetIndex)

        val stepPx = dp(22f).toInt() // ✅ TWEAK: bigger = faster glide

        if (targetView != null) {
            val centerY = contentCenterY(rv)
            val viewCenter = (targetView.top + targetView.bottom) / 2f
            val delta = viewCenter - centerY

            // If already close enough, don’t jitter.
            if (abs(delta) < 2f) return

            val dy = delta.coerceIn(-stepPx.toFloat(), stepPx.toFloat()).toInt()
            rv.scrollBy(0, dy)
            return
        }

        // If the target view isn't laid out yet, move in the correct direction.
        val currentCenterPos = findCenteredAdapterPosition()
        if (currentCenterPos == RecyclerView.NO_POSITION) return

        val dir = (targetIndex - currentCenterPos).toFloat().sign.toInt().coerceIn(-1, 1)
        if (dir != 0) rv.scrollBy(0, dir * stepPx)
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.state.collect { state -> render(state) }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            ElephantViewModel.ElephantEvent.NavigateToPath -> {
                                findNavController().navigate(R.id.pathFragment)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ElephantViewModel.ElephantUiState) {
        val selectedId = state.selectedMoodId
        val selectedIndex = selectedId
            ?.let { id -> moods.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0

        if (!hasAppliedInitialScroll) {
            hasAppliedInitialScroll = true
            binding.rvMoods.scrollToPosition(selectedIndex)
            onMoodCentered(selectedIndex)
            applyScaleAndFade(animated = false)
        }

        val mood = moods.getOrNull(selectedIndex) ?: moods.first()
        binding.tvMoodLabel.text = mood.label
        binding.dotScrubber.activeIndex = selectedIndex
        moodAdapter.setSelectedMoodId(mood.id)

        binding.tvEleTrends.text = getString(
            R.string.elephant_trends_summary,
            state.last7Total,
            state.lifetimeTotal
        )

        binding.tvEleError.isVisible = !state.error.isNullOrBlank()
        binding.tvEleError.text = state.error.orEmpty()
    }

    private fun onMoodCentered(index: Int) {
        val mood = moods.getOrNull(index) ?: return
        viewModel.selectMood(mood.id)

        // immediate UI feedback
        binding.tvMoodLabel.text = mood.label
        binding.dotScrubber.activeIndex = index
        moodAdapter.setSelectedMoodId(mood.id)
    }

    /**
     * Finds the child closest to the CONTENT center (padding-aware),
     * so selection matches what the user actually sees.
     */
    private fun findCenteredAdapterPosition(): Int {
        val rv = binding.rvMoods
        if (rv.childCount == 0) return RecyclerView.NO_POSITION

        val centerY = contentCenterY(rv)

        var bestPos = RecyclerView.NO_POSITION
        var bestDist = Float.MAX_VALUE

        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val childCenter = (child.top + child.bottom) / 2f
            val dist = abs(centerY - childCenter)
            if (dist < bestDist) {
                bestDist = dist
                bestPos = moodLayoutManager.getPosition(child)
            }
        }
        return bestPos
    }

    private fun smoothScrollToCenteredPosition(targetPos: Int) {
        val rv = binding.rvMoods
        rv.stopScroll()

        val scroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START

            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                val centerY = contentCenterY(rv).toInt()
                val viewCenter = (view.top + view.bottom) / 2
                return viewCenter - centerY
            }

            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                return 45f / displayMetrics.densityDpi
            }
        }

        scroller.targetPosition = targetPos
        moodLayoutManager.startSmoothScroll(scroller)
    }

    private fun applyScaleAndFade(animated: Boolean) {
        val rv = binding.rvMoods
        val centerY = contentCenterY(rv)
        if (centerY <= 0f) return

        val maxLiftPx = dp(36f)
        val maxScale = 1.18f
        val minAlpha = 0.03f
        val minScale = 0.78f

        val durationMs = if (animated) 160L else 0L
        val interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()

        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            child.animate().cancel()

            val childCenter = (child.top + child.bottom) / 2f
            val dist = abs(centerY - childCenter)
            val t = (dist / (rv.height - rv.paddingTop - rv.paddingBottom).coerceAtLeast(1)).coerceIn(0f, 1f)

            val eased = 1f - t
            val eased2 = eased * eased

            val targetScale = minScale + (maxScale - minScale) * eased2
            val targetAlpha = minAlpha + (1f - minAlpha) * eased2
            val targetLift = maxLiftPx * eased2

            if (animated) {
                child.animate()
                    .alpha(targetAlpha)
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationY(-targetLift)
                    .setDuration(durationMs)
                    .setInterpolator(interpolator)
                    .start()
            } else {
                child.alpha = targetAlpha
                child.scaleX = targetScale
                child.scaleY = targetScale
                child.translationY = -targetLift
            }
        }
    }

    /**
     * RecyclerView "content center" (same idea SnapHelper uses):
     * center of the area between paddingTop and paddingBottom.
     */
    private fun contentCenterY(rv: RecyclerView): Float {
        val start = rv.paddingTop.toFloat()
        val end = (rv.height - rv.paddingBottom).toFloat()
        return start + (end - start) / 2f
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
