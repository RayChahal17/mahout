package com.mahout.app.ui.path.timeline

import android.graphics.Color
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * One source of truth for "action accent colors".
 *
 * Goals:
 * - Same actionId => same color everywhere.
 * - Try very hard to avoid duplicates across actions (within this process lifetime).
 * - Large palette + infinite fallback via HSV if palette is exhausted.
 */
object ActionColors {

    // A bigger "premium" palette (not neon, not muddy) – 36 colors.
    // You can add more later safely.
    private val palette = intArrayOf(
        0xFF4F8DF5.toInt(), 0xFF63C174.toInt(), 0xFFF0A43B.toInt(), 0xFF9B6DFF.toInt(),
        0xFFE06363.toInt(), 0xFF2FB7B3.toInt(), 0xFF3F7CAC.toInt(), 0xFF5DBB63.toInt(),
        0xFFEE6C4D.toInt(), 0xFF8D5CF6.toInt(), 0xFFB56576.toInt(), 0xFF00A6A6.toInt(),
        0xFF6C63FF.toInt(), 0xFF2A9D8F.toInt(), 0xFFE9C46A.toInt(), 0xFFF4A261.toInt(),
        0xFFE76F51.toInt(), 0xFF577590.toInt(), 0xFF43AA8B.toInt(), 0xFF90BE6D.toInt(),
        0xFFF9C74F.toInt(), 0xFFF8961E.toInt(), 0xFFF3722C.toInt(), 0xFFF94144.toInt(),
        0xFF277DA1.toInt(), 0xFF4D96FF.toInt(), 0xFF6BCB77.toInt(), 0xFFFF6B6B.toInt(),
        0xFF845EC2.toInt(), 0xFF00C9A7.toInt(), 0xFFC34A36.toInt(), 0xFF0081CF.toInt(),
        0xFF7C83FD.toInt(), 0xFF4E9F3D.toInt(), 0xFFE07A5F.toInt(), 0xFF3D405B.toInt()
    )

    // assignment cache: actionId -> color
    private val assigned = ConcurrentHashMap<String, Int>()

    // global used colors (within process lifetime)
    private val used = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    /**
     * Optional: call this when you receive the full actions list, so colors get
     * assigned early + with less chance of collision.
     */
    fun prime(actionIds: List<String>) {
        // deterministic order helps stable assignments
        actionIds.distinct().sorted().forEach { forActionId(it) }
    }

    fun forActionId(id: String): Int {
        assigned[id]?.let { return it }

        // try palette first, but avoid duplicates
        val startIdx = positiveHash(id) % palette.size
        for (i in 0 until palette.size) {
            val c = palette[(startIdx + i) % palette.size]
            if (used.add(c)) {
                assigned[id] = c
                return c
            }
        }

        // If palette exhausted: generate an infinite stream of distinct-ish colors via HSV.
        // Use golden-angle stepping to spread hues.
        var hue = ((positiveHash(id) % 360) * 0.6180339887 * 360.0).toFloat() % 360f
        var attempt = 0
        while (attempt < 720) {
            val c = Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.92f))
            if (used.add(c) && !tooCloseToExisting(c)) {
                assigned[id] = c
                return c
            }
            hue = (hue + 137.508f) % 360f
            attempt++
        }

        // worst-case fallback (should basically never happen)
        val fallback = Color.HSVToColor(floatArrayOf(hue, 0.60f, 0.90f))
        assigned[id] = fallback
        return fallback
    }

    private fun positiveHash(s: String): Int =
        ((s.hashCode().toLong() and 0x7FFFFFFF).toInt()).coerceAtLeast(0)

    /**
     * Helps avoid super-similar colors when HSV fallback is used.
     * (Palette colors are already curated.)
     */
    private fun tooCloseToExisting(color: Int): Boolean {
        // simple quick heuristic (not expensive, avoids near duplicates)
        // Compare by RGB distance against a small sample of used colors.
        val r1 = (color shr 16) and 0xFF
        val g1 = (color shr 8) and 0xFF
        val b1 = (color) and 0xFF

        var checked = 0
        for (c in used) {
            val r2 = (c shr 16) and 0xFF
            val g2 = (c shr 8) and 0xFF
            val b2 = (c) and 0xFF
            val dist = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
            if (dist < 90) return true
            checked++
            if (checked >= 40) break
        }
        return false
    }
}
