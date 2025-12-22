package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.ChiefAim
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain-level contract for Chief Aim data.
 *
 * V1 is local-first (Room). This interface lets us swap implementations later
 * (e.g., Firestore sync in V2) without touching UI or ViewModels.
 */
interface ChiefAimRepository {

    /**
     * Observe the current Chief Aim (or null if the user hasn't set it yet).
     *
     * Room Flows are "live": they emit again whenever the underlying table changes.
     */
    fun observeChiefAim(): Flow<ChiefAim?>

    /**
     * Upsert (create or update) the singleton Chief Aim.
     *
     * Business rule:
     * - title must be non-blank (we validate in UI, and double-check in UseCase).
     *
     * Implementation detail:
     * - createdAt should stay stable after the first creation.
     * - updatedAt should always be "now".
     */
    suspend fun upsert(title: String, description: String?, targetDate: LocalDate?)
}
