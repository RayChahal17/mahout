package com.mahout.app.ui.path.timeline

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.R as AppCompatR
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class DayTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var date: LocalDate = LocalDate.now()
    private var blocks: List<TimelineBlock> = emptyList()

    // 1-hour rows, 6 columns per row => 10-min slots
    private val minutesPerRow = 60
    private val columns = 6
    private val minutesPerCol = 10
    private val rowsPerDay = (24 * 60) / minutesPerRow // 24

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity

    // ===== Theme-derived colors (NO hardcoded greys) =====
    private val outlineColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
    private val onSurfaceVariantColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
    private val onSurfaceColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

    private val surfaceColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
    private val surfaceVariantColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant)

    // ✅ Use AppCompat attr constant so it exists in your project.
    private val primaryColor =
        MaterialColors.getColor(this, AppCompatR.attr.colorPrimary)

    private val isDarkTheme: Boolean = ColorUtils.calculateLuminance(surfaceColor) < 0.35

    // ===== Layout metrics =====
    private val labelWidth = dp(44f)
    private val gridTopLabelHeight = dp(18f)
    private val topPadding = dp(10f)
    private val bottomPadding = dp(12f)

    private val rowHeight = dp(52f)
    private val colGap = dp(10f)

    private val pillHeight = dp(16f)
    private val pillRadius = dp(10f)

    private val blockHeight = dp(22f)
    private val blockRadius = dp(12f)
    private val blockInsetH = dp(5f)

    // ===== Paints =====
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = outlineColor
        alpha = if (isDarkTheme) 20 else 26
    }

    private val emptyPillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = outlineColor
        alpha = if (isDarkTheme) 38 else 42
    }

    private val emptyPillFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = surfaceVariantColor
        alpha = if (isDarkTheme) 70 else 120
    }

    private val blockFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // “Lift” without blur (fast)
    private val blockLiftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF000000.toInt()
        alpha = if (isDarkTheme) 30 else 22
    }

    private val blockStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.25f)
    }

    private val blockHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = 0xFFFFFFFF.toInt()
        alpha = if (isDarkTheme) 35 else 55
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        color = onSurfaceVariantColor
        alpha = 150
    }

    private val tinyTopLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10f)
        color = onSurfaceVariantColor
        alpha = 120
    }

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
        color = onSurfaceColor
        alpha = 220
    }

    private val checkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        color = onSurfaceColor
        alpha = 220
    }

    private val nowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        color = primaryColor
        alpha = if (isDarkTheme) 120 else 95
    }

    private val nowDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primaryColor
        alpha = 220
    }

    private val nowDotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = surfaceColor
        alpha = 255
    }

    private val hourFormatter = DateTimeFormatter.ofPattern("ha", Locale.getDefault())

    // Cached geometry
    private var gridLeft = 0f
    private var gridRight = 0f
    private var gridWidth = 0f
    private var slotWidth = 0f

    private val tmpRect = RectF()

    /**
     * ✅ “Now” indicator correctness fix:
     * - The view needs to redraw over time.
     * - We schedule invalidation every minute on the minute boundary.
     * - This also fixes cases where you change device time or cross midnight.
     */
    private val nowTicker = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return

            // Redraw so LocalTime.now() updates visually.
            invalidate()

            // Align to the next minute boundary for stability (no drift).
            val nowMs = System.currentTimeMillis()
            val delayToNextMinute = 60_000L - (nowMs % 60_000L)
            postDelayed(this, delayToNextMinute)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(nowTicker)
        post(nowTicker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(nowTicker)
        super.onDetachedFromWindow()
    }

    fun submit(date: LocalDate, blocks: List<TimelineBlock>) {
        this.date = date
        this.blocks = blocks
        invalidate()
    }
    /**
     * REQUIRED by TimelineView.kt.
     *
     * TimelineView uses this to:
     * - auto-scroll to "now"
     * - detect if user is near the current time
     *
     * It converts a LocalTime (like 14:35) into the Y scroll position inside this view.
     */
    fun scrollYForTime(time: java.time.LocalTime): Int {
        val minutes = time.hour * 60 + time.minute

        // Clamp into valid day range so we never return invalid coordinates.
        val rowIndex = (minutes / minutesPerRow).coerceIn(0, rowsPerDay - 1)
        val within = (minutes % minutesPerRow).toFloat() / minutesPerRow.toFloat()

        // This must match the exact grid top used in onDraw().
        val gridTop = topPadding + gridTopLabelHeight

        val y = gridTop + rowIndex * rowHeight + within * rowHeight
        return y.toInt()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desiredH = (topPadding + gridTopLabelHeight + rowsPerDay * rowHeight + bottomPadding).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gridLeft = paddingLeft + labelWidth
        gridRight = (w - paddingRight).toFloat()
        gridWidth = (gridRight - gridLeft).coerceAtLeast(1f)

        val totalGap = colGap * (columns - 1)
        slotWidth = ((gridWidth - totalGap) / columns.toFloat()).coerceAtLeast(dp(18f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val gridTop = topPadding + gridTopLabelHeight

        drawTopMinuteLabels(canvas)
        drawVerticalGuides(canvas, gridTop)

        val occupied = BooleanArray(rowsPerDay * columns)
        markOccupiedSlots(occupied)

        for (row in 0 until rowsPerDay) {
            val rowTop = gridTop + row * rowHeight
            drawHourLabel(canvas, row, rowTop)
            drawRowGrid(canvas, row, rowTop, occupied)
        }

        drawBlocks(canvas, gridTop)

        // Only draw now indicator for “today”
        if (date == LocalDate.now()) {
            drawNowIndicator(canvas, gridTop)
        }
    }

    private fun drawTopMinuteLabels(canvas: Canvas) {
        val y = topPadding + dp(12f)
        for (col in 0 until columns) {
            val minute = col * minutesPerCol
            val label = minute.toString().padStart(2, '0')
            val x = gridLeft + col * (slotWidth + colGap) + dp(2f)
            canvas.drawText(label, x, y, tinyTopLabelPaint)
        }
    }

    private fun drawVerticalGuides(canvas: Canvas, gridTop: Float) {
        for (col in 1 until columns) {
            val x = gridLeft + col * slotWidth + (col - 1) * colGap + colGap / 2f
            canvas.drawLine(x, gridTop, x, gridTop + rowsPerDay * rowHeight, gridLinePaint)
        }
    }

    private fun drawHourLabel(canvas: Canvas, row: Int, rowTop: Float) {
        val label = hourFormatter.format(LocalTime.of(row, 0)).lowercase(Locale.getDefault())
        val fm = labelPaint.fontMetrics
        val baseline = rowTop + rowHeight / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, paddingLeft.toFloat(), baseline, labelPaint)
    }

    private fun drawRowGrid(canvas: Canvas, row: Int, rowTop: Float, occupied: BooleanArray) {
        val pillTop = rowTop + (rowHeight - pillHeight) / 2f
        val pillBottom = pillTop + pillHeight

        for (col in 0 until columns) {
            val idx = row * columns + col
            if (idx in occupied.indices && occupied[idx]) continue

            val left = gridLeft + col * (slotWidth + colGap)
            val right = left + slotWidth

            tmpRect.set(left, pillTop, right, pillBottom)
            canvas.drawRoundRect(tmpRect, pillRadius, pillRadius, emptyPillFill)
            canvas.drawRoundRect(tmpRect, pillRadius, pillRadius, emptyPillStroke)
        }
    }

    private fun drawBlocks(canvas: Canvas, gridTop: Float) {
        val sorted = blocks.sortedBy { it.start }

        for (b in sorted) {
            val (startMin, endMin) = normalizedStartEndMinutes(b)
            if (endMin <= startMin) continue

            val durationMin = endMin - startMin
            val showCheckAndTitle = durationMin >= 10

            val startRow = (startMin / minutesPerRow).coerceIn(0, rowsPerDay - 1)
            val endRow = ((max(endMin - 1, startMin)) / minutesPerRow).coerceIn(0, rowsPerDay - 1)

            for (row in startRow..endRow) {
                val rowStartMin = row * minutesPerRow
                val rowEndMin = rowStartMin + minutesPerRow

                val segStart = max(startMin, rowStartMin)
                val segEnd = min(endMin, rowEndMin)
                if (segEnd <= segStart) continue

                val startColF = (segStart - rowStartMin).toFloat() / minutesPerCol.toFloat()
                val endColF = (segEnd - rowStartMin).toFloat() / minutesPerCol.toFloat()

                val left = columnFractionToX(startColF) + blockInsetH
                val right = columnFractionToX(endColF) - blockInsetH

                val rowTop = gridTop + row * rowHeight
                val top = rowTop + (rowHeight - blockHeight) / 2f
                val bottom = top + blockHeight

                val rect = RectF(left, top, right, bottom)

                tmpRect.set(rect.left, rect.top + dp(1.2f), rect.right, rect.bottom + dp(1.2f))
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockLiftPaint)

                val tintedMid = blendColor(surfaceColor, b.color, if (isDarkTheme) 0.24f else 0.18f)
                val tintedTop = blendColor(surfaceColor, b.color, if (isDarkTheme) 0.34f else 0.26f)
                val tintedBottom = blendColor(b.color, 0xFF000000.toInt(), if (isDarkTheme) 0.22f else 0.12f)

                blockFill.shader = LinearGradient(
                    rect.left, rect.top,
                    rect.left, rect.bottom,
                    intArrayOf(tintedTop, tintedMid, tintedBottom),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, blockRadius, blockRadius, blockFill)
                blockFill.shader = null

                blockStroke.color = b.color
                blockStroke.alpha = if (isDarkTheme) 160 else 150
                canvas.drawRoundRect(rect, blockRadius, blockRadius, blockStroke)

                val inset = dp(2f)
                tmpRect.set(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
                canvas.drawLine(tmpRect.left, tmpRect.top + dp(1f), tmpRect.right, tmpRect.top + dp(1f), blockHighlight)

                if (row == startRow && showCheckAndTitle) {
                    val baselineCenter = rect.centerY()

                    val checkX = rect.left + dp(8f)
                    val fmCheck = checkPaint.fontMetrics
                    val checkY = baselineCenter - (fmCheck.ascent + fmCheck.descent) / 2f

                    checkPaint.color = b.color
                    checkPaint.alpha = 230
                    canvas.drawText("✓", checkX, checkY, checkPaint)

                    val titleX = checkX + dp(12f)
                    val maxW = (rect.right - titleX - dp(8f)).coerceAtLeast(0f)
                    val title = ellipsize(b.title, maxW, titlePaint)
                    val fmTitle = titlePaint.fontMetrics
                    val titleY = baselineCenter - (fmTitle.ascent + fmTitle.descent) / 2f

                    titlePaint.alpha = 230
                    canvas.drawText(title, titleX, titleY, titlePaint)
                }
            }
        }
    }

    private fun drawNowIndicator(canvas: Canvas, gridTop: Float) {
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        val rowIndex = (minutes / minutesPerRow).coerceIn(0, rowsPerDay - 1)
        val within = (minutes % minutesPerRow).toFloat() / minutesPerRow.toFloat()

        val y = gridTop + rowIndex * rowHeight + within * rowHeight

        canvas.drawLine(gridLeft, y, gridRight, y, nowLinePaint)

        val x = paddingLeft + dp(30f)
        canvas.drawCircle(x, y, dp(4.0f), nowDotPaint)
        canvas.drawCircle(x, y, dp(1.9f), nowDotInnerPaint)
    }

    private fun markOccupiedSlots(occupied: BooleanArray) {
        for (b in blocks) {
            val (startMin, endMin) = normalizedStartEndMinutes(b)
            if (endMin <= startMin) continue

            val startRow = (startMin / minutesPerRow).coerceIn(0, rowsPerDay - 1)
            val endRow = ((max(endMin - 1, startMin)) / minutesPerRow).coerceIn(0, rowsPerDay - 1)

            for (row in startRow..endRow) {
                val rowStartMin = row * minutesPerRow
                val rowEndMin = rowStartMin + minutesPerRow

                val segStart = max(startMin, rowStartMin)
                val segEnd = min(endMin, rowEndMin)
                if (segEnd <= segStart) continue

                val startCol = floor((segStart - rowStartMin).toFloat() / minutesPerCol.toFloat()).toInt()
                    .coerceIn(0, columns - 1)
                val endCol = ceil((segEnd - rowStartMin).toFloat() / minutesPerCol.toFloat()).toInt()
                    .coerceIn(0, columns)

                for (col in startCol until endCol) {
                    val idx = row * columns + col
                    if (idx in occupied.indices) occupied[idx] = true
                }
            }
        }
    }

    private fun normalizedStartEndMinutes(b: TimelineBlock): Pair<Int, Int> {
        fun minutesOf(t: LocalTime): Int = t.hour * 60 + t.minute
        val s = minutesOf(b.start).coerceIn(0, 24 * 60)
        var e = minutesOf(b.end).coerceIn(0, 24 * 60)
        if (e == 0 && s > 0) e = 24 * 60
        return s to e
    }

    private fun columnFractionToX(colF: Float): Float {
        val c = colF.coerceIn(0f, columns.toFloat())
        val whole = floor(c).toInt()
        val gapCount = min(whole, columns - 1)
        return gridLeft + c * slotWidth + gapCount * colGap
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: TextPaint): String {
        if (text.isBlank()) return ""
        if (paint.measureText(text) <= maxWidth) return text

        val ellipsis = "…"
        val ellW = paint.measureText(ellipsis)
        if (ellW >= maxWidth) return ellipsis

        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + ellW > maxWidth) end--
        return if (end <= 0) ellipsis else text.substring(0, end) + ellipsis
    }

    private fun blendColor(color1: Int, color2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)

        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val rr = (r1 + (r2 - r1) * r).toInt().coerceIn(0, 255)
        val gg = (g1 + (g2 - g1) * r).toInt().coerceIn(0, 255)
        val bb = (b1 + (b2 - b1) * r).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
}
