package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.VideoEntity
import com.example.ui.PlayerSheetMode
import com.example.ui.VeloPlayViewModel
import com.example.ui.components.RenameVideoDialog
import com.example.ui.components.VideoOverflowBottomSheet
import com.example.ui.components.YoutubeWatchOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.VeloPlayTheme
import com.example.ui.theme.VeloTheme
import com.example.ui.theme.YtBlack
import com.example.ui.theme.YtRed
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_LIBRARY = "library"

class MainActivity : ComponentActivity() {

    private val viewModel: VeloPlayViewModel by viewModels()
    private val isInPipModeFlow = MutableStateFlow(false)
    private var videoSourceRect: Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingVideoIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val playerState by viewModel.playerUiState.collectAsStateWithLifecycle()
            val isInPipMode by isInPipModeFlow.collectAsStateWithLifecycle()

            val forceDarkBars = isInPipMode ||
                playerState.mode == PlayerSheetMode.EXPANDED ||
                playerState.mode == PlayerSheetMode.FULLSCREEN

            LaunchedEffect(
                playerState.mode,
                playerState.isPlaying,
                playerState.currentVideo?.width,
                playerState.currentVideo?.height,
                settings.autoPipEnabled
            ) {
                updatePipParams()
            }

            VeloPlayTheme(
                themeMode = settings.themeMode,
                forceDarkSystemBars = forceDarkBars
            ) {
                VeloPlayApp(
                    viewModel = viewModel,
                    isInPipMode = isInPipMode,
                    onEnterPip = { enterPipMode() },
                    onVideoBoundsChanged = { rect ->
                        videoSourceRect = rect
                        updatePipParams()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingVideoIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel.playerUiState.value
        val autoPip = viewModel.settings.value.autoPipEnabled
        if (autoPip && state.isPlaying &&
            (state.mode == PlayerSheetMode.EXPANDED || state.mode == PlayerSheetMode.FULLSCREEN)
        ) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeFlow.value = isInPictureInPictureMode
    }

    private fun buildPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val state = viewModel.playerUiState.value
        val rawW = (state.telemetry.videoWidth.takeIf { it > 0 } ?: state.currentVideo?.width ?: 16).coerceAtLeast(1)
        val rawH = (state.telemetry.videoHeight.takeIf { it > 0 } ?: state.currentVideo?.height ?: 9).coerceAtLeast(1)
        val ratioValue = rawW.toFloat() / rawH.toFloat()
        val aspectRatio = when {
            ratioValue > 2.35f -> Rational(235, 100)
            ratioValue < 0.45f -> Rational(45, 100)
            else -> Rational(rawW, rawH)
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)

        videoSourceRect?.let { rect ->
            if (!rect.isEmpty) builder.setSourceRectHint(rect)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val shouldAutoEnter = viewModel.settings.value.autoPipEnabled &&
                state.isPlaying &&
                (state.mode == PlayerSheetMode.EXPANDED || state.mode == PlayerSheetMode.FULLSCREEN)
            builder.setAutoEnterEnabled(shouldAutoEnter)
        }
        return builder.build()
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            buildPipParams()?.let { params ->
                runCatching { setPictureInPictureParams(params) }
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            val params = buildPipParams() ?: return
            runCatching { enterPictureInPictureMode(params) }
        }
    }

    private fun handleIncomingVideoIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                viewModel.openExternalUri(uri)
            }
        }
    }
}

private fun requiredVideoPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasVideoStoragePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, requiredVideoPermission()) == PackageManager.PERMISSION_GRANTED

