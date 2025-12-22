package com.mahout.app.ui.common

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.showSnackbar(
    message: String,
    actionText: String? = null,
    duration: Int = Snackbar.LENGTH_LONG,
    action: (() -> Unit)? = null
) {
    val snack = Snackbar.make(this, message, duration)
    if (!actionText.isNullOrBlank() && action != null) {
        snack.setAction(actionText) { action() }
    }
    snack.show()
}
