package com.mahout.app.ui.elephant

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * PagerSnapHelper with a tuned smooth-scroller speed.
 * Smaller speedPerPixel => faster snap.
 */
class ElephantSnapHelper(
    private val context: Context
) : PagerSnapHelper() {

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller? {
        if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) return null

        return object : LinearSmoothScroller(context) {

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                // ✅ TWEAK THIS:
                // Smaller => faster snap, bigger => slower snap.
                // Good premium range: 35f–60f / densityDpi
                return 42f / displayMetrics.densityDpi
            }

            override fun calculateTimeForDeceleration(dx: Int): Int {
                // Slightly longer decel feels "expensive"
                return (super.calculateTimeForDeceleration(dx) * 1.15f).toInt()
            }
        }
    }
}
