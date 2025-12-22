package com.mahout.app.core.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar

/**
 * Minimal snackbar helper.
 * We avoid heavy styling for now so it stays stable across themes.
 */
fun View.showSnackbar(
    message: CharSequence,
    actionText: CharSequence? = null,
    onAction: (() -> Unit)? = null,
    duration: Int = Snackbar.LENGTH_LONG
) {
    val snack = Snackbar.make(this, message, duration)
    if (actionText != null && onAction != null) {
        snack.setAction(actionText) { onAction() }
    }
    snack.setTextMaxLines(4)
    snack.show()
}
