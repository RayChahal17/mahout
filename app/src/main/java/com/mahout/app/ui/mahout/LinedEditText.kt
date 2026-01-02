package com.mahout.app.ui.mahout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.mahout.app.R
import kotlin.math.max

/**
 * A simple "notepad" edit text:
 * - Draws horizontal lines behind the text
 * - Gives the premium journaling vibe from your screenshots
 *
 * Safe: it doesn't affect input behavior, selection, IME, etc.
 */
class LinedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1f
        color = context.getColor(R.color.mahout_paper_line)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw lines FIRST, then text on top.
        val availableHeight = height - paddingTop - paddingBottom
        val linesToDraw = max(lineCount, availableHeight / lineHeight + 1)

        val startX = paddingLeft.toFloat()
        val endX = (width - paddingRight).toFloat()

        for (i in 0 until linesToDraw) {
            val baseline = try {
                // Use existing line if available; otherwise extend from the last known line height.
                if (lineCount > 0) {
                    val safeIdx = (i).coerceAtMost(lineCount - 1)
                    val base = getLineBounds(safeIdx, null)
                    val extra = (i - safeIdx) * lineHeight
                    base + extra
                } else {
                    paddingTop + i * lineHeight
                }
            } catch (_: Exception) {
                paddingTop + i * lineHeight
            }
            val y = (baseline + resources.displayMetrics.density * 6f)
            canvas.drawLine(startX, y, endX, y, linePaint)
        }

        super.onDraw(canvas)
    }
}
