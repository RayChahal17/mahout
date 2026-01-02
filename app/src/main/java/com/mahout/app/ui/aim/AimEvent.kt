package com.mahout.app.ui.aim

/**
 * One-time events from AimViewModel (e.g., show snackbar).
 */
sealed class AimEvent {
    data class ShowSnackbar(val message: String) : AimEvent()
}

