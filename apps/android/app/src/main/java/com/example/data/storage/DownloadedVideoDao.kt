package com.example.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedVideoDao {
    @Query("SELECT * FROM downloaded_videos ORDER BY downloadedAt DESC")
    fun getAllDownloadedVideos(): Flow<List<DownloadedVideo>>

    @Query("SELECT * FROM downloaded_videos WHERE videoAssetId = :videoId LIMIT 1")
    suspend fun getDownloadedVideoById(videoId: String): DownloadedVideo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedVideo(video: DownloadedVideo)

    @Query("DELETE FROM downloaded_videos WHERE videoAssetId = :videoId")
    suspend fun deleteDownloadedVideoById(videoId: String)
}
