package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val metadataExtractor: VideoMetadataExtractor
) {
    val videosFlow: Flow<List<VideoEntity>> = videoDao.observeAllVideos()

    suspend fun syncDeviceVideos(hasStoragePermission: Boolean) = withContext(Dispatchers.IO) {
        val existingMap = videoDao.observeAllVideos().first().associateBy { it.uri }

        if (hasStoragePermission) {
            val deviceVideos = queryMediaStoreVideos(existingMap)
            if (deviceVideos.isNotEmpty()) {
                val activeUris = deviceVideos.mapTo(mutableSetOf()) { it.uri }
                // Remove old bundled samples or deleted files once device videos are loaded
                existingMap.values.forEach { old ->
                    if (old.isBundled || old.uri !in activeUris) {
                        videoDao.deleteByUri(old.uri)
                    }
                }
                deviceVideos.forEach { videoDao.upsertVideo(it) }

                // Lazily populate missing thumbnails
                deviceVideos.forEach { video ->
                    if (video.thumbnailPath.isNullOrBlank()) {
                        val thumb = metadataExtractor.getOrCreateDeviceThumbnail(Uri.parse(video.uri))
                        if (thumb != null) {
                            videoDao.upsertVideo(video.copy(thumbnailPath = thumb))
                        }
                    }
                }
                return@withContext
            }
        }

        ensureBundledSamplesSeeded()
    }

    private fun queryMediaStoreVideos(existingMap: Map<String, VideoEntity>): List<VideoEntity> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val results = mutableListOf<VideoEntity>()
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val displayName = cursor.getString(nameCol)?.substringBeforeLast('.')
                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName
                        ?: "Video $id"
                    val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)
                    val width = cursor.getInt(widthCol).takeIf { it > 0 } ?: 1280
                    val height = cursor.getInt(heightCol).takeIf { it > 0 } ?: 720
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val modifiedMs = cursor.getLong(modCol) * 1000L
                    val folder = if (bucketCol >= 0) {
                        cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Device Storage"
                    } else {
                        "Device Storage"
                    }

                    val existing = existingMap[contentUri]
                    results += VideoEntity(
                        uri = contentUri,
                        title = existing?.title ?: rawTitle,
                        channelLabel = folder,
                        durationMs = durationMs,
                        lastPositionMs = existing?.lastPositionMs ?: 0L,
                        width = width,
                        height = height,
                        fileSizeBytes = size,
                        mimeType = mime,
                        frameRate = existing?.frameRate ?: 30f,
                        thumbnailPath = existing?.thumbnailPath,
                        subtitleUri = existing?.subtitleUri,
                        addedAtMs = existing?.addedAtMs ?: modifiedMs,
                        lastPlayedAtMs = existing?.lastPlayedAtMs ?: modifiedMs,
                        isFavorite = existing?.isFavorite ?: false,
                        isWatchLater = existing?.isWatchLater ?: false,
                        isShort = height > width && durationMs in 1L..180_000L,
                        preferredSpeed = existing?.preferredSpeed ?: 1.0f,
                        preferredPitch = existing?.preferredPitch ?: 1.0f,
                        pitchLockEnabled = existing?.pitchLockEnabled ?: true,
                        isBundled = false
                    )
                }
            }
        }
        return results
    }

    suspend fun ensureBundledSamplesSeeded() {
        if (videoDao.getVideoCount() > 0) return

        val samples = listOf(
            Triple(
                R.raw.sample_showcase_hd,
                "High-Speed 60fps Motion Test — 8x Engine Benchmark",
                "VeloPlay Lab"
            ),
            Triple(
                R.raw.sample_cinema_motion,
                "Cinema Crimson Gradient & Harmonic Pitch Sweep",
                "Offline Studio"
            )
        )

        val now = System.currentTimeMillis()
        val entities = samples.mapIndexed { index, (rawResId, title, channel) ->
            val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
            val meta = metadataExtractor.extractMetadata(uri, fallbackTitle = title)
            VideoEntity(
                uri = uri.toString(),
                title = title,
                channelLabel = channel,
                durationMs = meta.durationMs,
                lastPositionMs = 0L,
                width = meta.width,
                height = meta.height,
                fileSizeBytes = meta.fileSizeBytes,
                mimeType = meta.mimeType,
                frameRate = if (rawResId == R.raw.sample_showcase_hd) 60f else meta.frameRate,
                thumbnailPath = meta.thumbnailPath,
                addedAtMs = now - (index * 60_000L),
                lastPlayedAtMs = now - (index * 60_000L),
                isShort = false,
                isBundled = true
            )
        }
        videoDao.insertVideosIgnore(entities)
    }

    suspend fun importSingleUri(uri: Uri): VideoEntity {
        val existing = videoDao.getVideoByUri(uri.toString())
        if (existing != null) return existing
        val meta = metadataExtractor.extractMetadata(uri)
        val entity = VideoEntity(
            uri = uri.toString(),
            title = meta.title,
            channelLabel = "Device Storage",
            durationMs = meta.durationMs,
            width = meta.width,
            height = meta.height,
            fileSizeBytes = meta.fileSizeBytes,
            mimeType = meta.mimeType,
            frameRate = meta.frameRate,
            thumbnailPath = meta.thumbnailPath,
            isShort = meta.isShort,
            isBundled = false
        )
        videoDao.upsertVideo(entity)
        return entity
    }

    suspend fun saveProgress(uri: String, positionMs: Long) {
        videoDao.updatePlaybackProgress(uri, positionMs.coerceAtLeast(0L), System.currentTimeMillis())
    }

    suspend fun saveSpeedAndPitch(uri: String, speed: Float, pitch: Float, pitchLock: Boolean) {
        videoDao.updateSpeedAndPitch(uri, speed, pitch, pitchLock)
    }

    suspend fun toggleFavorite(video: VideoEntity) {
        videoDao.setFavorite(video.uri, !video.isFavorite)
    }

    suspend fun toggleWatchLater(video: VideoEntity) {
        videoDao.setWatchLater(video.uri, !video.isWatchLater)
    }

    suspend fun attachSubtitle(uri: String, subtitleUri: Uri) {
        videoDao.setSubtitleUri(uri, subtitleUri.toString())
    }

    suspend fun clearSubtitle(uri: String) {
        videoDao.setSubtitleUri(uri, null)
    }

    suspend fun renameVideo(uri: String, newTitle: String) {
        if (newTitle.isNotBlank()) {
            videoDao.renameVideo(uri, newTitle.trim())
        }
    }

    suspend fun deleteVideo(uri: String) {
        videoDao.deleteByUri(uri)
    }
}
