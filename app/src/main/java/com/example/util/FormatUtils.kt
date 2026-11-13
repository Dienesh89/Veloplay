package com.example.util

import java.util.Locale
import kotlin.math.roundToInt

object FormatUtils {
    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatSpeed(speed: Float): String {
        val rounded = (speed * 100f).roundToInt() / 100f
        return when {
            rounded % 1f == 0f -> String.format(Locale.US, "%.1fx", rounded)
            (rounded * 10f) % 1f == 0f -> String.format(Locale.US, "%.1fx", rounded)
            else -> String.format(Locale.US, "%.2fx", rounded)
        }
    }

    fun formatPitch(pitch: Float): String {
        val rounded = (pitch * 100f).roundToInt() / 100f
        return String.format(Locale.US, "%.2fx", rounded)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Local"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    fun formatResolutionLabel(width: Int, height: Int): String {
        val minDim = minOf(width, height)
        return when {
            minDim >= 2160 -> "4K"
            minDim >= 1440 -> "1440p"
            minDim >= 1080 -> "1080p"
            minDim >= 720 -> "720p"
            minDim >= 480 -> "480p"
            minDim > 0 -> "${minDim}p"
            else -> "HD"
        }
    }

    fun formatRelativeTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Offline"
        val diff = System.currentTimeMillis() - timestampMs
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
}
