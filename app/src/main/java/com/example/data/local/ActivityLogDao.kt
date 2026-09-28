package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("UPDATE activity_logs SET reverted = 1 WHERE id = :id")
    suspend fun markReverted(id: Long)

    @Query("UPDATE activity_logs SET reverted = 1")
    suspend fun markAllReverted()

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
