package com.mahout.app.domain.relations

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.path.model.Action

/**
 * Convenience model (not a DB table):
 * Goal + its linked actions.
 */
data class GoalWithActions(
    val goal: Goal,
    val actions: List<Action>
)
