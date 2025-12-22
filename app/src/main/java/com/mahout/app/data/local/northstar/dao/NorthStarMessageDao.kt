package com.mahout.app.data.local.northstar.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.northstar.entity.NorthStarMessageEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface NorthStarMessageDao {

    @Query(
        """
        SELECT * FROM north_star_messages
        WHERE isDeleted = 0
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int = 50): Flow<List<NorthStarMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NorthStarMessageEntity)

    @Query("UPDATE north_star_messages SET isDeleted = 1, deletedAt = :now, updatedAt = :now WHERE messageId = :id")
    suspend fun softDelete(id: String, now: Instant)

    @Query("SELECT * FROM north_star_messages WHERE messageId = :id LIMIT 1")
    suspend fun getById(id: String): NorthStarMessageEntity?
}
