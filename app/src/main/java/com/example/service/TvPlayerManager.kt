package com.example.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.model.ChannelItem
import com.example.model.ProgramItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(UnstableApi::class)
object TvPlayerManager {
    private const val TAG = "TvPlayerManager"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var artworkJob: Job? = null

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer? get() = _exoPlayer

    private var _mediaSession: MediaSession? = null
    val mediaSession: MediaSession? get() = _mediaSession

    private var serviceRef: TvMediaPlaybackService? = null

    private val _currentChannel = MutableStateFlow<ChannelItem?>(null)
    val currentChannel: StateFlow<ChannelItem?> = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<ProgramItem?>(null)
    val currentProgram: StateFlow<ProgramItem?> = _currentProgram.asStateFlow()

    var channelList: List<ChannelItem> = emptyList()

    var onNextChannelCallback: (() -> Unit)? = null
    var onPreviousChannelCallback: (() -> Unit)? = null
    var onChannelSelectCallback: ((ChannelItem) -> Unit)? = null

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        _exoPlayer?.let { return it }
        val appContext = context.applicationContext
        val renderersFactory = DefaultRenderersFactory(appContext).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 (VMA-Live-TV)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 (VMA-Live-TV)",
                    "Accept" to "*/*"
                )
            )

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(appContext, httpDataSourceFactory)

        // Bộ trích xuất đa định dạng tối ưu cho: MP4, MOV, MKV, WebM, TS, FLV, MP3, AAC, WAV, OGG...
        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory().apply {
            setConstantBitrateSeekingEnabled(true)
        }

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
            .setLiveMaxOffsetMs(15000)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                try {
                    setWakeMode(C.WAKE_MODE_LOCAL)
                } catch (_: Throwable) {}
            }
        _exoPlayer = player
        try {
            setupMediaSession(appContext, player)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to setup MediaSession: ${e.message}")
        }
        return player
    }

    private fun setupMediaSession(context: Context, player: ExoPlayer) {
        if (_mediaSession != null) return

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "TV")
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build()
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        triggerNextChannel()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        triggerPreviousChannel()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_STOP -> {
                        _exoPlayer?.stop()
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            triggerNextChannel()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                        KeyEvent.KEYCODE_MEDIA_REWIND,
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            triggerPreviousChannel()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            _exoPlayer?.stop()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        val session = MediaSession.Builder(context, player)
            .setId("VmaLiveTvMediaSession")
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        _mediaSession = session
        serviceRef?.registerSession(session)
        startService(context)
    }

    fun onServiceCreated(service: TvMediaPlaybackService) {
        serviceRef = service
        _mediaSession?.let { session: MediaSession -> service.registerSession(session) }
    }

    fun onServiceDestroyed() {
        serviceRef = null
    }

    fun startService(context: Context) {
        try {
            val intent = Intent(context.applicationContext, TvMediaPlaybackService::class.java)
            // Use standard startService so Android does not trigger fatal 5-second ForegroundServiceDidNotStartInTimeException
            context.applicationContext.startService(intent)
        } catch (e: Throwable) {
            Log.w(TAG, "Notice: TvMediaPlaybackService start postponed: ${e.message}")
        }
    }

    fun stopService(context: Context) {
        try {
            val intent = Intent(context.applicationContext, TvMediaPlaybackService::class.java)
            context.applicationContext.stopService(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to stop TvMediaPlaybackService: ${e.message}")
        }
    }

    fun triggerNextChannel() {
        scope.launch(Dispatchers.Main) {
            if (onNextChannelCallback != null) {
                onNextChannelCallback?.invoke()
            } else {
                val list = channelList
                val current = _currentChannel.value
                if (list.isNotEmpty() && current != null) {
                    val idx = list.indexOfFirst { it.streamUrl == current.streamUrl }
                    val next = if (idx in 0 until list.lastIndex) list[idx + 1] else list.first()
                    onChannelSelectCallback?.invoke(next)
                }
            }
        }
    }

    fun triggerPreviousChannel() {
        scope.launch(Dispatchers.Main) {
            if (onPreviousChannelCallback != null) {
                onPreviousChannelCallback?.invoke()
            } else {
                val list = channelList
                val current = _currentChannel.value
                if (list.isNotEmpty() && current != null) {
                    val idx = list.indexOfFirst { it.streamUrl == current.streamUrl }
                    val prev = if (idx > 0) list[idx - 1] else list.last()
                    onChannelSelectCallback?.invoke(prev)
                }
            }
        }
    }

    fun playChannel(
        context: Context,
        channel: ChannelItem,
        streamUri: String,
        program: ProgramItem? = null,
        isCatchup: Boolean = false
    ) {
        try {
            val player = getOrCreatePlayer(context)
            _currentChannel.value = channel
            _currentProgram.value = program

            val rawUri = streamUri.trim()
            if (rawUri.isBlank()) {
                Log.w(TAG, "Cannot play empty streamUri for ${channel.channelName}")
                return
            }

            val parsedUri = try {
                Uri.parse(rawUri)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid URI: $rawUri", e)
                return
            }

            val metadata = buildMediaMetadata(channel, program, isCatchup, null)
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(parsedUri)
                .setMediaId(channel.tvgId.ifBlank { rawUri })
                .setMediaMetadata(metadata)

            val lower = rawUri.lowercase()
            when {
                // HLS (.m3u8, .m3u, hls, m3u8 playlist)
                lower.contains(".m3u8") || lower.contains("hls") || lower.contains("format=m3u8") || lower.contains("/live.m3u8") || lower.endsWith(".m3u") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                }
                // DASH (.mpd)
                lower.contains(".mpd") || lower.contains("dash") || lower.contains("manifest=mpd") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                }
                // SmoothStreaming (.ism, .isml)
                lower.contains(".ism") || lower.contains("smoothstreaming") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SS)
                }
                // RTSP Streams (rtsp://)
                lower.startsWith("rtsp://") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_RTSP)
                }
                // MP4 / M4V / M4A
                lower.contains(".mp4") || lower.contains(".m4v") || lower.contains(".m4a") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                }
                // QuickTime MOV (.mov, .qt)
                lower.contains(".mov") || lower.contains(".qt") -> {
                    mediaItemBuilder.setMimeType("video/quicktime")
                }
                // Matroska MKV (.mkv)
                lower.contains(".mkv") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MATROSKA)
                }
                // WebM (.webm)
                lower.contains(".webm") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_WEBM)
                }
                // MPEG-TS (.ts)
                lower.contains(".ts") || lower.contains("mpegts") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP2T)
                }
                // Flash Video FLV (.flv)
                lower.contains(".flv") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_FLV)
                }
                // Audio MP3
                lower.contains(".mp3") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_MPEG)
                }
                // Audio AAC
                lower.contains(".aac") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC)
                }
                // Audio OGG
                lower.contains(".ogg") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_OGG)
                }
                // Audio FLAC
                lower.contains(".flac") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_FLAC)
                }
                // Audio WAV
                lower.contains(".wav") -> {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_WAV)
                }
            }

            val mediaItem = mediaItemBuilder.build()

            // Stop and clear previous playback state cleanly before loading new channel
            try {
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(mediaItem)
                player.setPlaylistMetadata(metadata)
                player.prepare()
                player.play()
            } catch (playbackEx: Throwable) {
                Log.e(TAG, "Error in player prepare/play: ${playbackEx.message}", playbackEx)
            }

            // Kích hoạt dịch vụ phát nền & liên kết trình điều khiển đa phương tiện hệ thống (OS Media Controls)
            startService(context)

            // Fetch artwork in background and update metadata with raw bytes
            loadArtworkInBackground(context, channel, program, isCatchup)
        } catch (t: Throwable) {
            Log.e(TAG, "Error playing channel ${channel.channelName}: ${t.message}", t)
        }
    }

    fun updateMetadata(
        context: Context,
        channel: ChannelItem,
        program: ProgramItem? = null,
        isCatchup: Boolean = false
    ) {
        _currentChannel.value = channel
        _currentProgram.value = program

        val metadata = buildMediaMetadata(channel, program, isCatchup, null)
        _exoPlayer?.setPlaylistMetadata(metadata)
        loadArtworkInBackground(context, channel, program, isCatchup)
    }

    private fun buildMediaMetadata(
        channel: ChannelItem,
        program: ProgramItem?,
        isCatchup: Boolean,
        artworkBytes: ByteArray?
    ): MediaMetadata {
        val titleText = when {
            isCatchup && program != null -> "[Xem lại] ${program.title}"
            program != null && program.title.isNotBlank() -> program.title
            !channel.currentProgramTitle.isNullOrBlank() -> channel.currentProgramTitle!!
            else -> channel.channelName
        }

        val artistText = when {
            program != null || !channel.currentProgramTitle.isNullOrBlank() -> {
                "${channel.channelName} • ${channel.groupTitle.ifBlank { "VMA Live" }}"
            }
            channel.groupTitle.isNotBlank() -> channel.groupTitle
            else -> "Truyền hình trực tuyến VMA Live"
        }

        val descText = when {
            program != null && !program.description.isNullOrBlank() -> program.description!!
            isCatchup -> "Phát lại chương trình truyền hình"
            else -> "Đang phát trực tiếp trên ${channel.channelName}"
        }

        val logoUrl = when {
            !channel.tvgLogo.isNullOrBlank() -> channel.tvgLogo
            program?.thumbnailUrl?.isNotBlank() == true -> program.thumbnailUrl
            else -> null
        }

        val builder = MediaMetadata.Builder()
            .setTitle(titleText)
            .setDisplayTitle(titleText)
            .setArtist(artistText)
            .setSubtitle(channel.channelName)
            .setDescription(descText)
            .setMediaType(MediaMetadata.MEDIA_TYPE_TV_CHANNEL)
            .setIsPlayable(true)

        if (!logoUrl.isNullOrBlank()) {
            try {
                builder.setArtworkUri(Uri.parse(logoUrl))
            } catch (_: Exception) {}
        }

        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            builder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        return builder.build()
    }

    private fun loadArtworkInBackground(
        context: Context,
        channel: ChannelItem,
        program: ProgramItem?,
        isCatchup: Boolean
    ) {
        artworkJob?.cancel()
        val logoUrl = when {
            !channel.tvgLogo.isNullOrBlank() -> channel.tvgLogo
            program?.thumbnailUrl?.isNotBlank() == true -> program.thumbnailUrl
            else -> null
        } ?: return

        artworkJob = scope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context.applicationContext)
                val request = ImageRequest.Builder(context.applicationContext)
                    .data(logoUrl)
                    .allowHardware(false)
                    .build()
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                    val bytes = stream.toByteArray()
                    val updatedMetadata = buildMediaMetadata(channel, program, isCatchup, bytes)
                    launch(Dispatchers.Main) {
                        _exoPlayer?.let { p ->
                            p.setPlaylistMetadata(updatedMetadata)
                            val curIdx = p.currentMediaItemIndex
                            val curItem = p.currentMediaItem
                            if (curItem != null) {
                                val newItem = curItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                                p.replaceMediaItem(curIdx, newItem)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not prefetch channel artwork: ${e.message}")
            }
        }
    }

    fun release() {
        artworkJob?.cancel()
        _mediaSession?.release()
        _mediaSession = null
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
