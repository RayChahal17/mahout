package com.mahout.app.ui.aim

import java.time.LocalDate

/**
 * UI model for Chief Aim (simplified for Fragment display).
 */
data class ChiefAimUiModel(
    val title: String,
    val description: String?,
    val targetDate: LocalDate?
)

