package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY lastPlayedAtMs DESC, addedAtMs DESC")
    fun observeAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideosIgnore(videos: List<VideoEntity>)

    @Query("UPDATE videos SET lastPositionMs = :positionMs, lastPlayedAtMs = :playedAtMs WHERE uri = :uri")
    suspend fun updatePlaybackProgress(uri: String, positionMs: Long, playedAtMs: Long)

    @Query("UPDATE videos SET preferredSpeed = :speed, preferredPitch = :pitch, pitchLockEnabled = :pitchLock WHERE uri = :uri")
    suspend fun updateSpeedAndPitch(uri: String, speed: Float, pitch: Float, pitchLock: Boolean)

    @Query("UPDATE videos SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query("UPDATE videos SET isWatchLater = :watchLater WHERE uri = :uri")
    suspend fun setWatchLater(uri: String, watchLater: Boolean)

    @Query("UPDATE videos SET subtitleUri = :subtitleUri WHERE uri = :uri")
    suspend fun setSubtitleUri(uri: String, subtitleUri: String?)

    @Query("UPDATE videos SET title = :title WHERE uri = :uri")
    suspend fun renameVideo(uri: String, title: String)

    @Query("DELETE FROM videos WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
