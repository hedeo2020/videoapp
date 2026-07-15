package com.example.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey val videoAssetId: String,
    val title: String,
    val artworkUrl: String?,
    val duration: String?,
    val localPath: String,
    val downloadedAt: Long,
    val expiresAt: Long?
)