@OptIn(UnstableApi::class)
@Composable
fun VeloPlayApp(
    viewModel: VeloPlayViewModel,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    onVideoBoundsChanged: (Rect) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val filteredVideos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val upNextVideos by viewModel.upNextVideos.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val playerState by viewModel.playerUiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()

    var menuTargetVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var renameTargetVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var requestedInitialPermissions by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val colors = VeloTheme.colors

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.syncDeviceVideos(hasVideoStoragePermission(context))
    }

    val requestStorageAndNotificationPermissions: () -> Unit = {
        val perms = buildList {
            add(requiredVideoPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Initial storage permission check & scan
    LaunchedEffect(Unit) {
        val granted = hasVideoStoragePermission(context)
        viewModel.syncDeviceVideos(granted)
        if (!granted && !requestedInitialPermissions) {
            requestedInitialPermissions = true
            requestStorageAndNotificationPermissions()
        }
    }

    // Lifecycle sync: scan new videos on resume; honor backgroundPlayEnabled on stop
    DisposableEffect(lifecycleOwner, isInPipMode, settings.backgroundPlayEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.syncDeviceVideos(hasVideoStoragePermission(context))
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!isInPipMode && !settings.backgroundPlayEnabled) {
                        viewModel.pausePlayer()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Sidecar Subtitle (.srt / .vtt) picker
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachSubtitleToCurrentVideo(uri)
        }
    }

    // When in Android Picture-in-Picture floating window, render ONLY full-bleed video surface
    if (isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ContentFrame(
                player = viewModel.playerManager.player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val isFullPlayerOverlay = playerState.mode == PlayerSheetMode.EXPANDED ||
        playerState.mode == PlayerSheetMode.FULLSCREEN

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isFullPlayerOverlay) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (playerState.mode == PlayerSheetMode.MINI) {
                            YoutubeWatchOverlay(
                                player = viewModel.playerManager.player,
                                state = playerState,
                                upNextVideos = upNextVideos,
                                autoplayEnabled = settings.autoplayNext,
                                showEngineStats = settings.showEngineStats,
                                backgroundPlayEnabled = settings.backgroundPlayEnabled,
                                doubleTapSeekSeconds = settings.doubleTapSeekSeconds,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onMinimize = viewModel::minimizePlayer,
                                onExpand = viewModel::expandPlayer,
                                onClose = viewModel::closePlayer,
                                onEnterPip = onEnterPip,
                                onToggleBackgroundPlay = {
                                    viewModel.setBackgroundPlayEnabled(!settings.backgroundPlayEnabled)
                                },
                                onToggleFullscreen = viewModel::toggleFullscreen,
                                onPipBoundsChanged = onVideoBoundsChanged,
                                onSelectVideo = { viewModel.openVideo(it) },
                                onVideoMoreClick = { menuTargetVideo = it },
                                onPrevious = viewModel::playPrevious,
                                onNext = viewModel::playNext,
                                onSeekDeltaSeconds = viewModel::seekBySeconds,
                                onHoldSpeedStart = viewModel::startHoldSpeedBoost,
                                onHoldSpeedEnd = viewModel::endHoldSpeedBoost,
                                onSpeedChange = viewModel::setPlaybackSpeed,
                                onPitchChange = viewModel::setPitchShift,
                                onPitchLockToggle = viewModel::setPitchLockEnabled,
                                onResetSpeedAndPitch = viewModel::resetSpeedAndPitch,
                                onToggleLoop = viewModel::toggleLooping,
                                onToggleCaptions = viewModel::toggleCaptions,
                                onPickSubtitleFile = {
                                    subtitlePickerLauncher.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*"))
                                },
                                onToggleLockScreen = viewModel::toggleControlsLocked,
                                onToggleControls = { viewModel.setControlsVisible(!playerState.isControlsVisible) },
                                onToggleResizeMode = viewModel::toggleResizeMode,
                                onToggleAutoplay = { viewModel.setAutoplayNext(!settings.autoplayNext) },
                                onToggleEngineStats = { viewModel.setShowEngineStats(!settings.showEngineStats) },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onToggleWatchLater = viewModel::toggleWatchLater,
                                onScrubStart = viewModel::onScrubStart,
                                onScrubChange = viewModel::onScrubChange,
                                onScrubEnd = viewModel::onScrubEnd
                            )
                        }

                        YoutubeBottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            videos = filteredVideos,
                            selectedFilter = selectedFilter,
                            isSearchExpanded = isSearchExpanded,
                            searchQuery = searchQuery,
                            showEngineStats = settings.showEngineStats,
                            themeMode = settings.themeMode,
                            hasStoragePermission = hasStoragePermission,
                            bottomContentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                            onSelectFilter = viewModel::selectFilter,
                            onSearchToggle = viewModel::setSearchExpanded,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onToggleEngineStats = { viewModel.setShowEngineStats(!settings.showEngineStats) },
                            onCycleThemeMode = viewModel::cycleThemeMode,
                            onRequestStoragePermission = requestStorageAndNotificationPermissions,
                            onVideoClick = { viewModel.openVideo(it, PlayerSheetMode.EXPANDED) },
                            onVideoMoreClick = { menuTargetVideo = it }
                        )
                    }

                    composable(ROUTE_LIBRARY) {
                        LibraryScreen(
                            allVideos = allVideos,
                            settings = settings,
                            hasStoragePermission = hasStoragePermission,
                            bottomContentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                            onVideoClick = { viewModel.openVideo(it, PlayerSheetMode.EXPANDED) },
                            onRequestStoragePermission = requestStorageAndNotificationPermissions,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onToggleBackgroundPlay = viewModel::setBackgroundPlayEnabled,
                            onToggleAutoPip = viewModel::setAutoPipEnabled,
                            onToggleAutoplay = viewModel::setAutoplayNext,
                            onToggleRememberSpeed = viewModel::setRememberSpeedPerVideo,
                            onToggleEngineStats = viewModel::setShowEngineStats,
                            onSelectDoubleTapSeek = viewModel::setDoubleTapSeekSeconds
                        )
                    }
                }

                // Expanded / Fullscreen YouTube Watch View Overlay
                if (isFullPlayerOverlay) {
                    YoutubeWatchOverlay(
                        player = viewModel.playerManager.player,
                        state = playerState,
                        upNextVideos = upNextVideos,
                        autoplayEnabled = settings.autoplayNext,
                        showEngineStats = settings.showEngineStats,
                        backgroundPlayEnabled = settings.backgroundPlayEnabled,
                        doubleTapSeekSeconds = settings.doubleTapSeekSeconds,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onMinimize = viewModel::minimizePlayer,
                        onExpand = viewModel::expandPlayer,
                        onClose = viewModel::closePlayer,
                        onEnterPip = onEnterPip,
                        onToggleBackgroundPlay = {
                            viewModel.setBackgroundPlayEnabled(!settings.backgroundPlayEnabled)
                        },
                        onToggleFullscreen = viewModel::toggleFullscreen,
                        onPipBoundsChanged = onVideoBoundsChanged,
                        onSelectVideo = { viewModel.openVideo(it, playerState.mode) },
                        onVideoMoreClick = { menuTargetVideo = it },
                        onPrevious = viewModel::playPrevious,
                        onNext = viewModel::playNext,
                        onSeekDeltaSeconds = viewModel::seekBySeconds,
                        onHoldSpeedStart = viewModel::startHoldSpeedBoost,
                        onHoldSpeedEnd = viewModel::endHoldSpeedBoost,
                        onSpeedChange = viewModel::setPlaybackSpeed,
                        onPitchChange = viewModel::setPitchShift,
                        onPitchLockToggle = viewModel::setPitchLockEnabled,
                        onResetSpeedAndPitch = viewModel::resetSpeedAndPitch,
                        onToggleLoop = viewModel::toggleLooping,
                        onToggleCaptions = viewModel::toggleCaptions,
                        onPickSubtitleFile = {
                            subtitlePickerLauncher.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*"))
                        },
                        onToggleLockScreen = viewModel::toggleControlsLocked,
                        onToggleControls = { viewModel.setControlsVisible(!playerState.isControlsVisible) },
                        onToggleResizeMode = viewModel::toggleResizeMode,
                        onToggleAutoplay = { viewModel.setAutoplayNext(!settings.autoplayNext) },
                        onToggleEngineStats = { viewModel.setShowEngineStats(!settings.showEngineStats) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleWatchLater = viewModel::toggleWatchLater,
                        onScrubStart = viewModel::onScrubStart,
                        onScrubChange = viewModel::onScrubChange,
                        onScrubEnd = viewModel::onScrubEnd,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Branded VeloPlay Logo Opening Animation
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(320))
        ) {
            VeloSplashOverlay(
                onFinished = { showSplash = false }
            )
        }
    }

    menuTargetVideo?.let { target ->
        VideoOverflowBottomSheet(
            video = target,
            onPlayNow = { viewModel.openVideo(target, PlayerSheetMode.EXPANDED) },
            onToggleFavorite = { viewModel.toggleFavorite(target) },
            onToggleWatchLater = { viewModel.toggleWatchLater(target) },
            onRenameRequest = { renameTargetVideo = target },
            onDelete = { viewModel.deleteVideo(target) },
            onDismiss = { menuTargetVideo = null }
        )
    }

    renameTargetVideo?.let { target ->
        RenameVideoDialog(
            currentTitle = target.title,
            onConfirm = { newTitle ->
                viewModel.renameVideo(target, newTitle)
                renameTargetVideo = null
            },
            onDismiss = { renameTargetVideo = null }
        )
    }
}

