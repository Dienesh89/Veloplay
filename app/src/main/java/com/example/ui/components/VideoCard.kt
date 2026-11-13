package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VideoEntity
import com.example.ui.theme.VeloTheme
import com.example.ui.theme.YtBadgeDark
import com.example.ui.theme.YtProgressRed
import com.example.ui.theme.YtRed
import com.example.ui.theme.YtTextPrimary
import com.example.util.FormatUtils
import java.io.File

@Composable
fun YoutubeVideoFeedCard(
    video: VideoEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VeloTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 16.dp)
            .testTag("video_card")
    ) {
        // 16:9 Full-width Thumbnail with YouTube duration pill & red watch progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surface)
        ) {
            if (!video.thumbnailPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(video.thumbnailPath),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1F1F24), Color(0xFF2B1218), Color(0xFF0F0F0F))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = YtTextPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Bottom-left saved speed badge
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (video.preferredSpeed > 1.05f || video.preferredSpeed < 0.95f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(YtRed.copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = YtTextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = FormatUtils.formatSpeed(video.preferredSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = YtTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Bottom-right duration badge (dark translucent YouTube pill)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(YtBadgeDark)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = YtTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                )
            }

            // Signature red resume progress bar along bottom edge of thumbnail
            if (video.durationMs > 0L && video.lastPositionMs > 500L) {
                val fraction = (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.02f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x66FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(YtProgressRed)
                    )
                }
            }
        }

        // Video Metadata Row (Avatar + Title + Subtitle + Overflow Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Circular channel/folder avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (video.isBundled) listOf(YtRed, Color(0xFF9E0022))
                            else listOf(colors.chipElevated, colors.chipBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = video.channelLabel.firstOrNull()?.uppercase() ?: "V",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (video.isBundled) YtTextPrimary else colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 21.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val res = FormatUtils.formatResolutionLabel(video.width, video.height)
                    val fpsTag = if (video.frameRate >= 50f) " 60fps" else ""
                    val sizeTag = FormatUtils.formatFileSize(video.fileSizeBytes)
                    val timeTag = FormatUtils.formatRelativeTime(video.lastPlayedAtMs)
                    Text(
                        text = "${video.channelLabel} • $res$fpsTag • $sizeTag • $timeTag",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (video.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = colors.red,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (video.isWatchLater) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Watch later",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_more_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Video options",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
