package com.mahout.app.domain.path.repository

import com.mahout.app.domain.path.model.Action
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ActionRepository {
    fun observeActiveActions(): Flow<List<Action>>
    suspend fun getAction(actionId: String): Action?
    suspend fun upsert(action: Action)

    /**
     * Soft archive (keeps history intact).
     */
    suspend fun archive(actionId: String, archivedAt: Instant)
}
