package com.mahout.app.domain.relations

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.path.model.Action

/**
 * Convenience model (not a DB table):
 * Action + the goals it supports.
 */
data class ActionWithGoals(
    val action: Action,
    val goals: List<Goal>
)
