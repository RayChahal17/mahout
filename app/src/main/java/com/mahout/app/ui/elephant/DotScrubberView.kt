package com.mahout.app.ui.elephant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

class DotScrubberView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onScrubIndexChanged(index: Int)
        fun onScrubReleased(index: Int)

        /**
         * NEW: if user drags vertically on the bar, we let the parent scroll the mood list.
         * dyFingerPx: + = finger moved down, - = finger moved up
         */
        fun onVerticalDrag(dyFingerPx: Float)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillRect = RectF()

    private var listener: Listener? = null

    var count: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            activeIndex = activeIndex
            invalidate()
        }

    var activeIndex: Int = 0
        set(value) {
            field = value.coerceIn(0, (count - 1).coerceAtLeast(0))
            invalidate()
        }

    fun setListener(l: Listener?) {
        listener = l
    }

    // Gesture tracking
    private enum class Mode { UNDECIDED, HORIZONTAL, VERTICAL }

    private var mode: Mode = Mode.UNDECIDED
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var lastEmittedIndex: Int = -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (count <= 0) return

        // Pill background
        val pillRadius = dp(18f)
        pillRect.set(
            0f + dp(6f),
            (height / 2f) - dp(18f),
            width.toFloat() - dp(6f),
            (height / 2f) + dp(18f)
        )
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = 28
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, paint)

        // Track
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = 45

        val left = pillRect.left + dp(16f)
        val right = pillRect.right - dp(16f)
        val cy = height / 2f
        canvas.drawLine(left, cy, right, cy, paint)

        // Dots
        val rInactive = dp(4.4f)
        val rActive = dp(6.2f)

        val usableWidth = right - left
        val step = if (count == 1) 0f else (usableWidth / (count - 1))

        for (i in 0 until count) {
            val cx = left + i * step
            val isActive = (i == activeIndex)

            paint.style = Paint.Style.FILL
            paint.color = 0xFFFFFFFF.toInt()
            paint.alpha = if (isActive) 255 else 125

            canvas.drawCircle(cx, cy, if (isActive) rActive else rInactive, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (count <= 0) return false

        val threshold = dp(10f) // decide horizontal vs vertical after small movement

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                mode = Mode.UNDECIDED
                downX = event.x
                downY = event.y
                lastY = event.y

                // Treat down as "potential scrub"
                val idx = indexForX(event.x)
                lastEmittedIndex = idx
                activeIndex = idx
                listener?.onScrubIndexChanged(idx)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY

                if (mode == Mode.UNDECIDED) {
                    if (abs(dx) > threshold || abs(dy) > threshold) {
                        mode = if (abs(dx) >= abs(dy)) Mode.HORIZONTAL else Mode.VERTICAL
                    }
                }

                when (mode) {
                    Mode.HORIZONTAL -> {
                        val idx = indexForX(event.x)
                        if (idx != lastEmittedIndex) {
                            lastEmittedIndex = idx
                            activeIndex = idx
                            listener?.onScrubIndexChanged(idx)
                        }
                    }

                    Mode.VERTICAL -> {
                        // allow list scrolling with finger
                        val dyFromLast = event.y - lastY
                        lastY = event.y
                        listener?.onVerticalDrag(dyFromLast)
                    }

                    Mode.UNDECIDED -> {
                        // do nothing yet
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)

                if (mode == Mode.VERTICAL) {
                    // Vertical drag = scroll only. Do NOT log on release.
                    mode = Mode.UNDECIDED
                    lastEmittedIndex = -1
                    return true
                }

                // Horizontal (or tap): commit/log
                val idx = indexForX(event.x)
                activeIndex = idx
                performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                listener?.onScrubReleased(idx)

                mode = Mode.UNDECIDED
                lastEmittedIndex = -1
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                mode = Mode.UNDECIDED
                lastEmittedIndex = -1
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun indexForX(x: Float): Int {
        val left = pillRect.left + dp(16f)
        val right = pillRect.right - dp(16f)

        val clamped = x.coerceIn(left, right)
        val t = if (right == left) 0f else ((clamped - left) / (right - left))
        val raw = (t * (count - 1)).roundToInt()
        return raw.coerceIn(0, count - 1)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
