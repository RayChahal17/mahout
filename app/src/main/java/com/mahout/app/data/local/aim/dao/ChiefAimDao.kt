package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.ChiefAimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChiefAimDao {

    @Query("SELECT * FROM chief_aim WHERE id = 1 LIMIT 1")
    fun observeChiefAim(): Flow<ChiefAimEntity?>

    @Query("SELECT * FROM chief_aim WHERE id = 1 LIMIT 1")
    suspend fun getChiefAim(): ChiefAimEntity?

    /**
     * Upsert: replace the singleton row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChiefAimEntity)
}