@Composable
private fun VeloSplashOverlay(
    onFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.62f) }
    val sweepProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            sweepProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(180L)
            titleAlpha.animateTo(1f, animationSpec = tween(360))
        }
        delay(950L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBlack),
        contentAlignment = Alignment.Center
    ) {
        // Animated crimson velocity arcs matching the app logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 28.dp.toPx())
            val p = sweepProgress.value
            val lineLen = size.width * 0.42f * p
            val leftStart = Offset(center.x - 90.dp.toPx() - lineLen, center.y)
            val leftEnd = Offset(center.x - 62.dp.toPx(), center.y)
            val rightStart = Offset(center.x + 62.dp.toPx(), center.y)
            val rightEnd = Offset(center.x + 90.dp.toPx() + lineLen, center.y)

            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, YtRed)),
                start = leftStart,
                end = leftEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.horizontalGradient(listOf(YtRed, Color.Transparent)),
                start = rightStart,
                end = rightEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .scale(logoScale.value)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art),
                    contentDescription = "VeloPlay App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(titleAlpha.value)
            ) {
                Text(
                    text = "VeloPlay",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = DisplayFontFamily,
                        color = YtTextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(YtRed)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "8X",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = YtTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "HIGH-SPEED MEDIA3 ENGINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = YtTextSecondary,
                    letterSpacing = 1.6.sp
                ),
                modifier = Modifier.alpha(titleAlpha.value)
            )
        }
    }
}

@Composable
private fun YoutubeBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val colors = VeloTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            YoutubeNavTab(
                label = stringResource(R.string.nav_home),
                icon = if (currentRoute == ROUTE_HOME) Icons.Filled.Home else Icons.Outlined.Home,
                selected = currentRoute == ROUTE_HOME,
                onClick = { onNavigate(ROUTE_HOME) },
                testTag = "nav_home"
            )
            YoutubeNavTab(
                label = stringResource(R.string.nav_library),
                icon = if (currentRoute == ROUTE_LIBRARY) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                selected = currentRoute == ROUTE_LIBRARY,
                onClick = { onNavigate(ROUTE_LIBRARY) },
                testTag = "nav_library"
            )
        }
    }
}

@Composable
private fun YoutubeNavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = VeloTheme.colors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) colors.textPrimary else colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
