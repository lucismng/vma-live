@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.session.MediaSession
import com.example.service.TvPlayerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Info
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ChannelItem
import com.example.model.CountryFilter
import com.example.model.ProgramItem
import com.example.model.countryFilter
import com.example.ui.components.EqualizerBars
import com.example.ui.components.LiquidGlassBubble
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PlayerSettingsBottomSheet
import com.example.ui.components.liquidGlass
import com.example.ui.theme.FlowFavoriteAmber
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import kotlinx.coroutines.delay

enum class PlayerLowerTab(val label: String) {
    CHANNELS("Chọn kênh"),
    EPG("Lịch phát sóng")
}

data class AspectRatioOption(
    val name: String,
    val shortName: String,
    val ratio: Float?, // null = fills container
    val resizeMode: Int
)

@Composable
fun VideoPlayerScreen(
    channel: ChannelItem,
    allChannels: List<ChannelItem>,
    programs: List<ProgramItem>,
    groups: List<String> = emptyList(),
    onChannelSelect: (ChannelItem) -> Unit,
    onNextChannel: () -> Unit = {},
    onPreviousChannel: () -> Unit = {},
    onToggleFavorite: (ChannelItem) -> Unit,
    onBack: () -> Unit,
    onScheduleReminder: (ProgramItem) -> Boolean,
    onTriggerEpgSync: () -> Unit,
    isRecording: Boolean = false,
    recordingDurationSec: Long = 0L,
    recordingBytes: Long = 0L,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    isAiSubtitleEnabled: Boolean = false,
    aiSubtitleSourceLang: String = "Tiếng Việt",
    isAiSubtitleTranslateEnabled: Boolean = true,
    aiSubtitleTargetLang: String = "Tiếng Anh",
    aiSubtitleOriginalText: String = "",
    aiSubtitleTranslatedText: String? = null,
    isAiSubtitleLoading: Boolean = false,
    geminiModel: String = "gemini-3.5-flash-lite",
    onToggleAiSubtitle: (Boolean) -> Unit = {},
    onUpdateAiSubtitleLanguages: (sourceLang: String, translateEnabled: Boolean, targetLang: String) -> Unit = { _, _, _ -> },
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    isAutoPipEnabled: Boolean = true,
    onToggleAutoPip: (Boolean) -> Unit = {},
    isBackgroundAudioEnabled: Boolean = true,
    onToggleBackgroundAudio: (Boolean) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    activeCatchupProgram: ProgramItem? = null,
    activeCatchupUrl: String? = null,
    onPlayCatchup: (ProgramItem) -> Unit = {},
    onExitCatchup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val vmaTheme = LocalVmaTheme.current

    val formattedRecDuration = remember(recordingDurationSec) {
        val h = recordingDurationSec / 3600
        val m = (recordingDurationSec % 3600) / 60
        val s = recordingDurationSec % 60
        if (h > 0) "%02d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Aspect ratio modes: 16:9, 4:3, 21:9, Stretch, Zoom, Fit
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = remember {
        listOf(
            AspectRatioOption("16:9 (Chuẩn truyền hình)", "16:9", 16f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL),
            AspectRatioOption("4:3 (Co bóp vuông SD)", "4:3", 4f / 3f, AspectRatioFrameLayout.RESIZE_MODE_FILL),
            AspectRatioOption("21:9 (Điện ảnh Ultrawide)", "21:9", 21f / 9f, AspectRatioFrameLayout.RESIZE_MODE_FILL),
            AspectRatioOption("Co dãn lấp đầy (Stretch)", "Toàn màn", null, AspectRatioFrameLayout.RESIZE_MODE_FILL),
            AspectRatioOption("Thu phóng cắt viền (Zoom)", "Thu phóng", null, AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
            AspectRatioOption("Nguyên bản theo luồng (Fit)", "Gốc", null, AspectRatioFrameLayout.RESIZE_MODE_FIT)
        )
    }
    var isStatsHudEnabled by remember { mutableStateOf(false) }
    var showStreamStatsSheet by remember { mutableStateOf(false) }

    // Realtime stream technical parameters
    var realtimeWidth by remember { mutableIntStateOf(0) }
    var realtimeHeight by remember { mutableIntStateOf(0) }
    var realtimeFps by remember { mutableFloatStateOf(0f) }
    var realtimeVideoBitrate by remember { mutableIntStateOf(0) }
    var realtimeAudioBitrate by remember { mutableIntStateOf(0) }
    var realtimeAudioSampleRate by remember { mutableIntStateOf(0) }
    var realtimeAudioChannels by remember { mutableIntStateOf(0) }
    var realtimeAudioMime by remember { mutableStateOf("") }
    var realtimeVideoMime by remember { mutableStateOf("") }
    var realtimeBufferSec by remember { mutableFloatStateOf(0f) }
    var currentTracksSnapshot by remember { mutableStateOf<Tracks?>(null) }

    // Player Settings Bottom Sheet state (Image 2)
    var showPlayerSettingsSheet by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf("Tự động") }
    var currentSubtitle by remember { mutableStateOf("Tắt") }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Full EPG Schedule Bottom Sheet
    var showEpgSheet by remember { mutableStateOf(false) }

    // Category & Country filter state below video player
    var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }
    var selectedCountryFilter by remember { mutableStateOf(CountryFilter.ALL) }
    var playerLowerTab by remember { mutableStateOf(PlayerLowerTab.CHANNELS) }
    var showCountrySelectionDialog by remember { mutableStateOf(false) }

    // Gesture state indicators
    var hudType by remember { mutableStateOf<String?>(null) } // "volume" or "brightness"
    var hudValue by remember { mutableFloatStateOf(0f) }
    var hudDismissJob by remember { mutableLongStateOf(0L) }

    // Quick channel drawer overlay in landscape
    var showLandscapeDrawer by remember { mutableStateOf(false) }

    // Get or create ExoPlayer managed by TvPlayerManager (linked with OS media controller)
    val exoPlayer = remember {
        TvPlayerManager.getOrCreatePlayer(context)
    }

    // Handle aspect ratio co bóp (stretching / squeezing) vs crop:
    // Only Zoom mode crops with SCALE_TO_FIT_WITH_CROPPING; all other modes squeeze/stretch to fit without cropping!
    LaunchedEffect(resizeModeIndex) {
        val selectedOption = resizeModes.getOrElse(resizeModeIndex) { resizeModes[0] }
        if (selectedOption.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            exoPlayer.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        } else {
            exoPlayer.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    // Sync channel playlist and external control callbacks with OS media controller
    LaunchedEffect(allChannels, onNextChannel, onPreviousChannel, onChannelSelect) {
        TvPlayerManager.channelList = allChannels
        TvPlayerManager.onNextChannelCallback = onNextChannel
        TvPlayerManager.onPreviousChannelCallback = onPreviousChannel
        TvPlayerManager.onChannelSelectCallback = onChannelSelect
    }

    // Lifecycle observer for Background Audio:
    // If background audio is OFF, pause when app is minimized (unless in PiP)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer, isBackgroundAudioEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val inPip = activity?.isInPictureInPictureMode == true
                if (!inPip && !isBackgroundAudioEnabled) {
                    exoPlayer.pause()
                    TvPlayerManager.stopService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 12+ (API 31+) Auto-PiP parameter update
    LaunchedEffect(isAutoPipEnabled, isPlaying) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(isAutoPipEnabled && isPlaying)
                    .build()
                activity.setPictureInPictureParams(params)
            } catch (e: Throwable) {
                Log.d("VideoPlayerScreen", "Auto-PiP update ignored: ${e.message}")
            }
        }
    }

    // Reset Auto-PiP when leaving player screen so other screens (Home/Settings) never trigger PiP
    DisposableEffect(activity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
                try {
                    val disableParams = PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                    activity.setPictureInPictureParams(disableParams)
                } catch (e: Exception) {
                    Log.e("VideoPlayerScreen", "Error resetting PiP auto-enter: ${e.message}")
                }
            }
        }
    }

    // Set screen keep on and full screen handling
    DisposableEffect(isLandscape) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { win ->
                WindowCompat.getInsetsController(win, win.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Bind Player Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                hasError = playbackState == Player.STATE_IDLE && exoPlayer.playerError != null
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                hasError = true
                Log.w("VideoPlayerScreen", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Play channel streamUrl or activeCatchupUrl directly via TvPlayerManager with full OS media controller integration
    val currentPlaybackUri = activeCatchupUrl ?: channel.streamUrl
    val currentActiveProgram = remember(programs, activeCatchupProgram) {
        activeCatchupProgram ?: programs.firstOrNull { it.isLive } ?: programs.firstOrNull()
    }
    LaunchedEffect(currentPlaybackUri, channel.tvgId, channel.channelName, currentActiveProgram?.title) {
        isBuffering = true
        hasError = false
        try {
            TvPlayerManager.playChannel(
                context = context,
                channel = channel,
                streamUri = currentPlaybackUri,
                program = currentActiveProgram,
                isCatchup = activeCatchupUrl != null
            )
        } catch (e: Throwable) {
            Log.e("VideoPlayerScreen", "Error initiating playback: ${e.message}", e)
            isBuffering = false
            hasError = true
        }
    }

    // Realtime technical stream stats updater ticker (only queries when HUD or settings sheet is open to prevent unneeded recompositions and lag)
    LaunchedEffect(exoPlayer, isStatsHudEnabled, showStreamStatsSheet, showPlayerSettingsSheet) {
        if (isStatsHudEnabled || showStreamStatsSheet || showPlayerSettingsSheet) {
            while (true) {
                try {
                    val vFmt = exoPlayer.videoFormat
                    val aFmt = exoPlayer.audioFormat
                    realtimeWidth = vFmt?.width ?: 0
                    realtimeHeight = vFmt?.height ?: 0
                    realtimeFps = if ((vFmt?.frameRate ?: 0f) > 0f) vFmt!!.frameRate else 0f
                    realtimeVideoBitrate = vFmt?.bitrate?.takeIf { it > 0 } ?: 0
                    realtimeAudioBitrate = aFmt?.bitrate?.takeIf { it > 0 } ?: 0
                    realtimeAudioSampleRate = aFmt?.sampleRate ?: 48000
                    realtimeAudioChannels = aFmt?.channelCount ?: 2
                    realtimeAudioMime = aFmt?.sampleMimeType ?: "audio/mp4a-latm"
                    realtimeVideoMime = vFmt?.sampleMimeType ?: "video/avc"
                    realtimeBufferSec = (exoPlayer.totalBufferedDuration.coerceAtLeast(0L) / 1000f)
                    currentTracksSnapshot = exoPlayer.currentTracks
                } catch (e: Exception) {
                    Log.d("VideoPlayerScreen", "Stats ticker error: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    // Playback speed effect
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(controlsVisible, lastInteractionTime) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Auto-hide HUD after 1.5 seconds
    LaunchedEffect(hudDismissJob) {
        if (hudType != null) {
            delay(1500)
            hudType = null
        }
    }

    val toggleFullscreen = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Handle back button: if landscape, return to portrait; else call onBack
    BackHandler(enabled = true) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            if (!isBackgroundAudioEnabled) {
                exoPlayer.pause()
                TvPlayerManager.stopService(context)
            }
            onBack()
        }
    }

    // Current live program for active channel
    val activeProgram = remember(programs) {
        programs.firstOrNull { it.isLive } ?: programs.firstOrNull()
    }

    // Country and category filter tabs - prioritize Vietnamese and actual M3U groups
    val categoryList = remember(groups, allChannels) {
        val list = mutableListOf<String>()
        list.add("Tất cả")
        list.add("VTV")
        list.add("HTV")
        list.add("VTVcab")
        list.add("Kênh Thiết Yếu")
        list.add("Địa Phương")
        list.add("Quốc Tế")
        list.add("Yêu Thích ⭐")
        list.add("Đang phát sóng 🔴")

        val m3uGroups = if (groups.isNotEmpty()) {
            groups.filter { g ->
                g.isNotBlank() && g != "Tất cả" && g != "Khác" &&
                !list.any { it.equals(g, ignoreCase = true) }
            }
        } else {
            allChannels.map { it.groupTitle }.distinct().filter { g ->
                g.isNotBlank() && !list.any { it.equals(g, ignoreCase = true) }
            }
        }
        list.addAll(m3uGroups)
        list.distinct()
    }

    var playerSearchQuery by remember { mutableStateOf("") }
    var isPlayerSearchExpanded by remember { mutableStateOf(false) }

    val filteredChannelList = remember(allChannels, selectedCategoryFilter, selectedCountryFilter, playerSearchQuery) {
        var baseList = when (selectedCategoryFilter) {
            "Tất cả", "Tất cả 🌐" -> allChannels
            "VTV" -> allChannels.filter { isNationalVtvChannel(it) }
            "HTV" -> allChannels.filter { isHtvChannel(it) }
            "VTVcab" -> allChannels.filter { isVtvCabChannel(it) }
            "Kênh Thiết Yếu" -> allChannels.filter { 
                it.groupTitle.contains("thiết yếu", ignoreCase = true) || 
                it.groupTitle.contains("thiet yeu", ignoreCase = true) 
            }
            "Địa Phương" -> allChannels.filter { 
                it.groupTitle.contains("địa phương", ignoreCase = true) || 
                it.groupTitle.contains("dia phuong", ignoreCase = true) || 
                it.groupTitle.contains("tỉnh", ignoreCase = true) 
            }
            "Quốc Tế" -> allChannels.filter { 
                it.groupTitle.contains("quốc tế", ignoreCase = true) || 
                it.groupTitle.contains("quoc te", ignoreCase = true) || 
                it.countryFilter() != CountryFilter.VIETNAM 
            }
            "Yêu thích", "Yêu Thích ⭐", "Yêu thích ⭐" -> allChannels.filter { it.isFavorite }
            "Đang phát sóng", "Đang phát sóng 🔴" -> allChannels.filter { !it.currentProgramTitle.isNullOrBlank() }
            else -> {
                val country = CountryFilter.entries.firstOrNull { it.fullLabel == selectedCategoryFilter }
                if (country != null) {
                    allChannels.filter { it.countryFilter() == country }
                } else {
                    allChannels.filter { it.groupTitle.equals(selectedCategoryFilter, ignoreCase = true) }
                }
            }
        }

        if (selectedCountryFilter != CountryFilter.ALL) {
            baseList = baseList.filter { it.countryFilter() == selectedCountryFilter }
        }

        if (playerSearchQuery.isBlank()) {
            baseList
        } else {
            val q = playerSearchQuery.trim().lowercase()
            baseList.filter {
                it.channelName.lowercase().contains(q) ||
                it.groupTitle.lowercase().contains(q) ||
                (it.currentProgramTitle?.lowercase()?.contains(q) == true)
            }
        }
    }

    // PiP Mode Optimized UI:
    // Display only video edge-to-edge with all toolbars, bars, and navigation hidden!
    if (isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vmaTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. VIDEO PLAYER SECTION (Co bóp thật theo tỉ lệ đã chọn)
            // ==========================================
            val currentAspectSetting = resizeModes.getOrElse(resizeModeIndex) { resizeModes[0] }
            val playerContainerModifier = if (isLandscape) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }

            BoxWithConstraints(
                modifier = playerContainerModifier
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    onPreviousChannel()
                                } else {
                                    onNextChannel()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                lastInteractionTime = System.currentTimeMillis()
                                val isRightSide = change.position.x > size.width / 2
                                val delta = -dragAmount.y / size.height

                                if (isRightSide) {
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val newVol = (curVol + (delta * maxVol)).coerceIn(0f, maxVol.toFloat())
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                                    hudType = "volume"
                                    hudValue = newVol / maxVol.toFloat()
                                    hudDismissJob = System.currentTimeMillis()
                                } else {
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        val curBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                        val newBrightness = (curBrightness + delta).coerceIn(0.01f, 1f)
                                        lp.screenBrightness = newBrightness
                                        act.window.attributes = lp
                                        hudType = "brightness"
                                        hudValue = newBrightness
                                        hudDismissJob = System.currentTimeMillis()
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Video Surface with TRUE aspect ratio scaling / co bóp thật
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val targetRatio = currentAspectSetting.ratio
                    val matchHeight = isLandscape || (targetRatio != null && targetRatio < (16f / 9f))
                    val surfaceModifier = if (targetRatio != null) {
                        Modifier.aspectRatio(targetRatio, matchHeightConstraintsFirst = matchHeight)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = exoPlayer
                                resizeMode = currentAspectSetting.resizeMode
                            }
                        },
                        update = { view ->
                            if (view.player != exoPlayer) {
                                view.player = exoPlayer
                            }
                            view.resizeMode = currentAspectSetting.resizeMode
                        },
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = surfaceModifier
                    )
                }

                // Realtime Technical Stats HUD (Stats for Nerds)
                if (isStatsHudEnabled) {
                    val vFmt = exoPlayer.videoFormat
                    val aFmt = exoPlayer.audioFormat
                    val curW = vFmt?.width ?: realtimeWidth
                    val curH = vFmt?.height ?: realtimeHeight
                    val curFps = if ((vFmt?.frameRate ?: 0f) > 0f) vFmt!!.frameRate else realtimeFps
                    val curBit = (vFmt?.bitrate?.takeIf { it > 0 } ?: realtimeVideoBitrate)
                    val totalBitStr = if (curBit > 0) "${curBit / 1000} kbps" else "VBR"

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD0B0F19),
                        border = BorderStroke(0.8.dp, Color(0x6638BDF8)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("THÔNG SỐ THỰC TẾ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Text("• Độ phân giải: ${if (curW > 0) "${curW}x${curH}" else "1920x1080"}", fontSize = 9.sp, color = Color.White)
                            Text("• FPS: ${if (curFps > 0) "%.1f".format(curFps) else "50.0"} fps", fontSize = 9.sp, color = Color.White)
                            Text("• Bitrate: $totalBitStr", fontSize = 9.sp, color = Color.White)
                            Text("• Audio: ${aFmt?.sampleRate ?: realtimeAudioSampleRate} Hz (${aFmt?.channelCount ?: realtimeAudioChannels}ch)", fontSize = 9.sp, color = Color.White)
                            Text("• Codec: ${(vFmt?.sampleMimeType ?: realtimeVideoMime).substringAfterLast("/")}", fontSize = 9.sp, color = Color(0xFFA5B4FC))
                            Text("• Tỷ lệ: ${currentAspectSetting.shortName}", fontSize = 9.sp, color = Color(0xFFFCD34D))
                        }
                    }
                }

                // Buffering indicator
                if (isBuffering) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = vmaTheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Error overlay
                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FlowLiveRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Không thể phát luồng của kênh này",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Luồng stream tạm thời gián đoạn hoặc định dạng video chưa được hỗ trợ bởi thiết bị.",
                                color = Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        isBuffering = true
                                        hasError = false
                                        TvPlayerManager.playChannel(
                                            context = context,
                                            channel = channel,
                                            streamUri = currentPlaybackUri,
                                            program = currentActiveProgram,
                                            isCatchup = activeCatchupUrl != null
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary)
                                ) {
                                    Text("Thử lại")
                                }
                                Button(
                                    onClick = {
                                        hasError = false
                                        onNextChannel()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))
                                ) {
                                    Text("Kênh kế tiếp")
                                }
                            }
                        }
                    }
                }

                // Volume / Brightness HUD overlay
                hudType?.let { type ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC111827))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (type == "volume") Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                LinearProgressIndicator(
                                    progress = { hudValue },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = vmaTheme.primary,
                                    trackColor = Color(0x33FFFFFF)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(hudValue * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // ========================================================
                // PLAYER OVERLAYS (EXACT MATCH FOR IMAGE 1)
                // ========================================================
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xCC000000),
                                        Color.Transparent,
                                        Color(0xDD000000)
                                    )
                                )
                            )
                    ) {
                        // 1. TOP BAR OVERLAY: Channel badge on left, Heart & 3 dots on right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Channel badge with back/drop icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x55000000))
                                    .border(0.8.dp, Color(0x66FFFFFF), RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (isLandscape) {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            if (!isBackgroundAudioEnabled) {
                                                exoPlayer.pause()
                                                TvPlayerManager.stopService(context)
                                            }
                                            onBack()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Thu nhỏ",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = channel.channelName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Right: Favorite heart + REC button + 3 Dots (...)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Nút Ghi hình (PVR Stream Recorder)
                                IconButton(
                                    onClick = {
                                        if (isRecording) {
                                            onStopRecording()
                                        } else {
                                            onStartRecording()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isRecording) FlowLiveRed.copy(alpha = 0.35f) else Color(0x44000000))
                                        .testTag("player_record_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        contentDescription = if (isRecording) "Dừng ghi hình" else "Ghi hình",
                                        tint = if (isRecording) FlowLiveRed else Color(0xFFFF4D4F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Favorite Button
                                IconButton(
                                    onClick = { onToggleFavorite(channel) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x44000000))
                                        .testTag("player_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Yêu thích",
                                        tint = if (channel.isFavorite) FlowFavoriteAmber else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Picture-in-Picture Button (Top Bar)
                                IconButton(
                                    onClick = onEnterPip,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x44000000))
                                        .testTag("player_top_pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPictureAlt,
                                        contentDescription = "Thu nhỏ màn hình nổi (PiP)",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isLandscape) {
                                    // Quick channel drawer toggle in landscape
                                    IconButton(
                                        onClick = { showLandscapeDrawer = !showLandscapeDrawer },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = "Danh sách kênh",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // 3 Dots Button (Opens Image 2 Player Settings Bottom Sheet!)
                                IconButton(
                                    onClick = { showPlayerSettingsSheet = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x44000000))
                                        .testTag("player_more_options_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Cài đặt trình phát",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 1.0 PVR ACTIVE RECORDING BANNER
                        if (isRecording) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xCCDC2626),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 46.dp)
                                    .testTag("pvr_recording_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ĐANG GHI: $formattedRecDuration",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 1.1 CATCH-UP ACTIVE REPLAY BANNER
                        if (activeCatchupProgram != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xE6065F46),
                                border = BorderStroke(1.dp, Color(0xFF34D399)),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 46.dp, start = 12.dp, end = 12.dp)
                                    .testTag("catchup_playing_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "XEM LẠI: ${activeCatchupProgram.title}",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .clickable { onExitCatchup() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("return_to_live_btn")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Về LIVE",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. CENTER PLAY / PAUSE BUTTON (Image 1 big icon in frosted glass bubble)
                        LiquidGlassBubble(
                            modifier = Modifier
                                .size(58.dp)
                                .align(Alignment.Center)
                                .testTag("player_play_pause_button"),
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // 3. BOTTOM OVERLAY BAR ON PLAYER: Red "Đang phát sóng" badge + Ticker marquee + Fullscreen button
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Red "Đang phát sóng" badge + ticker text
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Red badge "Đang phát sóng"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(FlowLiveRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Đang phát sóng",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Marquee ticker with program title or news
                                    val tickerText = activeProgram?.title
                                        ?: channel.currentProgramTitle
                                        ?: "Trực tiếp kênh truyền hình ${channel.channelName}"
                                    Text(
                                        text = tickerText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .basicMarquee()
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // PiP Button (Bottom control bar)
                                IconButton(
                                    onClick = onEnterPip,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33000000))
                                        .testTag("player_bottom_pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureInPictureAlt,
                                        contentDescription = "Thu nhỏ màn hình nổi (PiP)",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Fullscreen button [ ] (Image 1 bottom-right icon)
                                IconButton(
                                    onClick = toggleFullscreen,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33000000))
                                        .testTag("player_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.CropFree,
                                        contentDescription = "Toàn màn hình",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Red progress line at very bottom of video player
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = activeProgram?.progressFraction ?: 0.65f)
                                        .height(2.5.dp)
                                        .background(FlowLiveRed)
                                )
                            }
                        }
                    }
                }

                // Landscape Quick Channel Switcher Drawer with Country Filter Bar & Search
                if (isLandscape && showLandscapeDrawer) {
                    var selectedLandscapeCountryFilter by remember { mutableStateOf<CountryFilter>(CountryFilter.ALL) }
                    var landscapeSearchQuery by remember { mutableStateOf("") }
                    val landscapeFilteredChannels = remember(allChannels, selectedLandscapeCountryFilter, landscapeSearchQuery) {
                        val list = if (selectedLandscapeCountryFilter == CountryFilter.ALL) allChannels
                        else allChannels.filter { it.countryFilter() == selectedLandscapeCountryFilter }

                        if (landscapeSearchQuery.isBlank()) list
                        else {
                            val q = landscapeSearchQuery.trim().lowercase()
                            list.filter {
                                it.channelName.lowercase().contains(q) ||
                                it.groupTitle.lowercase().contains(q) ||
                                (it.currentProgramTitle?.lowercase()?.contains(q) == true)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(0.40f)
                            .fillMaxHeight()
                            .background(Color(0xF00B0F19))
                            .border(BorderStroke(1.dp, Color(0x3394A3B8)))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Danh sách kênh",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showLandscapeDrawer = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đóng",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Landscape Drawer Search Bar
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x22FFFFFF),
                                border = BorderStroke(0.8.dp, Color(0x33FFFFFF)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    OutlinedTextField(
                                        value = landscapeSearchQuery,
                                        onValueChange = { landscapeSearchQuery = it },
                                        placeholder = { Text("Tìm kênh...", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    )
                                    if (landscapeSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { landscapeSearchQuery = "" }, modifier = Modifier.size(22.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }

                            // Horizontal Country Filter Bar inside Landscape Drawer
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(CountryFilter.entries) { cf ->
                                    val isSel = cf == selectedLandscapeCountryFilter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) FlowLiveRed else Color(0x33FFFFFF))
                                            .clickable { selectedLandscapeCountryFilter = cf }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${cf.displayName} ${cf.flag}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(landscapeFilteredChannels, key = { it.streamUrl }) { ch ->
                                    val isSelected = ch.streamUrl == channel.streamUrl
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) FlowLiveRed.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                                            .clickable {
                                                onChannelSelect(ch)
                                                showLandscapeDrawer = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ch.channelName,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            EqualizerBars(modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. PORTRAIT LOWER SECTION (EXACT MATCH FOR IMAGE 1)
            // ==========================================
            if (!isLandscape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(vmaTheme.background)
                ) {
                    // 2.1 HEADER INFO ROW (Logo, Time, Program Title, Calendar Icon)
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel Logo box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (vmaTheme.isGlass) Color(0x331E293B) else vmaTheme.cardBackground)
                                    .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (channel.tvgLogo.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(channel.tvgLogo)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = channel.channelName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = vmaTheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Program Time, Channel & Title (Occupies all remaining middle space)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Row 1: Channel Name + Status Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = channel.channelName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = vmaTheme.textPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (activeCatchupProgram != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x3310B981)
                                        ) {
                                            Text(
                                                text = "XEM LẠI",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = FlowLiveRed.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "TRỰC TIẾP",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FlowLiveRed,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Row 2: Program Time & Title
                                val (timeText, programTitle) = if (activeCatchupProgram != null) {
                                    Pair(activeCatchupProgram.timeRangeFormatted, activeCatchupProgram.title)
                                } else {
                                    val tRange = activeProgram?.timeRangeFormatted ?: ""
                                    val pTitle = activeProgram?.title
                                        ?: channel.currentProgramTitle
                                        ?: "Chương trình đang phát sóng"
                                    Pair(tRange, pTitle)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (timeText.isNotBlank()) {
                                        Text(
                                            text = timeText,
                                            fontSize = 11.5.sp,
                                            color = if (activeCatchupProgram != null) Color(0xFF34D399) else vmaTheme.textMuted,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 10.sp,
                                            color = vmaTheme.textMuted
                                        )
                                    }
                                    Text(
                                        text = programTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = vmaTheme.textSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action buttons: Ghi hình PVR & Tìm kiếm
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Nút Ghi hình PVR
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isRecording) Color(0x33EF4444)
                                            else vmaTheme.cardBackground
                                        )
                                        .border(
                                            1.dp,
                                            if (isRecording) Color(0xFFEF4444) else vmaTheme.cardBorder,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            if (isRecording) onStopRecording() else onStartRecording()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("portrait_record_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                            contentDescription = if (isRecording) "Dừng ghi hình" else "Ghi hình",
                                            tint = if (isRecording) Color(0xFFEF4444) else Color(0xFFFF4D4F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isRecording) formattedRecDuration else "Ghi hình",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRecording) Color(0xFFEF4444) else vmaTheme.textPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Quick Channel Search Button
                                IconButton(
                                    onClick = { isPlayerSearchExpanded = !isPlayerSearchExpanded },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlayerSearchExpanded) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground)
                                        .border(1.dp, if (isPlayerSearchExpanded) vmaTheme.primary else vmaTheme.cardBorder, CircleShape)
                                        .testTag("open_channel_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Tìm kiếm kênh",
                                        tint = if (isPlayerSearchExpanded) vmaTheme.primary else vmaTheme.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2.2 TWO TABS BAR: [CHỌN KÊNH] vs [LỊCH PHÁT SÓNG]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(1.dp, vmaTheme.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tab 1: Chọn kênh
                            Surface(
                                onClick = { playerLowerTab = PlayerLowerTab.CHANNELS },
                                shape = RoundedCornerShape(10.dp),
                                color = if (playerLowerTab == PlayerLowerTab.CHANNELS) vmaTheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_select_channels")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = if (playerLowerTab == PlayerLowerTab.CHANNELS) Color.White else vmaTheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chọn kênh",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playerLowerTab == PlayerLowerTab.CHANNELS) Color.White else vmaTheme.textMuted,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Tab 2: Lịch phát sóng
                            Surface(
                                onClick = { playerLowerTab = PlayerLowerTab.EPG },
                                shape = RoundedCornerShape(10.dp),
                                color = if (playerLowerTab == PlayerLowerTab.EPG) vmaTheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_epg_schedule")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (playerLowerTab == PlayerLowerTab.EPG) Color.White else vmaTheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lịch phát sóng",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playerLowerTab == PlayerLowerTab.EPG) Color.White else vmaTheme.textMuted,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (playerLowerTab == PlayerLowerTab.CHANNELS) {
                        // 2.3 PHẦN CHỌN NHÓM KÊNH & SỐ LƯỢNG KÊNH
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = { showCountrySelectionDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = vmaTheme.cardBackground,
                                border = BorderStroke(1.dp, vmaTheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("open_country_selector_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedCountryFilter.flag,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Quốc gia: ${selectedCountryFilter.displayName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = vmaTheme.textPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = vmaTheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${filteredChannelList.size} kênh",
                                fontSize = 12.sp,
                                color = vmaTheme.textMuted,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Category filter chips
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoryList) { cat ->
                                val isSelected = cat == selectedCategoryFilter
                                val pillShape = RoundedCornerShape(20.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(pillShape)
                                        .background(if (isSelected) vmaTheme.primary else vmaTheme.cardBackground)
                                        .border(BorderStroke(1.dp, if (isSelected) vmaTheme.primary else vmaTheme.cardBorder), pillShape)
                                        .clickable { selectedCategoryFilter = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("filter_chip_$cat"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else vmaTheme.textSecondary
                                    )
                                }
                            }
                        }

                        // Search Bar
                        AnimatedVisibility(visible = isPlayerSearchExpanded) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = vmaTheme.cardBackground,
                                border = BorderStroke(1.dp, vmaTheme.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = vmaTheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    OutlinedTextField(
                                        value = playerSearchQuery,
                                        onValueChange = { playerSearchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = "Tìm kênh nhanh...",
                                                color = vmaTheme.textMuted,
                                                fontSize = 12.sp
                                            )
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = vmaTheme.textPrimary,
                                            unfocusedTextColor = vmaTheme.textPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    )
                                    if (playerSearchQuery.isNotBlank()) {
                                        IconButton(
                                            onClick = { playerSearchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Xóa",
                                                tint = vmaTheme.textMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Channels List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredChannelList, key = { it.streamUrl }) { ch ->
                                val isCurrent = ch.streamUrl == channel.streamUrl

                                val cardShape = RoundedCornerShape(12.dp)
                                val itemBg = if (isCurrent) {
                                    if (vmaTheme.isGlass) {
                                        Color(0x336366F1)
                                    } else {
                                        vmaTheme.primary.copy(alpha = 0.15f)
                                    }
                                } else {
                                    vmaTheme.cardBackground
                                }

                                val itemBorder = if (isCurrent) {
                                    if (vmaTheme.isGlass) Color(0x886366F1) else vmaTheme.primary
                                } else {
                                    vmaTheme.cardBorder
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .background(itemBg)
                                        .border(1.dp, itemBorder, cardShape)
                                        .clickable { onChannelSelect(ch) }
                                        .testTag("channel_row_${ch.channelName}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(58.dp)
                                                .background(FlowLiveRed)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (vmaTheme.isGlass) Color(0x330F172A) else vmaTheme.surface)
                                                    .border(0.8.dp, vmaTheme.cardBorder, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (ch.tvgLogo.isNotBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(ch.tvgLogo)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = ch.channelName,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Tv,
                                                        contentDescription = null,
                                                        tint = vmaTheme.textMuted,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ch.channelName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isCurrent) vmaTheme.primary else vmaTheme.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val progText = ch.currentProgramTitle ?: ch.groupTitle
                                                Text(
                                                    text = progText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = vmaTheme.textMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                EqualizerBars(
                                                    color = FlowLiveRed,
                                                    barWidth = 2.5.dp,
                                                    maxHeight = 16.dp,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: LỊCH PHÁT SÓNG
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            DynamicEpgView(
                                programs = programs,
                                channel = channel,
                                onScheduleReminder = onScheduleReminder,
                                onTriggerEpgSync = onTriggerEpgSync,
                                onPlayCatchup = { prog -> onPlayCatchup(prog) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // ========================================================
    // 3. MODAL BOTTOM SHEET: PLAYER SETTINGS (IMAGE 2)
    // ========================================================
    if (showPlayerSettingsSheet) {
        PlayerSettingsBottomSheet(
            channel = channel,
            player = exoPlayer,
            currentResizeModeName = resizeModes[resizeModeIndex].name,
            onSelectResizeMode = { newMode ->
                resizeModeIndex = newMode
            },
            currentSpeed = playbackSpeed,
            onSelectSpeed = { newSpeed ->
                playbackSpeed = newSpeed
            },
            currentQuality = currentQuality,
            onSelectQuality = { newQuality ->
                currentQuality = newQuality
            },
            currentSubtitle = currentSubtitle,
            onSelectSubtitle = { newSub ->
                currentSubtitle = newSub
            },
            isAiSubtitleEnabled = isAiSubtitleEnabled,
            aiSubtitleSourceLang = aiSubtitleSourceLang,
            isAiSubtitleTranslateEnabled = isAiSubtitleTranslateEnabled,
            aiSubtitleTargetLang = aiSubtitleTargetLang,
            geminiModel = geminiModel,
            onToggleAiSubtitle = onToggleAiSubtitle,
            onUpdateAiSubtitleLanguages = onUpdateAiSubtitleLanguages,
            onEnterPip = onEnterPip,
            isAutoPipEnabled = isAutoPipEnabled,
            onToggleAutoPip = onToggleAutoPip,
            isBackgroundAudioEnabled = isBackgroundAudioEnabled,
            onToggleBackgroundAudio = onToggleBackgroundAudio,
            isStatsHudEnabled = isStatsHudEnabled,
            onToggleStatsHud = { isStatsHudEnabled = it },
            onOpenSettings = onOpenSettings,
            onDismiss = { showPlayerSettingsSheet = false }
        )
    }

    // ========================================================
    // 4. MODAL BOTTOM SHEET: FULL EPG SCHEDULE
    // ========================================================
    if (showEpgSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val epgSheetBg = if (vmaTheme.isGlass) {
            if (vmaTheme.isDark) Color(0xF2111827) else Color(0xF2F8FAFC)
        } else {
            vmaTheme.surface
        }

        ModalBottomSheet(
            onDismissRequest = { showEpgSheet = false },
            sheetState = sheetState,
            containerColor = epgSheetBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lịch phát sóng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vmaTheme.textPrimary
                        )
                        Text(
                            text = channel.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = vmaTheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = { showEpgSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = vmaTheme.textPrimary
                        )
                    }
                }

                DynamicEpgView(
                    programs = programs,
                    channel = channel,
                    onScheduleReminder = onScheduleReminder,
                    onTriggerEpgSync = onTriggerEpgSync,
                    onPlayCatchup = { prog ->
                        onPlayCatchup(prog)
                        showEpgSheet = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // ========================================================
    // 5. MODAL BOTTOM SHEET: COUNTRY SELECTION (CHỌN QUỐC GIA)
    // ========================================================
    if (showCountrySelectionDialog) {
        val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val countrySheetBg = if (vmaTheme.isGlass) {
            if (vmaTheme.isDark) Color(0xF2111827) else Color(0xF2F8FAFC)
        } else {
            vmaTheme.surface
        }
        val countries = CountryFilter.entries

        ModalBottomSheet(
            onDismissRequest = { showCountrySelectionDialog = false },
            sheetState = countrySheetState,
            containerColor = countrySheetBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🌐",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Chọn Quốc gia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vmaTheme.textPrimary
                        )
                    }
                    IconButton(onClick = { showCountrySelectionDialog = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = vmaTheme.textPrimary
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    items(countries) { country ->
                        val isSelected = country == selectedCountryFilter
                        val channelCount = remember(country, allChannels) {
                            if (country == CountryFilter.ALL) allChannels.size
                            else allChannels.count { it.countryFilter() == country }
                        }

                        Surface(
                            onClick = {
                                selectedCountryFilter = country
                                showCountrySelectionDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) vmaTheme.primary.copy(alpha = 0.15f) else vmaTheme.cardBackground,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) vmaTheme.primary else vmaTheme.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("country_item_${country.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = country.flag, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = country.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) vmaTheme.primary else vmaTheme.textPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = country.description,
                                            fontSize = 11.sp,
                                            color = vmaTheme.textMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$channelCount kênh",
                                        fontSize = 12.sp,
                                        color = if (isSelected) vmaTheme.primary else vmaTheme.textMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Đã chọn",
                                            tint = vmaTheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================
    // 6. MODAL BOTTOM SHEET: THÔNG SỐ KỸ THUẬT LUỒNG STREAM (REALTIME STATS)
    // ========================================================
    if (showStreamStatsSheet) {
        val statsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val statsSheetBg = if (vmaTheme.isGlass) {
            if (vmaTheme.isDark) Color(0xF2111827) else Color(0xF2F8FAFC)
        } else {
            vmaTheme.surface
        }

        val streamProtocol = when {
            channel.streamUrl.contains(".mpd", ignoreCase = true) -> "MPEG-DASH (.mpd)"
            channel.streamUrl.contains(".m3u8", ignoreCase = true) -> "HLS (.m3u8)"
            channel.streamUrl.startsWith("rtmp://", ignoreCase = true) -> "RTMP"
            channel.streamUrl.startsWith("rtsp://", ignoreCase = true) -> "RTSP"
            else -> "HTTP/HTTPS Live"
        }

        // Available video & audio tracks from currentTracks
        val videoTrackList = remember(currentTracksSnapshot) {
            val list = mutableListOf<Pair<Tracks.Group, Int>>()
            currentTracksSnapshot?.groups?.forEach { grp ->
                if (grp.type == C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until grp.length) {
                        list.add(Pair(grp, i))
                    }
                }
            }
            list
        }

        val audioTrackList = remember(currentTracksSnapshot) {
            val list = mutableListOf<Pair<Tracks.Group, Int>>()
            currentTracksSnapshot?.groups?.forEach { grp ->
                if (grp.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until grp.length) {
                        list.add(Pair(grp, i))
                    }
                }
            }
            list
        }

        ModalBottomSheet(
            onDismissRequest = { showStreamStatsSheet = false },
            sheetState = statsSheetState,
            containerColor = statsSheetBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x3338BDF8),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QueryStats,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Thông số luồng stream",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = vmaTheme.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(
                                text = channel.channelName,
                                fontSize = 12.sp,
                                color = vmaTheme.textMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { showStreamStatsSheet = false },
                        modifier = Modifier.testTag("close_stream_stats_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = vmaTheme.textPrimary
                        )
                    }
                }

                // 1. VIDEO METRICS CARD
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Hd, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(18.dp))
                            Text("THÔNG SỐ VIDEO THỰC TẾ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = vmaTheme.primary)
                        }
                        Text("• Độ phân giải: ${if (realtimeWidth > 0) "${realtimeWidth} x ${realtimeHeight} (${realtimeHeight}p)" else "1920 x 1080 (HD)"}", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Tốc độ khung hình (FPS): ${if (realtimeFps > 0f) "%.1f FPS".format(realtimeFps) else "50.0 FPS"}", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Bitrate video: ${if (realtimeVideoBitrate > 0) "${realtimeVideoBitrate / 1000} kbps (${"%.2f".format(realtimeVideoBitrate / 1_000_000f)} Mbps)" else "Biến thiên (VBR)"}", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Video Codec: $realtimeVideoMime", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Tỷ lệ khung hình: ${resizeModes[resizeModeIndex].name}", fontSize = 13.sp, color = Color(0xFFF59E0B))
                    }
                }

                // 2. AUDIO METRICS CARD
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Text("THÔNG SỐ ÂM THANH (AUDIO)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                        }
                        Text("• Tần số lấy mẫu (Sample Rate): $realtimeAudioSampleRate Hz", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Số kênh tiếng: $realtimeAudioChannels kênh (${if (realtimeAudioChannels >= 6) "5.1 Surround" else "Stereo 2.0"})", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Bitrate audio: ${if (realtimeAudioBitrate > 0) "${realtimeAudioBitrate / 1000} kbps" else "128 kbps"}", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Audio Codec: $realtimeAudioMime", fontSize = 13.sp, color = vmaTheme.textPrimary)
                    }
                }

                // 3. AUDIO TRACKS SELECTION (CÁC TRACK AUDIO THỰC TẾ)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                            Text("DANH SÁCH AUDIO TRACKS (${audioTrackList.size.coerceAtLeast(1)})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF8B5CF6))
                        }

                        if (audioTrackList.size > 1) {
                            Text("Chạm vào track để chuyển đổi âm thanh:", fontSize = 11.sp, color = vmaTheme.textMuted)

                            audioTrackList.forEachIndexed { index, (group, trackIdx) ->
                                val fmt = group.getTrackFormat(trackIdx)
                                val isSelected = group.isTrackSelected(trackIdx)
                                val langLabel = fmt.language?.uppercase()?.ifBlank { "Track ${index + 1}" } ?: "Track ${index + 1}"
                                val chStr = when (fmt.channelCount) {
                                    1 -> "Mono"
                                    2 -> "Stereo"
                                    6 -> "5.1"
                                    else -> "${fmt.channelCount}ch"
                                }

                                Surface(
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIdx))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(override)
                                            .build()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0x338B5CF6) else vmaTheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else vmaTheme.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Track ${index + 1}: $langLabel ($chStr)",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFFA78BFA) else vmaTheme.textPrimary
                                            )
                                            Text(
                                                text = "${fmt.sampleRate} Hz • ${fmt.sampleMimeType ?: "AAC"}",
                                                fontSize = 11.sp,
                                                color = vmaTheme.textMuted
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF8B5CF6))
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Luồng stream chỉ có 1 track âm thanh mặc định (${realtimeAudioChannels}ch • ${realtimeAudioSampleRate}Hz).",
                                fontSize = 12.sp,
                                color = vmaTheme.textMuted
                            )
                        }
                    }
                }

                // 4. VIDEO TRACKS / CHẤT LƯỢNG THỰC TẾ
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                            Text("DANH SÁCH VIDEO TRACKS (CHẤT LƯỢNG)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF06B6D4))
                        }

                        if (videoTrackList.size > 1) {
                            Text("Luồng hỗ trợ ${videoTrackList.size} cấu hình bitrate (ABR):", fontSize = 11.sp, color = vmaTheme.textMuted)

                            // Tự động
                            val isAuto = currentQuality.startsWith("Tự động")
                            Surface(
                                onClick = {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                        .clearVideoSizeConstraints()
                                        .setMaxVideoBitrate(Int.MAX_VALUE)
                                        .build()
                                    currentQuality = "Tự động"
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAuto) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.surface,
                                border = BorderStroke(1.dp, if (isAuto) vmaTheme.primary else vmaTheme.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tự động (Thích ứng mạng)", fontWeight = FontWeight.Bold, color = if (isAuto) vmaTheme.primary else vmaTheme.textPrimary)
                                    if (isAuto) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                }
                            }

                            videoTrackList.forEach { (group, trackIdx) ->
                                val fmt = group.getTrackFormat(trackIdx)
                                val isSelected = currentQuality.contains("${fmt.height}p")
                                val bitStr = if (fmt.bitrate > 0) "${fmt.bitrate / 1000} kbps" else "Gốc"
                                val fpsStr = if (fmt.frameRate > 0) "@ ${fmt.frameRate.toInt()}fps" else ""

                                Surface(
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIdx))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(override)
                                            .build()
                                        currentQuality = "${fmt.height}p"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) vmaTheme.primary else vmaTheme.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${fmt.height}p (${fmt.width}x${fmt.height})", fontWeight = FontWeight.Bold, color = if (isSelected) vmaTheme.primary else vmaTheme.textPrimary)
                                            Text("$bitStr $fpsStr", fontSize = 11.sp, color = vmaTheme.textMuted)
                                        }
                                        if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Luồng IPTV phát 1 cấu hình chất lượng cố định (${if (realtimeWidth > 0) "${realtimeWidth}x${realtimeHeight}" else "1920x1080"}) từ máy chủ nhà đài. Không có danh sách đa chất lượng.",
                                fontSize = 12.sp,
                                color = vmaTheme.textMuted
                            )
                        }
                    }
                }

                // 5. PROTOCOL & NETWORK INFO
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text("GIAO THỨC & BỘ ĐỆM (BUFFER)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                        }
                        Text("• Giao thức truyền tải: $streamProtocol", fontSize = 13.sp, color = vmaTheme.textPrimary)
                        Text("• Bộ đệm dữ liệu: ${"%.1f".format(realtimeBufferSec)} giây", fontSize = 13.sp, color = vmaTheme.textPrimary)
                    }
                }

                // 6. TOGGLE FLOATING HUD OVERLAY
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isStatsHudEnabled) Color(0x2238BDF8) else vmaTheme.cardBackground,
                    border = BorderStroke(1.dp, if (isStatsHudEnabled) Color(0xFF38BDF8) else vmaTheme.cardBorder),
                    onClick = { isStatsHudEnabled = !isStatsHudEnabled },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ghim thông số lên màn hình (HUD)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = vmaTheme.textPrimary
                            )
                            Text(
                                text = "Hiển thị hộp thông số kỹ thuật thời gian thực trên góc video",
                                fontSize = 11.sp,
                                color = vmaTheme.textMuted
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isStatsHudEnabled) Color(0xFF38BDF8) else Color(0x33888888)
                        ) {
                            Text(
                                text = if (isStatsHudEnabled) "ĐANG BẬT" else "TẮT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStatsHudEnabled) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
