package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.model.GoalHorizon
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.repository.GoalRepository
import com.mahout.app.domain.aim.util.GoalHorizonResolver
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
class UpsertGoalUseCase @Inject constructor(
    private val repo: GoalRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
    private val observeChiefAimUseCase: ObserveChiefAimUseCase
) {
    suspend operator fun invoke(
        goalId: String?,
        title: String,
        why: String?,
        horizon: GoalHorizon,
        targetDate: LocalDate?
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Goal title is required." }

        val cleanWhy = why?.trim()?.takeIf { it.isNotBlank() }

        val now = timeProvider.nowInstant()
        val today = timeProvider.nowInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        // If targetDate is set, enforce the "Chief Aim year" limit.
        // (Max = Dec 31 of Chief Aim target year.)
        if (targetDate != null) {
            val chiefAim = observeChiefAimUseCase().first()
            val chiefTarget = chiefAim?.targetDate
                ?: throw IllegalArgumentException(
                    "Set a Chief Aim target date first. Goals can’t extend past your Chief Aim year."
                )

            val maxGoalDate = LocalDate.of(chiefTarget.year, 12, 31)

            require(!targetDate.isAfter(maxGoalDate)) {
                "Goal target can’t be after ${maxGoalDate}. Goals must stay within your Chief Aim year (${chiefTarget.year})."
            }
        }

        // Auto-derive horizon when targetDate exists (your UI behavior, enforced in domain too).
        val finalHorizon = targetDate?.let { GoalHorizonResolver.fromTargetDate(today, it) } ?: horizon

        val existing: Goal? = goalId?.let { repo.getGoal(it) }

        val priority = existing?.priority ?: now.epochSecond.toInt()

        val finalGoal = Goal(
            id = existing?.id ?: idProvider.newId(),
            title = cleanTitle,
            why = cleanWhy,
            horizon = finalHorizon,
            status = existing?.status ?: GoalStatus.ACTIVE,
            priority = priority,
            targetDate = targetDate,
            parentGoalId = existing?.parentGoalId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deletedAt = existing?.deletedAt
        )

        repo.upsert(finalGoal)
    }
}
