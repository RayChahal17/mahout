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

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onRequestDate(date: LocalDate)          // user browses to another day
        fun onBackToToday()                         // user taps "Today"/"Back to today"
        fun onLogTime(date: LocalDate)              // plus button
        fun onStats(date: LocalDate)                // bars button (future)
    }

    private val binding = ViewTimelineBinding.inflate(LayoutInflater.from(context), this, true)
    private var listener: Listener? = null

    private var displayedDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    private var followToday: Boolean = true

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE. MMM d", Locale.getDefault())

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).toInt()

    // Prevent repeated day flips when the user stays at the top/bottom edge.
    private var edgePagingLockedUntilMs: Long = 0L

    init {
        binding.btnLogTime.setOnClickListener { listener?.onLogTime(displayedDate) }
        binding.btnTimelineStats.setOnClickListener { listener?.onStats(displayedDate) }
        binding.btnBackToToday.setOnClickListener { listener?.onBackToToday() }

        binding.timelineScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, y, _, oldY ->
                handleEdgePaging(y, oldY)
            }
        )
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun submit(date: LocalDate, blocks: List<TimelineBlock>, followToday: Boolean) {
        this.displayedDate = date
        this.followToday = followToday

        binding.tvTimelineDate.text = dateFormatter.format(date)

        val today = LocalDate.now(ZoneId.systemDefault())
        binding.btnBackToToday.isVisible = (!followToday) || (date != today)
        binding.btnBackToToday.text = if (date != today) "Today" else "Back to today"

        binding.dayTimelineView.submit(date, blocks)

        // If we are following today and displaying today, keep it snapped to "now" on first render.
        if (followToday && date == today) {
            post { scrollToNow(animated = false) }
        }
    }

    /**
     * Call from Fragment.onResume().
     *
     * IMPORTANT:
     * If followToday=true and midnight rollover happened while app was open,
     * we must NOT call onRequestDate(today) (that would disable followToday in ViewModel).
     */
    fun onHostResumed() {
        if (!followToday) return
        val today = LocalDate.now(ZoneId.systemDefault())
        if (displayedDate != today) {
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

    private fun handleEdgePaging(scrollY: Int, oldY: Int) {
        // Edge paging: top => previous day, bottom => next day
        val now = SystemClock.elapsedRealtime()
        if (now < edgePagingLockedUntilMs) return

        val goingUp = scrollY < oldY
        val goingDown = scrollY > oldY
        val threshold = dp(10f)

        val child = binding.timelineScroll.getChildAt(0) ?: return
        val maxScroll = (child.height - binding.timelineScroll.height).coerceAtLeast(0)

        if (goingUp && scrollY <= threshold) {
            edgePagingLockedUntilMs = now + 700L
            listener?.onRequestDate(displayedDate.minusDays(1))
        } else if (goingDown && scrollY >= (maxScroll - threshold)) {
            edgePagingLockedUntilMs = now + 700L
            listener?.onRequestDate(displayedDate.plusDays(1))
        }
    }
}
