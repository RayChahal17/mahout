package com.mahout.app.core.id

/**
 * Generates IDs for new rows (actionId, goalId, sessionId, linkId, etc).
 *
 * Keeping this injectable makes testing simple.
 */
interface IdProvider {
    fun newId(): String
}
