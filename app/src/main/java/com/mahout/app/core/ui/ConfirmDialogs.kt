package com.mahout.app.core.ui

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Confirm dialog helper used everywhere:
 * - Delete confirmations
 * - Clear data
 * - Dangerous actions
 */
fun Context.showConfirmDialog(
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence = "Confirm",
    cancelText: CharSequence = "Cancel",
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(confirmText) { _, _ -> onConfirm() }
        .setNegativeButton(cancelText) { _, _ -> onCancel?.invoke() }
        .show()
}

fun Context.showConfirmDialog(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes confirmRes: Int,
    @StringRes cancelRes: Int,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    showConfirmDialog(
        title = getString(titleRes),
        message = getString(messageRes),
        confirmText = getString(confirmRes),
        cancelText = getString(cancelRes),
        onConfirm = onConfirm,
        onCancel = onCancel
    )
}
