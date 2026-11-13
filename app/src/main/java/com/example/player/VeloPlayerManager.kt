package com.example.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.example.data.VideoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import kotlin.math.roundToInt

data class EngineTelemetry(
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoFps: Float = 0f,
    val videoCodec: String = "Hardware MediaCodec",
    val audioCodec: String = "Sonic DSP Float",
    val droppedFrames: Int = 0,
    val errorMessage: String? = null
)

object VeloPlayerHolder {
    @Volatile
    private var instance: VeloPlayerManager? = null

    fun get(context: Context): VeloPlayerManager {
        return instance ?: synchronized(this) {
            instance ?: VeloPlayerManager(context.applicationContext).also { instance = it }
        }
    }
}

@OptIn(UnstableApi::class)
class VeloPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private val trackSelector = DefaultTrackSelector(appContext).apply {
        parameters = buildUponParameters()
            .setTunnelingEnabled(false)
            .build()
    }

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,
            60_000,
            150,
            300
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val renderersFactory = object : DefaultRenderersFactory(appContext) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(true)
                .setEnableAudioTrackPlaybackParams(false)
                .build()
        }

        override fun buildVideoRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            eventHandler: Handler,
            eventListener: VideoRendererEventListener,
            allowedVideoJoiningTimeMs: Long,
            out: ArrayList<Renderer>
        ) {
            val builder = MediaCodecVideoRenderer.Builder(context)
                .setCodecAdapterFactory(codecAdapterFactory)
                .setMediaCodecSelector(mediaCodecSelector)
                .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
                .setEnableDecoderFallback(enableDecoderFallback)
                .setEventHandler(eventHandler)
                .setEventListener(eventListener)
                .setMaxDroppedFramesToNotify(50)
                .setAssumedMinimumCodecOperatingRate(240f)
                .experimentalSetEnableMediaCodecBufferDecodeOnlyFlag(true)
                .setEnableDurationToProgressUs(true)

            out.add(HighSpeedMediaCodecVideoRenderer(builder))
        }
    }
        .forceEnableMediaCodecAsynchronousQueueing()
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

    val player: ExoPlayer = ExoPlayer.Builder(appContext, renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(10_000L)
        .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
        .setHandleAudioBecomingNoisy(true)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .build()

    private val _telemetry = MutableStateFlow(EngineTelemetry())
    val telemetry: StateFlow<EngineTelemetry> = _telemetry.asStateFlow()

    private var lastAppliedSpeed = 1.0f
    private var lastAppliedPitch = 1.0f

    init {
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                _telemetry.update { it.copy(videoCodec = decoderName) }
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                _telemetry.update {
                    it.copy(
                        videoWidth = format.width.coerceAtLeast(0),
                        videoHeight = format.height.coerceAtLeast(0),
                        videoFps = if (format.frameRate > 0f) format.frameRate else it.videoFps
                    )
                }
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                _telemetry.update { it.copy(droppedFrames = it.droppedFrames + droppedFrames) }
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                _telemetry.update { it.copy(audioCodec = "$decoderName + Sonic DSP") }
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime,
                error: PlaybackException
            ) {
                _telemetry.update { it.copy(errorMessage = error.localizedMessage ?: "Playback error") }
            }
        })
    }

    fun playPlaylist(
        videos: List<VideoEntity>,
        startIndex: Int,
        startPositionMs: Long,
        speed: Float,
        pitch: Float,
        pitchLock: Boolean,
        loop: Boolean
    ) {
        if (videos.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, videos.lastIndex)
        val active = videos[clampedIndex]

        _telemetry.update {
            EngineTelemetry(
                videoWidth = active.width,
                videoHeight = active.height,
                videoFps = active.frameRate,
                droppedFrames = 0,
                errorMessage = null
            )
        }

        val mediaItems = videos.map { buildMediaItem(it) }
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaItems(mediaItems, clampedIndex, startPositionMs.coerceAtLeast(0L))
        applySpeedAndPitch(speed, pitch, pitchLock, force = true)
        player.prepare()
        player.playWhenReady = true
    }

    fun playVideo(
        video: VideoEntity,
        startPositionMs: Long,
        speed: Float,
        pitch: Float,
        pitchLock: Boolean,
        loop: Boolean
    ) {
        playPlaylist(
            videos = listOf(video),
            startIndex = 0,
            startPositionMs = startPositionMs,
            speed = speed,
            pitch = pitch,
            pitchLock = pitchLock,
            loop = loop
        )
    }

    private fun buildMediaItem(video: VideoEntity): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.channelLabel)

        if (!video.thumbnailPath.isNullOrBlank()) {
            val thumbFile = File(video.thumbnailPath)
            if (thumbFile.exists()) {
                runCatching {
                    val bytes = thumbFile.readBytes()
                    metadataBuilder.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
        }

        val itemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(video.uri))
            .setMediaId(video.uri)
            .setMediaMetadata(metadataBuilder.build())

        if (!video.subtitleUri.isNullOrBlank()) {
            val subUri = Uri.parse(video.subtitleUri)
            val mime = if (video.subtitleUri.endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(mime)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            itemBuilder.setSubtitleConfigurations(listOf(subConfig))
        }

        return itemBuilder.build()
    }

    fun applySpeedAndPitch(
        speed: Float,
        pitch: Float,
        pitchLock: Boolean,
        force: Boolean = false
    ) {
        val quantizedSpeed = ((speed.coerceIn(0.25f, 8.0f) * 20f).roundToInt() / 20f)
        val rawPitch = if (pitchLock) pitch else (pitch * (1f + (quantizedSpeed - 1f) * 0.12f))
        val quantizedPitch = ((rawPitch.coerceIn(0.50f, 2.00f) * 20f).roundToInt() / 20f)

        if (!force && quantizedSpeed == lastAppliedSpeed && quantizedPitch == lastAppliedPitch) {
            return
        }
        lastAppliedSpeed = quantizedSpeed
        lastAppliedPitch = quantizedPitch
        player.playbackParameters = PlaybackParameters(quantizedSpeed, quantizedPitch)
    }

    fun setScrubbingMode(scrubbing: Boolean) {
        player.setSeekParameters(
            if (scrubbing) SeekParameters.CLOSEST_SYNC else SeekParameters.DEFAULT
        )
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        player.seekTo(positionMs.coerceIn(0L, duration))
    }

    fun seekBy(deltaMs: Long) {
        val current = player.currentPosition.coerceAtLeast(0L)
        seekTo(current + deltaMs)
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stopAndClear() {
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    fun setLooping(loop: Boolean) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun setCaptionsEnabled(enabled: Boolean) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    private class HighSpeedMediaCodecVideoRenderer(
        builder: Builder
    ) : MediaCodecVideoRenderer(builder) {

        override fun getCodecOperatingRateV23(
            targetPlaybackSpeed: Float,
            format: Format,
            streamFormats: Array<out Format>
        ): Float {
            val baseRate = super.getCodecOperatingRateV23(targetPlaybackSpeed, format, streamFormats)
            val fps = if (format.frameRate > 0f) format.frameRate else 30f
            val desiredRate = fps * targetPlaybackSpeed.coerceAtLeast(1f)
            return maxOf(baseRate, desiredRate.coerceAtMost(480f))
        }

        override fun shouldDropBuffersToKeyframe(
            earlyUs: Long,
            elapsedRealtimeUs: Long,
            isLastBuffer: Boolean
        ): Boolean {
            return earlyUs < -2_500_000L && !isLastBuffer
        }
    }
}
