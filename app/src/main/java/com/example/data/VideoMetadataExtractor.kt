package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

data class ExtractedVideoMetadata(
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val fileSizeBytes: Long,
    val mimeType: String,
    val thumbnailPath: String?,
    val isShort: Boolean
)

class VideoMetadataExtractor(private val context: Context) {

    private val previewFrameCache = object : LruCache<String, Bitmap>(36) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    private val previewDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val retrieverMutex = Mutex()
    private var cachedPreviewUri: String? = null
    private var cachedPreviewRetriever: MediaMetadataRetriever? = null

    suspend fun extractMetadata(
        uri: Uri,
        fallbackTitle: String? = null
    ): ExtractedVideoMetadata = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = fallbackTitle ?: "Offline Video"
        var fileSize = 0L

        if (uri.scheme == "content") {
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx >= 0) {
                                cursor.getString(nameIdx)?.let { raw ->
                                    displayName = raw.substringBeforeLast('.').ifBlank { raw }
                                }
                            }
                            if (sizeIdx >= 0) {
                                fileSize = cursor.getLong(sizeIdx)
                            }
                        }
                    }
            }
        } else if (uri.scheme == "android.resource") {
            runCatching {
                val resId = uri.lastPathSegment?.toIntOrNull()
                if (resId != null) {
                    context.resources.openRawResourceFd(resId).use { afd ->
                        fileSize = afd.length
                    }
                }
            }
        }

        val mimeType = resolver.getType(uri) ?: "video/mp4"
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 1280
        var height = 720
        var frameRate = 30f
        var thumbnailPath: String? = null

        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                width = rawHeight
                height = rawWidth
            } else {
                width = rawWidth
                height = rawHeight
            }
            frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                ?: 30f

            val frameTimeUs = when {
                durationMs > 4000L -> 2_000_000L
                durationMs > 1000L -> durationMs * 400L
                else -> 0L
            }
            val rawBitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (rawBitmap != null) {
                val softwareBitmap = ensureSoftwareArgbBitmap(rawBitmap)
                thumbnailPath = saveThumbnailToCache(uri.toString(), softwareBitmap)
                if (softwareBitmap !== rawBitmap) softwareBitmap.recycle()
                rawBitmap.recycle()
            }
        } catch (_: Throwable) {
            // Keep defaults if unsupported metadata
        } finally {
            runCatching { retriever.release() }
        }

        val isPortraitShort = height > width && (durationMs in 1L..180_000L)
        ExtractedVideoMetadata(
            title = displayName,
            durationMs = durationMs,
            width = width,
            height = height,
            frameRate = frameRate,
            fileSizeBytes = fileSize,
            mimeType = mimeType,
            thumbnailPath = thumbnailPath,
            isShort = isPortraitShort
        )
    }

    suspend fun getOrCreateDeviceThumbnail(uri: Uri): String? = withContext(Dispatchers.IO) {
        val existingFile = thumbnailFileForKey(uri.toString())
        if (existingFile.exists() && existingFile.length() > 0L) {
            return@withContext existingFile.absolutePath
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sysThumb = runCatching {
                context.contentResolver.loadThumbnail(uri, Size(640, 360), null)
            }.getOrNull()
            if (sysThumb != null) {
                val sw = ensureSoftwareArgbBitmap(sysThumb)
                val path = saveThumbnailToCache(uri.toString(), sw)
                if (sw !== sysThumb) sw.recycle()
                sysThumb.recycle()
                if (path != null) return@withContext path
            }
        }

        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val timeUs = if (durationMs > 3000L) 1_500_000L else (durationMs * 350L)
            val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return@withContext null
            val sw = ensureSoftwareArgbBitmap(raw)
            val path = saveThumbnailToCache(uri.toString(), sw)
            if (sw !== raw) sw.recycle()
            raw.recycle()
            path
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun loadCachedThumbnailBitmap(thumbnailPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (thumbnailPath.isNullOrBlank()) return@withContext null
        runCatching {
            val file = File(thumbnailPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }

    suspend fun extractPreviewFrame(uriString: String, positionMs: Long): Bitmap? = withContext(previewDispatcher) {
        val bucketMs = (positionMs.coerceAtLeast(0L) / 500L) * 500L
        val cacheKey = "$uriString#$bucketMs"
        previewFrameCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        retrieverMutex.withLock {
            previewFrameCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }

            try {
                if (cachedPreviewUri != uriString || cachedPreviewRetriever == null) {
                    runCatching { cachedPreviewRetriever?.release() }
                    cachedPreviewRetriever = MediaMetadataRetriever().apply {
                        setDataSource(context, Uri.parse(uriString))
                    }
                    cachedPreviewUri = uriString
                }

                val retriever = cachedPreviewRetriever ?: return@withLock null
                val timeUs = bucketMs * 1000L

                val raw: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        320,
                        180
                    ) ?: retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        320,
                        180
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                }

                if (raw == null) return@withLock null

                val software = ensureSoftwareArgbBitmap(raw)
                val targetWidth = 320
                val aspect = if (software.height > 0) software.width.toFloat() / software.height.toFloat() else 16f / 9f
                val targetHeight = (targetWidth / aspect).toInt().coerceIn(90, 320)
                val scaled = if (software.width > 340 || software.height > 340) {
                    Bitmap.createScaledBitmap(software, targetWidth, targetHeight, true)
                } else {
                    software
                }
                if (software !== raw) raw.recycle()
                if (scaled !== software) software.recycle()

                previewFrameCache.put(cacheKey, scaled)
                scaled
            } catch (_: Throwable) {
                runCatching { cachedPreviewRetriever?.release() }
                cachedPreviewRetriever = null
                cachedPreviewUri = null
                null
            }
        }
    }

    private fun ensureSoftwareArgbBitmap(source: Bitmap): Bitmap {
        return if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) ||
            source.config != Bitmap.Config.ARGB_8888
        ) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: source
        } else {
            source
        }
    }

    private fun thumbnailFileForKey(key: String): File {
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)
        return File(dir, "$hash.jpg")
    }

    private fun saveThumbnailToCache(key: String, bitmap: Bitmap): String? {
        val file = thumbnailFileForKey(key)
        val targetWidth = 640
        val aspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 16f / 9f
        val targetHeight = (targetWidth / aspect).toInt().coerceIn(180, 1140)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        return try {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled != bitmap) scaled.recycle()
            file.absolutePath
        } catch (_: Throwable) {
            if (scaled != bitmap) scaled.recycle()
            null
        }
    }
}
