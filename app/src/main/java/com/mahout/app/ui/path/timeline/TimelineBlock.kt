package com.mahout.app.ui.path.timeline

import androidx.annotation.ColorInt
import java.time.LocalTime

data class TimelineBlock(
    val id: String,
    val actionId: String,
    val title: String,
    val start: LocalTime,
    val end: LocalTime,
    @ColorInt val color: Int
)
