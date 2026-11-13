package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.YtBadgeDark
import com.example.ui.theme.YtProgressRed
import com.example.ui.theme.YtTextPrimary
import com.example.util.FormatUtils
import kotlin.math.roundToInt

@Composable
fun YoutubeScrubber(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    isControlsVisible: Boolean,
    isScrubbing: Boolean,
    previewBitmap: Bitmap?,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val playedFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val currentPlayedFraction by rememberUpdatedState(playedFraction)

    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var dragFraction by remember { mutableFloatStateOf(playedFraction) }
    var activeDrag by remember { mutableStateOf(false) }

    val displayFraction = if (activeDrag || isScrubbing) dragFraction else playedFraction
    val thumbRadius by animateDpAsState(
        targetValue = when {
            activeDrag || isScrubbing -> 9.dp
            isControlsVisible -> 6.dp
            else -> 0.dp
        },
        label = "scrubberThumbRadius"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (activeDrag || isScrubbing) 4.dp else if (isControlsVisible) 3.dp else 2.5.dp,
        label = "scrubberTrackHeight"
    )

    val density = LocalDensity.current
    val previewWidthPx = with(density) { 154.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        // Floating YouTube Filmstrip Preview Card above thumb during scrubbing
        if (activeDrag || isScrubbing) {
            val thumbX = displayFraction * trackWidthPx
            val horizontalMarginPx = with(density) { 8.dp.toPx() }
            val maxOffset = (trackWidthPx - previewWidthPx - horizontalMarginPx).coerceAtLeast(horizontalMarginPx)
            val clampedX = (thumbX - previewWidthPx / 2f).coerceIn(horizontalMarginPx, maxOffset)
            val previewMs = (displayFraction * safeDuration).toLong()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(clampedX.roundToInt(), with(density) { (-118).dp.roundToPx() }) }
                    .size(width = 154.dp, height = 110.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 154.dp, height = 87.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.5.dp, YtTextPrimary.copy(alpha = 0.92f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val safeBitmap = previewBitmap?.takeIf { !it.isRecycled }
                    if (safeBitmap != null) {
                        Image(
                            bitmap = safeBitmap.asImageBitmap(),
                            contentDescription = "Scrub preview frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Text(
                            text = FormatUtils.formatDuration(previewMs),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = YtTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(YtBadgeDark)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(previewMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = YtTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }

        // Relative horizontal scrubbing: dot stays where it is and moves smoothly left/right with drag delta (never jumps under finger!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.BottomCenter)
                .testTag("youtube_scrubber")
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            activeDrag = true
                            dragFraction = currentPlayedFraction
                            onScrubStart((dragFraction * safeDuration).toLong())
                        },
                        onDragEnd = {
                            val targetMs = (dragFraction * safeDuration).toLong()
                            activeDrag = false
                            onScrubEnd(targetMs)
                        },
                        onDragCancel = {
                            val targetMs = (dragFraction * safeDuration).toLong()
                            activeDrag = false
                            onScrubEnd(targetMs)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val deltaFraction = dragAmount / trackWidthPx
                            val nextFraction = (dragFraction + deltaFraction).coerceIn(0f, 1f)
                            dragFraction = nextFraction
                            onScrubChange((nextFraction * safeDuration).toLong())
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val centerY = size.height - 7.dp.toPx()
                val strokePx = trackHeight.toPx()
                val playedX = displayFraction * size.width
                val bufferedX = bufferedFraction * size.width

                // Background track
                drawLine(
                    color = Color(0x4DFFFFFF),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )

                // Buffered track
                if (bufferedX > 0f) {
                    drawLine(
                        color = Color(0x88FFFFFF),
                        start = Offset(0f, centerY),
                        end = Offset(bufferedX, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                // Played YouTube Red track
                if (playedX > 0f) {
                    drawLine(
                        color = YtProgressRed,
                        start = Offset(0f, centerY),
                        end = Offset(playedX, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                // Thumb dot
                val radiusPx = thumbRadius.toPx()
                if (radiusPx > 0.5f) {
                    if (activeDrag || isScrubbing) {
                        drawCircle(
                            color = YtProgressRed.copy(alpha = 0.28f),
                            radius = radiusPx * 1.85f,
                            center = Offset(playedX, centerY)
                        )
                    }
                    drawCircle(
                        color = YtProgressRed,
                        radius = radiusPx,
                        center = Offset(playedX, centerY)
                    )
                }
            }
        }
    }
}
