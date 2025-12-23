package com.mahout.app.domain.path.repository

import com.mahout.app.domain.path.model.Action
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Domain-layer contract for Path Actions.
 *
 * IMPORTANT (V1 Scope Lock):
 * - Actions are TIME-based only (no checklists / no "checks").
 * - We soft-archive actions instead of deleting, so history can exist later.
 *
 * Backwards-compatibility note:
 * ----------------------------
 * Earlier code in this repo uses:
 *    archive(actionId, archivedAt)
 *
 * Newer Day 12 code (ArchiveActionUseCase) expects:
 *    archiveAction(actionId, archivedAt)
 *
 * We keep the original method AND add a safe alias so we don't break old code.
 */
interface ActionRepository {

    /**
     * Observe only ACTIVE (non-archived) actions.
     * This is what Path tab lists.
     */
    fun observeActiveActions(): Flow<List<Action>>

    /**
     * One-shot fetch of a single action (nullable if not found).
     */
    suspend fun getAction(actionId: String): Action?

    /**
     * Insert or update an action.
     * Upsert means: if id exists -> update, else -> insert.
     */
    suspend fun upsert(action: Action)

    /**
     * Original/legacy archive API.
     * Implementations (RoomActionRepository) already implement THIS.
     */
    suspend fun archive(actionId: String, archivedAt: Instant)

    /**
     * New Day 12 API name (alias).
     *
     * We provide a DEFAULT implementation so:
     * - repositories do NOT need to change
     * - old working code stays working
     * - new usecase compiles
     */
    suspend fun archiveAction(actionId: String, archivedAt: Instant) {
        archive(actionId = actionId, archivedAt = archivedAt)
    }
}
