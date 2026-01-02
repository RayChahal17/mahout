package com.mahout.app.ui.path.timeline

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Premium action color palette.
 *
 * Why this palette?
 * - Muted jewel tones read as “expensive” on both light & dark surfaces.
 * - Avoids neon/bright “toy” colors.
 * - Still provides enough distinction between actions.
 */
object ActionColors {

    private val assigned = ConcurrentHashMap<String, Int>()
    private val used = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    /**
     * Premium, muted, high-contrast-but-not-neon palette.
     * All colors are opaque ARGB.
     */
    private val palette = intArrayOf(
        0xFF2D3A4A.toInt(), // Deep slate
        0xFF234E70.toInt(), // Navy
        0xFF2A6F55.toInt(), // Forest
        0xFF3B5B92.toInt(), // Sapphire
        0xFF6B3A5B.toInt(), // Plum
        0xFF7A3040.toInt(), // Burgundy
        0xFF2D6A6A.toInt(), // Teal
        0xFF5A4E3C.toInt(), // Warm taupe
        0xFF8A5A2B.toInt(), // Amber brown
        0xFF3E6D5C.toInt(), // Moss
        0xFF4C4F6E.toInt(), // Slate indigo
        0xFF6A4C93.toInt(), // Muted violet
        0xFF8C2F39.toInt(), // Rich red (muted)
        0xFF2B5B88.toInt(), // Deep blue
        0xFF3C6E71.toInt(), // Cool teal
        0xFFB08A2A.toInt()  // Warm gold accent (sparingly)
    )

    fun prime(actionIds: List<String>) {
        actionIds.distinct().sorted().forEach { forActionId(it) }
    }

    fun forActionId(id: String): Int {
        assigned[id]?.let { return it }

        // Pick the least-colliding palette color.
        val seed = stableHash(id)
        val startIndex = abs(seed) % palette.size

        // Try palette first
        for (i in 0 until palette.size) {
            val c = palette[(startIndex + i) % palette.size]
            if (used.add(c)) {
                assigned[id] = c
                return c
            }
        }

        // Fallback: deterministic hue shift if palette is exhausted
        val fallback = hsvFallback(seed)
        assigned[id] = fallback
        return fallback
    }

    private fun stableHash(s: String): Int {
        // Deterministic hash, stable per process.
        var h = 7
        for (ch in s) h = h * 31 + ch.code
        return h
    }

    private fun hsvFallback(seed: Int): Int {
        // Generate a muted color from HSV space: not too bright.
        val hue = (abs(seed) % 360).toFloat()
        val sat = 0.48f
        val value = 0.72f
        return hsvToColor(hue, sat, value)
    }

    private fun hsvToColor(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
