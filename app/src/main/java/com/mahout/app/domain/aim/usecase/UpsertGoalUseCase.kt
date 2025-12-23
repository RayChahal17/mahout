// app/src/main/java/com/mahout/app/domain/aim/usecase/UpsertGoalUseCase.kt
package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.repository.GoalRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Day 11: Upsert (create or edit) a Goal.
 *
 * Backwards-compatibility rule:
 * - Older callers only passed: goalId/title/why/horizon/targetDate
 * - Newer model supports: parentGoalId (optional)
 *
 * So we provide parentGoalId with a DEFAULT value.
 * This fixes compilation without forcing you to update every call site.
 */
class UpsertGoalUseCase @Inject constructor(
    private val repo: GoalRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        goalId: String?,
        title: String,
        why: String?,
        horizon: GoalHorizon,
        targetDate: LocalDate?,

        // ✅ NEW (optional) — default keeps old code working
        parentGoalId: String? = null
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Goal title is required." }

        val cleanWhy = why?.trim()?.takeIf { it.isNotBlank() }

        val now = timeProvider.nowInstant()

        // If editing, preserve fields we are not changing today.
        val existing: Goal? = goalId?.let { repo.getGoal(it) }

        val finalId = existing?.id ?: goalId ?: idProvider.newId()

        // Parent rules (safe guard rails)
        require(parentGoalId != finalId) { "A goal cannot be its own parent." }

        val resolvedParentGoalId =
            // If caller provided a parent id, use it.
            parentGoalId
            // Otherwise preserve existing parent relationship.
                ?: existing?.parentGoalId

        val goal = Goal(
            id = finalId,
            title = cleanTitle,
            why = cleanWhy,
            horizon = horizon,

            // Preserve status/priority if editing; default if new
            status = existing?.status ?: GoalStatus.ACTIVE,
            priority = existing?.priority ?: 0,

            targetDate = targetDate,
            parentGoalId = resolvedParentGoalId,

            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deletedAt = existing?.deletedAt
        )

        repo.upsert(goal)
    }
}
