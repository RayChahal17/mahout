package com.mahout.app.ui.path

/**
 * One-shot UI events for Path screen.
 * (We use snackbar events to keep the ViewModel UI-framework-agnostic.)
 */
sealed interface PathEvent {
    data class ShowSnackbar(val message: String) : PathEvent
}
