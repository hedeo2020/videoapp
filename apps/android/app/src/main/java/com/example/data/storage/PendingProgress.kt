package com.example.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_progress")
data class PendingProgress(
    @PrimaryKey val videoAssetId: String,
    val positionSeconds: Int,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
