package com.mahout.app.ui.path.timeline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class DayTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var date: LocalDate = LocalDate.now()
    private var blocks: List<TimelineBlock> = emptyList()

    // ✅ 1-hour rows, 6 columns per row => 10-min slots
    private val minutesPerRow = 60
    private val columns = 6
    private val minutesPerCol = 10
    private val rowsPerDay = (24 * 60) / minutesPerRow // 24

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity

    // Theme-derived colors
    private val outlineColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
    private val onSurfaceVariantColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
    private val onSurfaceColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

    // Layout metrics
    private val labelWidth = dp(44f)
    private val gridTopLabelHeight = dp(18f)
    private val topPadding = dp(10f)
    private val bottomPadding = dp(12f)

    private val rowHeight = dp(46f)
    private val colGap = dp(10f)

    private val pillHeight = dp(16f)
    private val pillRadius = dp(10f)

    private val blockHeight = dp(18f)
    private val blockRadius = dp(12f)
    private val blockInsetH = dp(6f)
    private val blockUnderlineH = dp(2.2f)

    // Paints
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = outlineColor
        alpha = 30
    }

    private val emptyPillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        color = outlineColor
        alpha = 60
    }

    private val blockFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val blockShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val blockStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        color = onSurfaceVariantColor
        alpha = 160
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
        alpha = 230
    }

    private val nowDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 190
    }

    private val hourFormatter = DateTimeFormatter.ofPattern("ha", Locale.getDefault())

    // Cached geometry
    private var gridLeft = 0f
    private var gridRight = 0f
    private var gridWidth = 0f
    private var slotWidth = 0f

    private val tmpRect = RectF()

    fun submit(date: LocalDate, blocks: List<TimelineBlock>) {
        this.date = date
        this.blocks = blocks
        invalidate()
    }

    fun scrollYForTime(time: LocalTime): Int {
        val minutes = time.hour * 60 + time.minute
        val rowIndex = (minutes / minutesPerRow).coerceIn(0, rowsPerDay - 1)
        val within = (minutes % minutesPerRow).toFloat() / minutesPerRow.toFloat()
        val y = topPadding + gridTopLabelHeight + rowIndex * rowHeight + (within * rowHeight)
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

        // Mark occupied slots so we do NOT draw empty pills behind blocks.
        val occupied = BooleanArray(rowsPerDay * columns)
        markOccupiedSlots(occupied)

        // Draw background pill grid and hour labels
        for (row in 0 until rowsPerDay) {
            val rowTop = gridTop + row * rowHeight
            drawHourLabel(canvas, row, rowTop)
            drawRowGrid(canvas, row, rowTop, occupied)
        }

        // Draw blocks on top
        drawBlocks(canvas, gridTop)

        // Draw "now" dot only if viewing today
        if (date == LocalDate.now()) {
            drawNowDot(canvas, gridTop)
        }
    }

    private fun drawTopMinuteLabels(canvas: Canvas) {
        val y = topPadding + dp(12f)
        canvas.drawText(":00", gridLeft, y, tinyTopLabelPaint)

        val xMid = gridLeft + (gridWidth / 2f)
        canvas.drawText(":30", xMid - dp(10f), y, tinyTopLabelPaint)
    }

    private fun drawVerticalGuides(canvas: Canvas, gridTop: Float) {
        val y1 = gridTop
        val y2 = (topPadding + gridTopLabelHeight + rowsPerDay * rowHeight)

        canvas.drawLine(gridLeft, y1, gridLeft, y2, gridLinePaint)
        canvas.drawLine(gridLeft + gridWidth / 2f, y1, gridLeft + gridWidth / 2f, y2, gridLinePaint)
        canvas.drawLine(gridRight, y1, gridRight, y2, gridLinePaint)
    }

    private fun drawHourLabel(canvas: Canvas, row: Int, rowTop: Float) {
        val hour = row % 24
        val labelTime = LocalTime.of(hour, 0)
        val label = hourFormatter.format(labelTime).lowercase(Locale.getDefault())

        val centerY = rowTop + rowHeight / 2f
        val fm = labelPaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f

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
            canvas.drawRoundRect(tmpRect, pillRadius, pillRadius, emptyPillStroke)
        }
    }

    // ✅ Helpers (fix midnight end + prevent min/max flipping)
    private fun minutesOf(t: LocalTime): Int = t.hour * 60 + t.minute

    private fun normalizedStartEndMinutes(b: TimelineBlock): Pair<Int, Int> {
        val s = minutesOf(b.start).coerceIn(0, 24 * 60)
        var e = minutesOf(b.end).coerceIn(0, 24 * 60)

        // If end is midnight but start is later in day, treat end as 24:00.
        if (e == 0 && s > 0 && b.end == LocalTime.MIDNIGHT) {
            e = 24 * 60
        }
        return s to e
    }

    private fun markOccupiedSlots(occupied: BooleanArray) {
        for (b in blocks) {
            val (s, e) = normalizedStartEndMinutes(b)
            if (e <= s) continue

            val startSlot = s / minutesPerCol
            val endSlotExclusive = ceil(e / minutesPerCol.toDouble()).toInt()

            for (slot in startSlot until endSlotExclusive) {
                val row = (slot * minutesPerCol) / minutesPerRow
                if (row !in 0 until rowsPerDay) continue

                val withinRowMin = (slot * minutesPerCol) % minutesPerRow
                val col = (withinRowMin / minutesPerCol).coerceIn(0, columns - 1)

                val idx = row * columns + col
                if (idx in occupied.indices) occupied[idx] = true
            }
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

                val startCol = startColF.toInt().coerceIn(0, columns - 1)
                val endColExclusive = ceil(endColF.toDouble()).toInt().coerceIn(startCol + 1, columns)

                val left = gridLeft + startCol * (slotWidth + colGap) + blockInsetH
                val right = gridLeft + endColExclusive * (slotWidth + colGap) - colGap - blockInsetH

                val rowTop = gridTop + row * rowHeight
                val top = rowTop + (rowHeight - blockHeight) / 2f
                val bottom = top + blockHeight

                // shadow
                blockShadow.color = b.color
                blockShadow.alpha = 65
                tmpRect.set(left, top + dp(2.5f), right, bottom + dp(2.5f))
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockShadow)

                // fill
                blockFill.color = 0xFFF7F7F7.toInt()
                blockFill.alpha = 255
                tmpRect.set(left, top, right, bottom)
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockFill)

                // outline
                blockStroke.color = b.color
                blockStroke.alpha = 170
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockStroke)

                // underline
                underlinePaint.color = b.color
                underlinePaint.alpha = 230
                val ulH = dp(3.0f)
                val ulTop = bottom - ulH
                tmpRect.set(left, ulTop, right, bottom)
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, underlinePaint)

                if (row == startRow && showCheckAndTitle) {
                    val checkX = left + dp(10f)
                    val checkY = top + dp(13f)

                    checkPaint.color = b.color
                    canvas.drawText("✓", checkX, checkY, checkPaint)

                    val titleX = left + dp(10f)
                    val titleY = bottom + dp(14f)

                    titlePaint.color = b.color
                    canvas.drawText(
                        ellipsize(b.title, right - left - dp(20f), titlePaint),
                        titleX,
                        titleY,
                        titlePaint
                    )
                }
            }
        }
    }

    private fun drawNowDot(canvas: Canvas, gridTop: Float) {
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        val rowIndex = (minutes / minutesPerRow).coerceIn(0, rowsPerDay - 1)
        val within = (minutes % minutesPerRow).toFloat() / minutesPerRow.toFloat()

        val y = gridTop + rowIndex * rowHeight + within * rowHeight
        val x = paddingLeft + dp(30f)

        nowDotPaint.color = 0xFFB08A2A.toInt()
        canvas.drawCircle(x, y, dp(2.6f), nowDotPaint)
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: TextPaint): String {
        if (text.isBlank()) return ""
        if (paint.measureText(text) <= maxWidth) return text

        val ell = "…"
        var end = text.length
        while (end > 1) {
            val candidate = text.substring(0, end) + ell
            if (paint.measureText(candidate) <= maxWidth) return candidate
            end--
        }
        return ell
    }
}
