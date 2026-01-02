package com.mahout.app.ui.path.timeline

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.mahout.app.databinding.ViewTimelineBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onRequestDate(date: LocalDate)
        fun onBackToToday()
        fun onLogTime(date: LocalDate)
        fun onStats(date: LocalDate)
    }

    private val binding: ViewTimelineBinding =
        ViewTimelineBinding.inflate(LayoutInflater.from(context), this, true)

    private var listener: Listener? = null

    private val zone = ZoneId.systemDefault()
    private var displayedDate: LocalDate = LocalDate.now(zone)
    private var followToday: Boolean = true

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE. MMM d", Locale.getDefault())

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).toInt()

    // Prevent repeated day flips when staying at top/bottom edge
    private var edgePagingLockedUntilMs: Long = 0L

    // ✅ prevents "snap back to now" while user scrolls
    private var pendingSnapToNow: Boolean = true
    private var lastSubmitDate: LocalDate? = null
    private var lastSubmitFollowToday: Boolean? = null
    private var lastUserScrollMs: Long = 0L
    private val userScrollGraceMs = 900L

    init {
        binding.btnLogTime.setOnClickListener { listener?.onLogTime(displayedDate) }
        binding.btnTimelineStats.setOnClickListener { listener?.onStats(displayedDate) }

        binding.btnBackToToday.setOnClickListener {
            // ✅ When user taps Now/Today, we allow ONE snap (and only then)
            pendingSnapToNow = true
            listener?.onBackToToday()
            post { scrollToNow(animated = true) }
        }

        binding.timelineScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, y, _, oldY ->
                if (abs(y - oldY) > 2) {
                    lastUserScrollMs = SystemClock.elapsedRealtime()
                }
                handleEdgePaging(y, oldY)
                updateNowButtonVisibility()
            }
        )
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun submit(date: LocalDate, blocks: List<TimelineBlock>, followToday: Boolean) {
        val today = LocalDate.now(zone)

        // detect transitions that should allow snapping
        val dateChanged = lastSubmitDate != date
        val followChanged = lastSubmitFollowToday != followToday

        if (dateChanged || (followChanged && followToday)) {
            pendingSnapToNow = true
        }

        lastSubmitDate = date
        lastSubmitFollowToday = followToday

        this.displayedDate = date
        this.followToday = followToday

        binding.tvTimelineDate.text = dateFormatter.format(date)
        binding.dayTimelineView.submit(date, blocks)

        updateNowButtonVisibility()

        // ✅ Only snap when:
        // - followToday=true
        // - viewing today
        // - pendingSnapToNow=true (one-shot)
        // - user did NOT scroll recently
        if (followToday && date == today && pendingSnapToNow) {
            val recentlyScrolled = (SystemClock.elapsedRealtime() - lastUserScrollMs) < userScrollGraceMs
            if (!recentlyScrolled) {
                post {
                    scrollToNow(animated = false)
                    pendingSnapToNow = false
                    updateNowButtonVisibility()
                }
            }
        }
    }

    /**
     * Call from Fragment.onResume().
     * If followToday=true and midnight rollover happened, snap to today.
     */
    fun onHostResumed() {
        if (!followToday) return
        val today = LocalDate.now(zone)
        if (displayedDate != today) {
            pendingSnapToNow = true
            listener?.onBackToToday()
        }
    }

    fun scrollToNow(animated: Boolean) {
        scrollToTime(LocalTime.now(), animated)
    }

    fun scrollToTime(time: LocalTime, animated: Boolean) {
        val rawY = binding.dayTimelineView.scrollYForTime(time)
        val targetY = (rawY - binding.timelineScroll.height / 3).coerceAtLeast(0)
        if (animated) binding.timelineScroll.smoothScrollTo(0, targetY)
        else binding.timelineScroll.scrollTo(0, targetY)
    }

    private fun updateNowButtonVisibility() {
        val today = LocalDate.now(zone)

        val isToday = displayedDate == today
        val nearNow = if (!isToday || binding.timelineScroll.height == 0) {
            false
        } else {
            val nowRaw = binding.dayTimelineView.scrollYForTime(LocalTime.now())
            val nowTarget = (nowRaw - binding.timelineScroll.height / 3).coerceAtLeast(0)
            abs(binding.timelineScroll.scrollY - nowTarget) <= dp(24f)
        }

        // ✅ Show button when:
        // - not following today OR
        // - browsing another date OR
        // - user is away from now (past OR future)
        val show = (!followToday) || (!isToday) || (!nearNow)

        binding.btnBackToToday.isVisible = show
        binding.btnBackToToday.text = "Now"
    }

    private fun handleEdgePaging(scrollY: Int, oldY: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now < edgePagingLockedUntilMs) return

        val today = LocalDate.now(zone)
        val isToday = displayedDate == today
        val bottomPagingAllowed = !(followToday && isToday)

        val goingUp = scrollY < oldY
        val goingDown = scrollY > oldY
        val fastEnough = kotlin.math.abs(scrollY - oldY) > dp(12f)

        val child = binding.timelineScroll.getChildAt(0) ?: return
        val maxScroll = (child.height - binding.timelineScroll.height).coerceAtLeast(0)

        // Only flip days when user reaches the true edges with a deliberate fling
        if (fastEnough && goingUp && scrollY <= 0) {
            edgePagingLockedUntilMs = now + 700L
            listener?.onRequestDate(displayedDate.minusDays(1))
        } else if (bottomPagingAllowed && fastEnough && goingDown && scrollY >= maxScroll) {
            edgePagingLockedUntilMs = now + 700L
            listener?.onRequestDate(displayedDate.plusDays(1))
        }
    }
}
