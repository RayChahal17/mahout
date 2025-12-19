package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.ActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {

    @Query("SELECT * FROM actions WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeActiveActions(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE actionId = :actionId LIMIT 1")
    suspend fun getAction(actionId: String): ActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: ActionEntity)

    @Query("UPDATE actions SET isArchived = 1, archivedAt = :archivedAt, updatedAt = :archivedAt WHERE actionId = :actionId")
    suspend fun archive(actionId: String, archivedAt: java.time.Instant)
}
