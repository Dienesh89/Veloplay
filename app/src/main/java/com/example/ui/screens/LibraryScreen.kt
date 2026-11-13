package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PlayerSettings
import com.example.data.ThemeMode
import com.example.data.VideoEntity
import com.example.ui.theme.VeloTheme
import com.example.ui.theme.YtBadgeDark
import com.example.ui.theme.YtProgressRed
import com.example.ui.theme.YtTextPrimary
import com.example.util.FormatUtils
import java.io.File

@Composable
fun LibraryScreen(
    allVideos: List<VideoEntity>,
    settings: PlayerSettings,
    hasStoragePermission: Boolean,
    bottomContentPadding: PaddingValues,
    onVideoClick: (VideoEntity) -> Unit,
    onRequestStoragePermission: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoPip: (Boolean) -> Unit,
    onToggleAutoplay: (Boolean) -> Unit,
    onToggleRememberSpeed: (Boolean) -> Unit,
    onToggleEngineStats: (Boolean) -> Unit,
    onSelectDoubleTapSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VeloTheme.colors
    val historyVideos = allVideos.filter { it.lastPlayedAtMs > 0L }
    val favoriteVideos = allVideos.filter { it.isFavorite }
    val watchLaterVideos = allVideos.filter { it.isWatchLater }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        // Profile / Offline Library Header matching app logo
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_art),
                        contentDescription = "VeloPlay Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Library",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${allVideos.size} videos • ExoPlayer 8x Sonic Engine",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                    )
                }
            }
        }

        if (!hasStoragePermission) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.chipBackground)
                        .clickable(onClick = onRequestStoragePermission)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Storage Permission Needed",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Tap to allow access and scan all videos on your device",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                    }
                    Text(
                        text = "Allow",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colors.accentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Appearance Mode Selector (System / Light / Dark)
        item {
            SectionHeaderRow(icon = Icons.Filled.Contrast, title = "Appearance")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val selected = settings.themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.selectedChipBackground else colors.chipBackground)
                            .clickable { onSelectThemeMode(mode) }
                            .padding(vertical = 10.dp)
                            .testTag("theme_option_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selected) colors.selectedChipText else colors.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // History Carousel
        if (historyVideos.isNotEmpty()) {
            item {
                SectionHeaderRow(icon = Icons.Filled.History, title = "History")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyVideos.take(10), key = { it.uri }) { video ->
                        HistoryMiniCard(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Playlists / Collections Summary
        item {
            SectionHeaderRow(icon = Icons.Filled.VideoLibrary, title = "Playlists")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
            ) {
                PlaylistRow(
                    icon = Icons.Filled.Favorite,
                    iconTint = colors.red,
                    title = "Liked & Favorites",
                    count = favoriteVideos.size,
                    onClick = { favoriteVideos.firstOrNull()?.let(onVideoClick) }
                )
                HorizontalDivider(color = colors.divider)
                PlaylistRow(
                    icon = Icons.Filled.Bookmark,
                    iconTint = colors.accentBlue,
                    title = "Watch later",
                    count = watchLaterVideos.size,
                    onClick = { watchLaterVideos.firstOrNull()?.let(onVideoClick) }
                )
                HorizontalDivider(color = colors.divider)
                PlaylistRow(
                    icon = Icons.Filled.VideoLibrary,
                    iconTint = colors.textPrimary,
                    title = "All offline videos",
                    count = allVideos.size,
                    onClick = { allVideos.firstOrNull()?.let(onVideoClick) }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Engine & Player Preferences (including Background Play & Auto PiP)
        item {
            SectionHeaderRow(icon = Icons.Filled.Speed, title = "8x Engine, PiP & Background Play")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .padding(vertical = 4.dp)
            ) {
                PreferenceToggleRow(
                    title = "Background play",
                    subtitle = "Continue audio & media controls when leaving app",
                    checked = settings.backgroundPlayEnabled,
                    onCheckedChange = onToggleBackgroundPlay
                )
                HorizontalDivider(color = colors.divider)
                PreferenceToggleRow(
                    title = "Auto floating window (PiP)",
                    subtitle = "Automatically pop into floating window on Home",
                    checked = settings.autoPipEnabled,
                    onCheckedChange = onToggleAutoPip
                )
                HorizontalDivider(color = colors.divider)
                PreferenceToggleRow(
                    title = "Autoplay next video",
                    subtitle = "Automatically play next offline video in queue",
                    checked = settings.autoplayNext,
                    onCheckedChange = onToggleAutoplay
                )
                HorizontalDivider(color = colors.divider)
                PreferenceToggleRow(
                    title = "Remember speed & pitch per video",
                    subtitle = "Saves your custom 0.25x–8.0x speed per file",
                    checked = settings.rememberSpeedPerVideo,
                    onCheckedChange = onToggleRememberSpeed
                )
                HorizontalDivider(color = colors.divider)
                PreferenceToggleRow(
                    title = "Show live engine telemetry HUD",
                    subtitle = "Displays FPS, codec, pitch, and dropped frames",
                    checked = settings.showEngineStats,
                    onCheckedChange = onToggleEngineStats
                )
                HorizontalDivider(color = colors.divider)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Double-tap seek interval",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            val selected = settings.doubleTapSeekSeconds == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.selectedChipBackground else colors.chipBackground)
                                    .clickable { onSelectDoubleTapSeek(sec) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${sec}s",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (selected) colors.selectedChipText else colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(icon: ImageVector, title: String) {
    val colors = VeloTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun HistoryMiniCard(
    video: VideoEntity,
    onClick: () -> Unit
) {
    val colors = VeloTheme.colors
    Column(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
        ) {
            if (!video.thumbnailPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(video.thumbnailPath),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(YtBadgeDark)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = YtTextPrimary,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
            if (video.durationMs > 0L && video.lastPositionMs > 0L) {
                val frac = (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.03f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color(0x55FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(2.5.dp)
                            .background(YtProgressRed)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    val colors = VeloTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count videos",
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.textSecondary,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = VeloTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.accentBlue,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.chipElevated
            )
        )
    }
}
