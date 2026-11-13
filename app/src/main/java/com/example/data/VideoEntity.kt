package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val channelLabel: String,
    val durationMs: Long,
    val lastPositionMs: Long = 0L,
    val width: Int = 1280,
    val height: Int = 720,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "video/mp4",
    val frameRate: Float = 30f,
    val thumbnailPath: String? = null,
    val subtitleUri: String? = null,
    val addedAtMs: Long = System.currentTimeMillis(),
    val lastPlayedAtMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isWatchLater: Boolean = false,
    val isShort: Boolean = false,
    val preferredSpeed: Float = 1.0f,
    val preferredPitch: Float = 1.0f,
    val pitchLockEnabled: Boolean = true,
    val isBundled: Boolean = false
)
