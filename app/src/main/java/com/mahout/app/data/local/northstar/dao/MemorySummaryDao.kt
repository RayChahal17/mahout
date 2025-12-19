package com.mahout.app.data.local.northstar.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.northstar.entity.MemorySummaryEntity
import com.mahout.app.domain.northstar.model.MemoryPeriodType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MemorySummaryDao {

    @Query("SELECT * FROM memory_summaries WHERE isDeleted = 0 ORDER BY periodStart DESC")
    fun observeMemories(): Flow<List<MemorySummaryEntity>>

    @Query("SELECT * FROM memory_summaries WHERE periodType = :type AND periodStart = :start LIMIT 1")
    suspend fun getMemory(type: MemoryPeriodType, start: LocalDate): MemorySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemorySummaryEntity)
}
