package com.mahout.app.ui.path.timeline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
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

    // 30-min rows, 6 columns per row => 5-min slots
    private val minutesPerRow = 30
    private val columns = 6
    private val minutesPerCol = 5
    private val rowsPerDay = (24 * 60) / minutesPerRow // 48

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity

    // Layout metrics
    private val labelWidth = dp(46f)
    private val gridTopLabelHeight = dp(18f)
    private val topPadding = dp(10f)
    private val bottomPadding = dp(12f)
    private val rowHeight = dp(56f)

    private val colGap = dp(10f)
    private val pillHeight = dp(16f)
    private val pillRadius = dp(10f)

    private val blockHeight = dp(18f)
    private val blockRadius = dp(12f)
    private val blockInsetH = dp(6f)
    private val blockUnderlineH = dp(2.2f)

    // Paints (no setShadowLayer => avoids software layer warnings)
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        alpha = 30
    }

    private val emptyPillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        alpha = 60
    }

    private val emptyPillFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 0
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

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        alpha = 160
    }

    private val tinyTopLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10f)
        alpha = 120
    }

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
        alpha = 220
    }

    private val checkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
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
        requestLayout()
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
            drawHourLabel(canvas, row, rowTop, gridTop)
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
        // These are decorative labels to match the premium screenshot look.
        val y = topPadding + dp(12f)
        tinyTopLabelPaint.color = labelPaint.color

        // Left edge label ":00"
        canvas.drawText(":00", gridLeft, y, tinyTopLabelPaint)

        // Mid label ":30" (center of grid)
        val xMid = gridLeft + (gridWidth / 2f)
        canvas.drawText(":30", xMid - dp(10f), y, tinyTopLabelPaint)
    }

    private fun drawVerticalGuides(canvas: Canvas, gridTop: Float) {
        // Subtle vertical lines: left edge, mid, right edge
        val y1 = gridTop
        val y2 = (topPadding + gridTopLabelHeight + rowsPerDay * rowHeight)

        gridLinePaint.color = labelPaint.color

        canvas.drawLine(gridLeft, y1, gridLeft, y2, gridLinePaint)
        canvas.drawLine(gridLeft + gridWidth / 2f, y1, gridLeft + gridWidth / 2f, y2, gridLinePaint)
        canvas.drawLine(gridRight, y1, gridRight, y2, gridLinePaint)
    }

    private fun drawHourLabel(canvas: Canvas, row: Int, rowTop: Float, gridTop: Float) {
        // Draw hour label once per hour, centered across the two 30-min rows.
        if (row % 2 != 0) return

        val hour = (row / 2) % 24
        val labelTime = LocalTime.of(hour, 0)
        val label = hourFormatter.format(labelTime).lowercase(Locale.getDefault())

        val yCenter = rowTop + rowHeight // center across the 2 rows
        val x = paddingLeft.toFloat()
        labelPaint.color = titlePaint.color
        canvas.drawText(label, x, yCenter, labelPaint)
    }

    private fun drawRowGrid(canvas: Canvas, row: Int, rowTop: Float, occupied: BooleanArray) {
        val pillTop = rowTop + (rowHeight - pillHeight) / 2f
        val pillBottom = pillTop + pillHeight

        for (col in 0 until columns) {
            val idx = row * columns + col
            if (idx in occupied.indices && occupied[idx]) {
                // IMPORTANT: do NOT draw unused pill behind a block.
                continue
            }

            val left = gridLeft + col * (slotWidth + colGap)
            val right = left + slotWidth

            tmpRect.set(left, pillTop, right, pillBottom)
            emptyPillStroke.color = labelPaint.color
            emptyPillFill.color = labelPaint.color

            canvas.drawRoundRect(tmpRect, pillRadius, pillRadius, emptyPillStroke)
        }
    }

    private fun markOccupiedSlots(occupied: BooleanArray) {
        for (b in blocks) {
            val startMin = (b.start.hour * 60 + b.start.minute).coerceIn(0, 24 * 60)
            val endMin = (b.end.hour * 60 + b.end.minute).coerceIn(0, 24 * 60)
            val s = min(startMin, endMin)
            val e = max(startMin, endMin)

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
        // Sort by start time for consistent draw order
        val sorted = blocks.sortedBy { it.start }

        for (b in sorted) {
            val startMinRaw = (b.start.hour * 60 + b.start.minute).coerceIn(0, 24 * 60)
            val endMinRaw = (b.end.hour * 60 + b.end.minute).coerceIn(0, 24 * 60)
            val startMin = min(startMinRaw, endMinRaw)
            val endMin = max(startMinRaw, endMinRaw)

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

                // Colored shadow (simulated, hardware-friendly)
                blockShadow.color = b.color
                blockShadow.alpha = 40
                tmpRect.set(left, top + dp(3f), right, bottom + dp(3f))
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockShadow)

                // Main block fill (white-ish)
                blockFill.color = 0xFFFFFFFF.toInt()
                blockFill.alpha = 230
                tmpRect.set(left, top, right, bottom)
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, blockFill)

                // Colored underline
                underlinePaint.color = b.color
                underlinePaint.alpha = 220
                val ulTop = bottom - blockUnderlineH
                tmpRect.set(left, ulTop, right, bottom)
                canvas.drawRoundRect(tmpRect, blockRadius, blockRadius, underlinePaint)

                // Check + title only on the first row of the block
                if (row == startRow && showCheckAndTitle) {
                    val checkX = left + dp(10f)
                    val checkY = top + dp(13f)
                    checkPaint.color = b.color
                    canvas.drawText("✓", checkX, checkY, checkPaint)

                    val titleX = left + dp(10f)
                    val titleY = bottom + dp(14f)
                    titlePaint.color = b.color
                    canvas.drawText(ellipsize(b.title, right - left - dp(20f), titlePaint), titleX, titleY, titlePaint)
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

        // golden-ish dot like screenshot
        nowDotPaint.color = 0xFFB08A2A.toInt()
        canvas.drawCircle(x, y, dp(2.6f), nowDotPaint)
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: TextPaint): String {
        if (text.isBlank()) return ""
        val measured = paint.measureText(text)
        if (measured <= maxWidth) return text

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
