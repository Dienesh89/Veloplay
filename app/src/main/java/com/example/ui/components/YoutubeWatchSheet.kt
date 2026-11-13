package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.R
import com.example.data.VideoEntity
import com.example.ui.PlayerSheetMode
import com.example.ui.PlayerUiState
import com.example.ui.VideoResizeMode
import com.example.ui.theme.VeloTheme
import com.example.ui.theme.YtBadgeDark
import com.example.ui.theme.YtProgressRed
import com.example.ui.theme.YtRed
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTrueBlack
import com.example.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun YoutubeWatchOverlay(
    player: Player,
    state: PlayerUiState,
    upNextVideos: List<VideoEntity>,
    autoplayEnabled: Boolean,
    showEngineStats: Boolean,
    backgroundPlayEnabled: Boolean,
    doubleTapSeekSeconds: Int,
    onTogglePlayPause: () -> Unit,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onPipBoundsChanged: (Rect) -> Unit,
    onSelectVideo: (VideoEntity) -> Unit,
    onVideoMoreClick: (VideoEntity) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekDeltaSeconds: (Int) -> Unit,
    onHoldSpeedStart: () -> Unit,
    onHoldSpeedEnd: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchLockToggle: (Boolean) -> Unit,
    onResetSpeedAndPitch: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleCaptions: () -> Unit,
    onPickSubtitleFile: () -> Unit,
    onToggleLockScreen: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleResizeMode: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleEngineStats: () -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit,
    onToggleWatchLater: (VideoEntity) -> Unit,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = state.currentVideo ?: return
    val colors = VeloTheme.colors
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    val dragOffsetY = remember { Animatable(0f) }
    val miniDragOffsetX = remember { Animatable(0f) }
    val transitionScale = remember { Animatable(1f) }
    val transitionAlpha = remember { Animatable(1f) }

    // Smooth entry animation whenever transitioning between MINI / EXPANDED / FULLSCREEN
    LaunchedEffect(state.mode) {
        dragOffsetY.snapTo(0f)
        miniDragOffsetX.snapTo(0f)
        if (state.mode == PlayerSheetMode.FULLSCREEN || state.mode == PlayerSheetMode.EXPANDED) {
            transitionScale.snapTo(0.93f)
            transitionAlpha.snapTo(0.84f)
            launch {
                transitionScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                transitionAlpha.animateTo(1f, tween(210))
            }
        } else {
            transitionScale.snapTo(1f)
            transitionAlpha.snapTo(1f)
        }
    }

    // Keep screen awake while playing video
    DisposableEffect(state.isPlaying, state.mode) {
        val window = (context as? Activity)?.window
        if (state.isPlaying && state.mode != PlayerSheetMode.HIDDEN) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle fullscreen orientation & system bars cleanly
    DisposableEffect(state.mode) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
        if (state.mode == PlayerSheetMode.FULLSCREEN) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (state.mode == PlayerSheetMode.FULLSCREEN) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // System Back handling
    BackHandler(enabled = state.mode == PlayerSheetMode.FULLSCREEN || state.mode == PlayerSheetMode.EXPANDED) {
        when (state.mode) {
            PlayerSheetMode.FULLSCREEN -> onToggleFullscreen()
            PlayerSheetMode.EXPANDED -> onMinimize()
            else -> Unit
        }
    }

    val contentScale = if (state.mode == PlayerSheetMode.FULLSCREEN && state.resizeMode == VideoResizeMode.ZOOM) {
        ContentScale.Crop
    } else {
        ContentScale.Fit
    }

    // Responsive YouTube-style thresholds: slight slide minimizes / enters / exits fullscreen
    val minimizeThresholdPx = with(density) { 52.dp.toPx() }
    val fullscreenSwipeUpThresholdPx = with(density) { -46.dp.toPx() }
    val fullscreenExitThresholdPx = with(density) { 52.dp.toPx() }
    val miniDismissThresholdPx = with(density) { 96.dp.toPx() }

    when (state.mode) {
        PlayerSheetMode.HIDDEN -> Unit

        PlayerSheetMode.MINI -> {
            val swipeAlpha = (1f - (abs(miniDragOffsetX.value) / (miniDismissThresholdPx * 2f))).coerceIn(0.35f, 1f)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .offset { IntOffset(miniDragOffsetX.value.roundToInt(), 0) }
                    .alpha(swipeAlpha)
                    .background(colors.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpand
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -18f) {
                                    onExpand()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    miniDragOffsetX.snapTo(miniDragOffsetX.value + dragAmount)
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (abs(miniDragOffsetX.value) > miniDismissThresholdPx) {
                                        onClose()
                                    } else {
                                        miniDragOffsetX.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { miniDragOffsetX.animateTo(0f, spring()) }
                            }
                        )
                    }
                    .testTag("youtube_mini_player")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(62.dp)
                            .background(YtTrueBlack)
                    ) {
                        ContentFrame(
                            player = player,
                            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = video.channelLabel,
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.playbackSpeed != 1.0f) {
                                Text(
                                    text = " • ${FormatUtils.formatSpeed(state.playbackSpeed)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.red,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close player",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Thin red progress bar at bottom of MiniPlayer
                val safeDur = state.durationMs.coerceAtLeast(1L)
                val fraction = (state.positionMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(2.dp)
                            .background(YtProgressRed)
                    )
                }
            }
        }

        PlayerSheetMode.FULLSCREEN -> {
            // Live interactive slide-down exit fullscreen animation
            val exitDragProgress = (dragOffsetY.value / (fullscreenExitThresholdPx * 3f)).coerceIn(0f, 1f)
            val activeScale = transitionScale.value * (1f - exitDragProgress * 0.14f)
            val cornerDp = (exitDragProgress * 22f).dp

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(YtTrueBlack.copy(alpha = (1f - exitDragProgress * 0.35f).coerceIn(0.6f, 1f)))
                    .graphicsLayer {
                        scaleX = activeScale
                        scaleY = activeScale
                        translationY = dragOffsetY.value * 0.65f
                        alpha = transitionAlpha.value
                    }
                    .clip(RoundedCornerShape(cornerDp))
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        if (b.width > 0f && b.height > 0f) {
                            onPipBoundsChanged(Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt()))
                        }
                    }
                    .pointerInput(state.isControlsLocked) {
                        if (!state.isControlsLocked) {
                            val tracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = { tracker.resetTracking() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    val next = (dragOffsetY.value + dragAmount).coerceIn(0f, 450f)
                                    scope.launch { dragOffsetY.snapTo(next) }
                                },
                                onDragEnd = {
                                    val velocityY = tracker.calculateVelocity().y
                                    scope.launch {
                                        if (dragOffsetY.value > fullscreenExitThresholdPx || velocityY > 360f) {
                                            dragOffsetY.animateTo(fullscreenExitThresholdPx * 2.2f, tween(130))
                                            dragOffsetY.snapTo(0f)
                                            onToggleFullscreen()
                                        } else {
                                            dragOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffsetY.animateTo(0f, spring()) }
                                }
                            )
                        }
                    }
            ) {
                ContentFrame(
                    player = player,
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
                YoutubePlayerOverlay(
                    state = state,
                    autoplayEnabled = autoplayEnabled,
                    showEngineStats = showEngineStats,
                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                    onToggleControls = onToggleControls,
                    onMinimize = onMinimize,
                    onEnterPip = onEnterPip,
                    onToggleAutoplay = onToggleAutoplay,
                    onToggleCaptions = onToggleCaptions,
                    onOpenSettings = { showSettingsSheet = true },
                    onOpenSpeedSheet = { showSpeedSheet = true },
                    onTogglePlayPause = onTogglePlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeekDeltaSeconds = onSeekDeltaSeconds,
                    onHoldSpeedStart = onHoldSpeedStart,
                    onHoldSpeedEnd = onHoldSpeedEnd,
                    onToggleLockScreen = onToggleLockScreen,
                    onToggleResizeMode = onToggleResizeMode,
                    onToggleFullscreen = onToggleFullscreen,
                    onScrubStart = onScrubStart,
                    onScrubChange = onScrubChange,
                    onScrubEnd = onScrubEnd
                )

                // Interactive slide-down-to-exit indicator pill
                androidx.compose.animation.AnimatedVisibility(
                    visible = dragOffsetY.value > 18f,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(YtBadgeDark)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FullscreenExit,
                            contentDescription = null,
                            tint = YtTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dragOffsetY.value > fullscreenExitThresholdPx) "Release to exit fullscreen" else "Slide down to exit fullscreen",
                            style = MaterialTheme.typography.labelMedium.copy(color = YtTextPrimary)
                        )
                    }
                }
            }
        }

        PlayerSheetMode.EXPANDED -> {
            val downProgress = (dragOffsetY.value.coerceAtLeast(0f) / (minimizeThresholdPx * 3.2f)).coerceIn(0f, 1f)
            val upProgress = ((-dragOffsetY.value).coerceAtLeast(0f) / (abs(fullscreenSwipeUpThresholdPx) * 2.2f)).coerceIn(0f, 1f)
            val sheetScale = transitionScale.value * (1f - (downProgress * 0.14f))
            val videoSwipeUpScale = 1f + (upProgress * 0.08f)
            val belowVideoAlpha = (1f - downProgress * 2.2f - upProgress * 1.4f).coerceIn(0f, 1f)

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragOffsetY.value.coerceAtLeast(0f).roundToInt()) }
                    .graphicsLayer {
                        scaleX = sheetScale
                        scaleY = sheetScale
                        alpha = transitionAlpha.value
                    }
                    .clip(RoundedCornerShape(topStart = (downProgress * 18f).dp, topEnd = (downProgress * 18f).dp))
                    .background(colors.background.copy(alpha = (1f - downProgress * 0.18f).coerceIn(0.65f, 1f)))
                    .statusBarsPadding()
            ) {
                // Top 16:9 YouTube Video Viewport — pull down slightly to minimize, swipe up slightly for animated Fullscreen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .graphicsLayer {
                            scaleX = videoSwipeUpScale
                            scaleY = videoSwipeUpScale
                            translationY = if (dragOffsetY.value < 0f) dragOffsetY.value * 0.25f else 0f
                        }
                        .background(YtTrueBlack)
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInWindow()
                            if (b.width > 0f && b.height > 0f) {
                                onPipBoundsChanged(Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt()))
                            }
                        }
                        .pointerInput(state.isControlsLocked) {
                            val tracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = { tracker.resetTracking() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    val next = (dragOffsetY.value + dragAmount).coerceIn(-180f, 750f)
                                    scope.launch { dragOffsetY.snapTo(next) }
                                },
                                onDragEnd = {
                                    val velocityY = tracker.calculateVelocity().y
                                    scope.launch {
                                        when {
                                            dragOffsetY.value > minimizeThresholdPx || velocityY > 360f -> {
                                                dragOffsetY.animateTo(minimizeThresholdPx * 2.5f, tween(120))
                                                dragOffsetY.snapTo(0f)
                                                onMinimize()
                                            }
                                            !state.isControlsLocked && (dragOffsetY.value < fullscreenSwipeUpThresholdPx || velocityY < -360f) -> {
                                                dragOffsetY.animateTo(fullscreenSwipeUpThresholdPx * 2f, tween(120))
                                                dragOffsetY.snapTo(0f)
                                                onToggleFullscreen()
                                            }
                                            else -> dragOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffsetY.animateTo(0f, spring()) }
                                }
                            )
                        }
                ) {
                    ContentFrame(
                        player = player,
                        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    YoutubePlayerOverlay(
                        state = state,
                        autoplayEnabled = autoplayEnabled,
                        showEngineStats = showEngineStats,
                        doubleTapSeekSeconds = doubleTapSeekSeconds,
                        onToggleControls = onToggleControls,
                        onMinimize = onMinimize,
                        onEnterPip = onEnterPip,
                        onToggleAutoplay = onToggleAutoplay,
                        onToggleCaptions = onToggleCaptions,
                        onOpenSettings = { showSettingsSheet = true },
                        onOpenSpeedSheet = { showSpeedSheet = true },
                        onTogglePlayPause = onTogglePlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSeekDeltaSeconds = onSeekDeltaSeconds,
                        onHoldSpeedStart = onHoldSpeedStart,
                        onHoldSpeedEnd = onHoldSpeedEnd,
                        onToggleLockScreen = onToggleLockScreen,
                        onToggleResizeMode = onToggleResizeMode,
                        onToggleFullscreen = onToggleFullscreen,
                        onScrubStart = onScrubStart,
                        onScrubChange = onScrubChange,
                        onScrubEnd = onScrubEnd
                    )

                    // Interactive slide-up-for-fullscreen hint pill
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !state.isControlsLocked && dragOffsetY.value < -18f,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(YtBadgeDark)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = null,
                                tint = YtTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (dragOffsetY.value < fullscreenSwipeUpThresholdPx) "Release for fullscreen" else "Slide up for fullscreen",
                                style = MaterialTheme.typography.labelMedium.copy(color = YtTextPrimary)
                            )
                        }
                    }
                }

                // YouTube Watch Page Below Video
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .alpha(belowVideoAlpha)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDescriptionSheet = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("watch_title_section")
                        ) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    lineHeight = 23.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val res = FormatUtils.formatResolutionLabel(
                                state.telemetry.videoWidth.takeIf { it > 0 } ?: video.width,
                                state.telemetry.videoHeight.takeIf { it > 0 } ?: video.height
                            )
                            val fps = (state.telemetry.videoFps.takeIf { it > 0f } ?: video.frameRate).toInt()
                            Text(
                                text = "${video.channelLabel} • $res ${fps}fps • ${FormatUtils.formatFileSize(video.fileSizeBytes)}   ...more",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }

                    // Channel / Engine Status Strip
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YtRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = video.channelLabel.firstOrNull()?.uppercase() ?: "V",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = YtTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.channelLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Media3 ExoPlayer • Sonic DSP",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.selectedChipBackground)
                                    .clickable { showSpeedSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("watch_speed_cta")
                            ) {
                                Text(
                                    text = "Speed ${FormatUtils.formatSpeed(state.playbackSpeed)}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = colors.selectedChipText,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                        }
                    }

                    // YouTube Horizontal Scrollable Action Pills Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            YoutubeActionPill(
                                icon = Icons.Filled.Speed,
                                label = FormatUtils.formatSpeed(state.playbackSpeed),
                                isActive = state.playbackSpeed != 1.0f,
                                onClick = { showSpeedSheet = true },
                                testTag = "pill_speed"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.PictureInPictureAlt,
                                label = "Floating",
                                isActive = false,
                                onClick = onEnterPip,
                                testTag = "pill_pip"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.Headphones,
                                label = "Background",
                                isActive = backgroundPlayEnabled,
                                onClick = onToggleBackgroundPlay,
                                testTag = "pill_background_play"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.GraphicEq,
                                label = if (state.pitchLockEnabled && state.pitchShift == 1.0f) "Pitch Lock" else "Pitch ${FormatUtils.formatPitch(state.pitchShift)}",
                                isActive = state.pitchShift != 1.0f || !state.pitchLockEnabled,
                                onClick = { showSpeedSheet = true },
                                testTag = "pill_pitch"
                            )
                            YoutubeActionPill(
                                icon = if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                label = if (video.isFavorite) "Liked" else "Like",
                                isActive = video.isFavorite,
                                onClick = { onToggleFavorite(video) },
                                testTag = "pill_like"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.RepeatOne,
                                label = "Loop",
                                isActive = state.isLooping,
                                onClick = onToggleLoop,
                                testTag = "pill_loop"
                            )
                            YoutubeActionPill(
                                icon = if (video.isWatchLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                label = "Save",
                                isActive = video.isWatchLater,
                                onClick = { onToggleWatchLater(video) },
                                testTag = "pill_save"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.ClosedCaption,
                                label = "Subtitles",
                                isActive = !video.subtitleUri.isNullOrBlank() && state.captionsEnabled,
                                onClick = onPickSubtitleFile,
                                testTag = "pill_subtitles"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.Lock,
                                label = "Lock",
                                isActive = false,
                                onClick = onToggleLockScreen,
                                testTag = "pill_lock"
                            )
                            YoutubeActionPill(
                                icon = Icons.Filled.Share,
                                label = "Share",
                                isActive = false,
                                onClick = {
                                    if (!video.isBundled) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = video.mimeType
                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(shareIntent, video.title))
                                        }
                                    }
                                },
                                testTag = "pill_share"
                            )
                        }
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(top = 4.dp))
                    }

                    // Up Next Header + Autoplay Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.up_next),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.autoplay),
                                    style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = autoplayEnabled,
                                    onCheckedChange = { onToggleAutoplay() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.background,
                                        checkedTrackColor = colors.accentBlue,
                                        uncheckedThumbColor = colors.textSecondary,
                                        uncheckedTrackColor = colors.chipElevated
                                    )
                                )
                            }
                        }
                    }

                    // Queue Video Cards
                    items(upNextVideos, key = { it.uri }) { queuedVideo ->
                        YoutubeVideoFeedCard(
                            video = queuedVideo,
                            onClick = { onSelectVideo(queuedVideo) },
                            onMoreClick = { onVideoMoreClick(queuedVideo) }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        YoutubeSettingsBottomSheet(
            playbackSpeed = state.playbackSpeed,
            pitchShift = state.pitchShift,
            pitchLockEnabled = state.pitchLockEnabled,
            isLooping = state.isLooping,
            captionsEnabled = state.captionsEnabled,
            hasSubtitleTrack = !video.subtitleUri.isNullOrBlank(),
            showEngineStats = showEngineStats,
            backgroundPlayEnabled = backgroundPlayEnabled,
            telemetry = state.telemetry,
            onDismiss = { showSettingsSheet = false },
            onOpenSpeedSheet = { showSpeedSheet = true },
            onEnterPip = onEnterPip,
            onToggleBackgroundPlay = onToggleBackgroundPlay,
            onToggleLoop = onToggleLoop,
            onToggleCaptions = onToggleCaptions,
            onPickSubtitleFile = onPickSubtitleFile,
            onToggleLockScreen = onToggleLockScreen,
            onToggleEngineStats = onToggleEngineStats
        )
    }

    if (showSpeedSheet) {
        YoutubeSpeedAndPitchSheet(
            playbackSpeed = state.playbackSpeed,
            pitchShift = state.pitchShift,
            pitchLockEnabled = state.pitchLockEnabled,
            telemetry = state.telemetry,
            onSpeedChange = onSpeedChange,
            onPitchChange = onPitchChange,
            onPitchLockToggle = onPitchLockToggle,
            onReset = onResetSpeedAndPitch,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showDescriptionSheet) {
        VideoDescriptionBottomSheet(
            video = video,
            telemetry = state.telemetry,
            onDismiss = { showDescriptionSheet = false }
        )
    }
}

@Composable
private fun YoutubeActionPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = VeloTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) colors.selectedChipBackground else colors.chipBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) colors.selectedChipText else colors.textPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (isActive) colors.selectedChipText else colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            )
        )
    }
}
