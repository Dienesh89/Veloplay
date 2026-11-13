package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.VideoEntity
import com.example.player.EngineTelemetry
import com.example.ui.theme.VeloTheme
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeSettingsBottomSheet(
    playbackSpeed: Float,
    pitchShift: Float,
    pitchLockEnabled: Boolean,
    isLooping: Boolean,
    captionsEnabled: Boolean,
    hasSubtitleTrack: Boolean,
    showEngineStats: Boolean,
    backgroundPlayEnabled: Boolean,
    telemetry: EngineTelemetry,
    onDismiss: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleCaptions: () -> Unit,
    onPickSubtitleFile: () -> Unit,
    onToggleLockScreen: () -> Unit,
    onToggleEngineStats: () -> Unit
) {
    val colors = VeloTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetSurface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            SettingsRowItem(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.playback_speed),
                valueText = FormatUtils.formatSpeed(playbackSpeed),
                highlightValue = playbackSpeed != 1.0f,
                onClick = {
                    onDismiss()
                    onOpenSpeedSheet()
                },
                testTag = "settings_playback_speed_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.GraphicEq,
                title = stringResource(R.string.audio_pitch),
                valueText = if (pitchLockEnabled && pitchShift == 1.0f) {
                    "Natural (Locked)"
                } else {
                    "${FormatUtils.formatPitch(pitchShift)}${if (pitchLockEnabled) " • Locked" else " • Linked"}"
                },
                highlightValue = pitchShift != 1.0f || !pitchLockEnabled,
                onClick = {
                    onDismiss()
                    onOpenSpeedSheet()
                },
                testTag = "settings_audio_pitch_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.PictureInPictureAlt,
                title = stringResource(R.string.pip_mode),
                valueText = "Open",
                onClick = {
                    onDismiss()
                    onEnterPip()
                },
                testTag = "settings_pip_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.Headphones,
                title = stringResource(R.string.background_play),
                valueText = if (backgroundPlayEnabled) "On" else "Off",
                highlightValue = backgroundPlayEnabled,
                onClick = onToggleBackgroundPlay,
                testTag = "settings_background_play_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.HighQuality,
                title = stringResource(R.string.quality_decoder),
                valueText = "${FormatUtils.formatResolutionLabel(telemetry.videoWidth, telemetry.videoHeight)} • ${telemetry.videoFps.toInt()}fps",
                onClick = {
                    onDismiss()
                    onToggleEngineStats()
                },
                testTag = "settings_quality_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.RepeatOne,
                title = stringResource(R.string.loop_video),
                valueText = if (isLooping) "On" else "Off",
                highlightValue = isLooping,
                onClick = onToggleLoop,
                testTag = "settings_loop_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.ClosedCaption,
                title = stringResource(R.string.captions),
                valueText = when {
                    !hasSubtitleTrack -> "Load .SRT / .VTT"
                    captionsEnabled -> "On"
                    else -> "Off"
                },
                onClick = {
                    if (hasSubtitleTrack) onToggleCaptions() else {
                        onDismiss()
                        onPickSubtitleFile()
                    }
                },
                testTag = "settings_captions_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.Tune,
                title = "Engine Telemetry HUD",
                valueText = if (showEngineStats) "On" else "Off",
                highlightValue = showEngineStats,
                onClick = onToggleEngineStats,
                testTag = "settings_telemetry_row"
            )
            SettingsRowItem(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.lock_screen),
                valueText = "",
                onClick = {
                    onDismiss()
                    onToggleLockScreen()
                },
                testTag = "settings_lock_row"
            )
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    valueText: String,
    highlightValue: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = VeloTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f)
        )
        if (valueText.isNotEmpty()) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (highlightValue) colors.accentBlue else colors.textSecondary,
                    fontWeight = if (highlightValue) FontWeight.SemiBold else FontWeight.Normal,
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YoutubeSpeedAndPitchSheet(
    playbackSpeed: Float,
    pitchShift: Float,
    pitchLockEnabled: Boolean,
    telemetry: EngineTelemetry,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchLockToggle: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VeloTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedPresets = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
    val pitchPresets = listOf(0.75f, 1.0f, 1.25f, 1.5f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetSurface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.playback_speed),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("reset_speed_pitch_button")
                ) {
                    Text(
                        text = "Reset 1.0x",
                        color = colors.accentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onSpeedChange((playbackSpeed - 0.05f).coerceAtLeast(0.25f)) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.chipBackground)
                        .testTag("speed_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease speed",
                        tint = colors.textPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    Text(
                        text = FormatUtils.formatSpeed(playbackSpeed),
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = if (playbackSpeed > 2.0f) colors.red else colors.textPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.testTag("speed_value_readout")
                    )
                    Text(
                        text = when {
                            playbackSpeed >= 6.0f -> "Hyper Speed • Zero-Stutter Async"
                            playbackSpeed >= 2.5f -> "Turbo Speed • Smooth Audio DSP"
                            playbackSpeed < 1.0f -> "Slow Motion"
                            else -> "Standard Playback"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )
                }

                IconButton(
                    onClick = { onSpeedChange((playbackSpeed + 0.05f).coerceAtMost(8.0f)) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.chipBackground)
                        .testTag("speed_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase speed",
                        tint = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = playbackSpeed.coerceIn(0.25f, 8.0f),
                onValueChange = { raw ->
                    val stepped = (kotlin.math.round(raw * 20f) / 20f).coerceIn(0.25f, 8.0f)
                    onSpeedChange(stepped)
                },
                valueRange = 0.25f..8.0f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.textPrimary,
                    activeTrackColor = colors.red,
                    inactiveTrackColor = colors.chipElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_speed_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("0.25x", "1x", "2x", "4x", "6x", "8x").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                speedPresets.forEach { preset ->
                    val selected = kotlin.math.abs(playbackSpeed - preset) < 0.03f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) colors.selectedChipBackground else colors.chipBackground)
                            .clickable { onSpeedChange(preset) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("speed_preset_${preset}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = FormatUtils.formatSpeed(preset),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selected) colors.selectedChipText else colors.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.chipBackground)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = colors.accentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pitch_lock_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = stringResource(R.string.pitch_lock_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                    }
                    Switch(
                        checked = pitchLockEnabled,
                        onCheckedChange = onPitchLockToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.sheetSurface,
                            checkedTrackColor = colors.accentBlue,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.chipElevated
                        ),
                        modifier = Modifier.testTag("pitch_lock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Smooth Pitch Shift",
                        style = MaterialTheme.typography.labelLarge.copy(color = colors.textPrimary)
                    )
                    Text(
                        text = FormatUtils.formatPitch(pitchShift),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.accentBlue,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier.testTag("pitch_value_readout")
                    )
                }

                Slider(
                    value = pitchShift.coerceIn(0.50f, 2.00f),
                    onValueChange = { raw ->
                        val stepped = (kotlin.math.round(raw * 20f) / 20f).coerceIn(0.50f, 2.00f)
                        onPitchChange(stepped)
                    },
                    valueRange = 0.50f..2.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentBlue,
                        activeTrackColor = colors.accentBlue,
                        inactiveTrackColor = colors.chipElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pitch_shift_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pitchPresets.forEach { preset ->
                        val selected = kotlin.math.abs(pitchShift - preset) < 0.03f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) colors.accentBlue else colors.sheetSurface)
                                .clickable { onPitchChange(preset) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormatUtils.formatPitch(preset),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selected) colors.sheetSurface else colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = colors.successGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sonic Float DSP • Async MediaCodec • ${telemetry.droppedFrames} dropped frames",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDescriptionBottomSheet(
    video: VideoEntity,
    telemetry: EngineTelemetry,
    onDismiss: () -> Unit
) {
    val colors = VeloTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetSurface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Description & Media Specs",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.chipBackground)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecStatColumn(
                    value = "${telemetry.videoWidth.takeIf { it > 0 } ?: video.width}×${telemetry.videoHeight.takeIf { it > 0 } ?: video.height}",
                    label = "Resolution"
                )
                SpecStatColumn(
                    value = "${(telemetry.videoFps.takeIf { it > 0f } ?: video.frameRate).toInt()} fps",
                    label = "Frame Rate"
                )
                SpecStatColumn(
                    value = FormatUtils.formatFileSize(video.fileSizeBytes),
                    label = "File Size"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Video Decoder: ${telemetry.videoCodec}",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Audio Pipeline: ${telemetry.audioCodec}",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Container: ${video.mimeType} • Dropped frames: ${telemetry.droppedFrames}",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
        }
    }
}

@Composable
private fun SpecStatColumn(value: String, label: String) {
    val colors = VeloTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
        )
    }
}
