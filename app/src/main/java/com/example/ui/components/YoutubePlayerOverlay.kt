package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.PlayerSheetMode
import com.example.ui.PlayerUiState
import com.example.ui.VideoResizeMode
import com.example.ui.theme.YtBadgeDark
import com.example.ui.theme.YtOverlayScrim
import com.example.ui.theme.YtRed
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import com.example.util.FormatUtils
import kotlinx.coroutines.delay

@Composable
fun YoutubePlayerOverlay(
    state: PlayerUiState,
    autoplayEnabled: Boolean,
    showEngineStats: Boolean,
    doubleTapSeekSeconds: Int,
    onToggleControls: () -> Unit,
    onMinimize: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleCaptions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekDeltaSeconds: (Int) -> Unit,
    onHoldSpeedStart: () -> Unit,
    onHoldSpeedEnd: () -> Unit,
    onToggleLockScreen: () -> Unit,
    onToggleResizeMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxWidthPx by remember { mutableIntStateOf(1) }
    var seekSide by remember { mutableStateOf<Int?>(null) } // -1 left, +1 right
    var accumulatedSeekSeconds by remember { mutableIntStateOf(0) }
    var lockBannerVisible by remember { mutableStateOf(false) }

    // Auto-hide controls after 3.2s when playing and not scrubbing
    LaunchedEffect(state.isControlsVisible, state.isPlaying, state.isScrubbing, state.mode) {
        if (state.isControlsVisible && state.isPlaying && !state.isScrubbing) {
            delay(3200L)
            onToggleControls()
        }
    }

    // Reset double-tap ripple indicator
    LaunchedEffect(seekSide, accumulatedSeekSeconds) {
        if (seekSide != null) {
            delay(650L)
            seekSide = null
            accumulatedSeekSeconds = 0
        }
    }

    // Auto-hide lock banner
    LaunchedEffect(lockBannerVisible) {
        if (lockBannerVisible) {
            delay(2200L)
            lockBannerVisible = false
        }
    }

    val isFullscreen = state.mode == PlayerSheetMode.FULLSCREEN

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(state.isControlsLocked, doubleTapSeekSeconds) {
                detectTapGestures(
                    onTap = {
                        if (state.isControlsLocked) {
                            lockBannerVisible = true
                        } else {
                            onToggleControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (state.isControlsLocked) {
                            lockBannerVisible = true
                        } else {
                            val isRight = offset.x > boxWidthPx / 2f
                            val delta = if (isRight) doubleTapSeekSeconds else -doubleTapSeekSeconds
                            seekSide = if (isRight) 1 else -1
                            accumulatedSeekSeconds += doubleTapSeekSeconds
                            onSeekDeltaSeconds(delta)
                        }
                    },
                    onPress = {
                        if (!state.isControlsLocked) {
                            var boostStarted = false
                            try {
                                val released = withTimeoutOrNullSafe(340L) {
                                    tryAwaitRelease()
                                }
                                if (released == null) {
                                    boostStarted = true
                                    onHoldSpeedStart()
                                    tryAwaitRelease()
                                }
                            } finally {
                                if (boostStarted) {
                                    onHoldSpeedEnd()
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Double-Tap Seek Left/Right YouTube Ripple Indicators
        AnimatedVisibility(
            visible = seekSide == -1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topEnd = 160.dp, bottomEnd = 160.dp))
                    .background(Color(0x44FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = YtTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "${accumulatedSeekSeconds} seconds",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekSide == 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 160.dp, bottomStart = 160.dp))
                    .background(Color(0x44FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = null,
                        tint = YtTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "${accumulatedSeekSeconds} seconds",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // YouTube Hold-to-2x Speed Boost Pill at Top Center
        AnimatedVisibility(
            visible = state.isSpeedBoostActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(YtBadgeDark)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.speedBoostLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = YtTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = null,
                    tint = YtTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Optional Live Engine Telemetry HUD
        if (showEngineStats && !state.isSpeedBoostActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = if (state.isControlsVisible) 52.dp else 10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val pitchLabel = if (state.pitchLockEnabled) "Lock ${FormatUtils.formatPitch(state.pitchShift)}" else "Linked"
                Text(
                    text = "${FormatUtils.formatSpeed(state.playbackSpeed)} • Pitch $pitchLabel • ${state.telemetry.videoFps.toInt()}fps • ${state.telemetry.droppedFrames} drop",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = YtTextPrimary,
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }

        // Main YouTube Controls Overlay (when unlocked and visible)
        AnimatedVisibility(
            visible = state.isControlsVisible && !state.isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99000000),
                                Color(0x33000000),
                                Color(0xAA000000)
                            )
                        )
                    )
            ) {
                // Top Row: Minimize Chevron, Title (in Fullscreen), Autoplay, PiP, Lock, CC, Settings
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = if (isFullscreen) onToggleFullscreen else onMinimize,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Minimize player",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (isFullscreen) {
                        Text(
                            text = state.currentVideo?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = YtTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // YouTube Autoplay Toggle Pill
                    YoutubeAutoplaySwitch(
                        checked = autoplayEnabled,
                        onToggle = onToggleAutoplay,
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Floating window",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleLockScreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Lock controls",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleCaptions,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_captions_button")
                    ) {
                        Icon(
                            imageVector = if (state.captionsEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                            contentDescription = "Toggle captions",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Player settings",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Center Transport Row: Previous, Play/Pause, Next
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .testTag("player_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous video",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(YtOverlayScrim)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = when {
                                state.isEnded -> Icons.Filled.Replay
                                state.isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next video",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Timestamps + Speed Badge + Fullscreen Row (just above scrubber)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${FormatUtils.formatDuration(state.positionMs)} ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                    Text(
                        text = "/ ${FormatUtils.formatDuration(state.durationMs)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = YtTextSecondary,
                            fontFeatureSettings = "tnum"
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Quick Speed Pill right on the player bar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (state.playbackSpeed != 1.0f) YtRed.copy(alpha = 0.9f)
                                else Color(0x66000000)
                            )
                            .clickable(onClick = onOpenSpeedSheet)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("overlay_speed_pill")
                    ) {
                        Text(
                            text = FormatUtils.formatSpeed(state.playbackSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = YtTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isFullscreen) {
                        IconButton(
                            onClick = onToggleResizeMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_zoom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = if (state.resizeMode == VideoResizeMode.FIT) "Zoom to fill" else "Fit screen",
                                tint = YtTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                            tint = YtTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Screen Locked Banner
        AnimatedVisibility(
            visible = state.isControlsLocked && lockBannerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(YtBadgeDark)
                    .clickable {
                        lockBannerVisible = false
                        onToggleLockScreen()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("unlock_controls_pill")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = YtTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.screen_locked_banner),
                    style = MaterialTheme.typography.labelLarge.copy(color = YtTextPrimary)
                )
            }
        }

        // Bottom Scrubber Bar: hidden completely in fullscreen when controls are hidden!
        val showScrubber = !state.isControlsLocked && (!isFullscreen || state.isControlsVisible || state.isScrubbing)
        if (showScrubber) {
            YoutubeScrubber(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedMs = state.bufferedMs,
                isControlsVisible = state.isControlsVisible,
                isScrubbing = state.isScrubbing,
                previewBitmap = state.scrubPreviewBitmap,
                onScrubStart = onScrubStart,
                onScrubChange = onScrubChange,
                onScrubEnd = onScrubEnd,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = if (isFullscreen) 12.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun YoutubeAutoplaySwitch(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color(0x88FFFFFF) else Color(0x44FFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(2.dp)
            .testTag("player_autoplay_toggle"),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) YtTextPrimary else YtTextSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (checked) "Autoplay on" else "Autoplay off",
                tint = Color.Black,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private suspend fun <T> withTimeoutOrNullSafe(
    timeMillis: Long,
    block: suspend () -> T
): T? = kotlinx.coroutines.withTimeoutOrNull(timeMillis) { block() }
