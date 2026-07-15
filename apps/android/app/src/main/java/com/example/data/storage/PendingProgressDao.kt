package com.example.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingProgressDao {
    @Query("SELECT * FROM pending_progress")
    suspend fun getAllPendingProgress(): List<PendingProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingProgress(progress: PendingProgress)

    @Query("DELETE FROM pending_progress WHERE videoAssetId = :videoAssetId")
    suspend fun deletePendingProgressById(videoAssetId: String)
}
