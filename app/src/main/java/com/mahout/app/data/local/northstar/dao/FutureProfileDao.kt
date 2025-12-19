package com.mahout.app.data.local.northstar.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.northstar.entity.FutureProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FutureProfileDao {

    @Query("SELECT * FROM future_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<FutureProfileEntity?>

    @Query("SELECT * FROM future_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): FutureProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FutureProfileEntity)
}
