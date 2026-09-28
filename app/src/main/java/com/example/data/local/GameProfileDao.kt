package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY appName ASC")
    fun getAllProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfile(packageName: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GameProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :packageName")
    suspend fun deleteProfileByPackage(packageName: String)
}
