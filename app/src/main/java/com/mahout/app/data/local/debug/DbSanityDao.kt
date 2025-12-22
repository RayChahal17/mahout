package com.mahout.app.data.local.debug

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface DbSanityDao {

    // Lists app tables (excludes Room + Android internal tables)
    @Query(
        """
        SELECT name 
        FROM sqlite_master 
        WHERE type='table' 
          AND name NOT LIKE 'android_%'
          AND name NOT LIKE 'room_%'
        ORDER BY name
        """
    )
    suspend fun listAppTables(): List<String>

    @RawQuery
    suspend fun countRows(query: SupportSQLiteQuery): Long
}
