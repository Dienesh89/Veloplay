package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ThemeMode
import com.example.data.VideoEntity
import com.example.ui.FeedFilter
import com.example.ui.components.YoutubeTopBar
import com.example.ui.components.YoutubeVideoFeedCard
import com.example.ui.theme.VeloTheme
import com.example.ui.theme.YtTextPrimary

@Composable
fun HomeScreen(
    videos: List<VideoEntity>,
    selectedFilter: FeedFilter,
    isSearchExpanded: Boolean,
    searchQuery: String,
    showEngineStats: Boolean,
    themeMode: ThemeMode,
    hasStoragePermission: Boolean,
    bottomContentPadding: PaddingValues,
    onSelectFilter: (FeedFilter) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleEngineStats: () -> Unit,
    onCycleThemeMode: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onVideoClick: (VideoEntity) -> Unit,
    onVideoMoreClick: (VideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VeloTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        YoutubeTopBar(
            isSearchExpanded = isSearchExpanded,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
            showEngineStats = showEngineStats,
            themeMode = themeMode,
            onSearchToggle = onSearchToggle,
            onSearchQueryChange = onSearchQueryChange,
            onSelectFilter = onSelectFilter,
            onToggleEngineStats = onToggleEngineStats,
            onCycleThemeMode = onCycleThemeMode
        )

        if (!hasStoragePermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.chipBackground)
                    .clickable(onClick = onRequestStoragePermission)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("storage_permission_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = colors.red,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Load all device videos automatically",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Grant storage access to display every video on your phone",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Allow",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.accentBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (videos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = bottomContentPadding.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_videos),
                    contentDescription = "Offline video library illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 240.dp, height = 180.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching videos" else stringResource(R.string.empty_library_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Try another title or switch back to All videos."
                    } else {
                        stringResource(R.string.empty_library_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                    textAlign = TextAlign.Center
                )
                if (!hasStoragePermission) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRequestStoragePermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.red,
                            contentColor = YtTextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("empty_permission_button")
                    ) {
                        Text(
                            text = stringResource(R.string.grant_storage_permission),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_video_list"),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
                )
            ) {
                items(videos, key = { it.uri }) { video ->
                    YoutubeVideoFeedCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onMoreClick = { onVideoMoreClick(video) }
                    )
                }
            }
        }
    }
}
