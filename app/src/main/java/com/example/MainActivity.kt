package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ChannelItem
import com.example.ui.components.VmaBottomNavBar
import com.example.ui.components.GeminiChatBubble
import com.example.ui.components.GeminiChatSheet
import com.example.ui.components.NavTab
import com.example.ui.components.OtaUpdateDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MultiChannelEpgScreen
import com.example.ui.screens.PvrScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.data.repository.TvRepository
import com.example.ui.theme.AppStyle
import com.example.ui.theme.LocalVmaTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TvViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var currentTabState by mutableStateOf(NavTab.HOME)
    private var tvViewModelInstance: TvViewModel? = null

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            currentTabState = NavTab.TV
        }
    }

    fun enterPipMode() {
        val vm = tvViewModelInstance
        val activeChannel = vm?.selectedChannel?.value
        if (activeChannel == null) {
            // Never enter PiP if no channel is active/playing
            return
        }
        currentTabState = NavTab.TV
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to enter PiP: ${e.message}")
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        tvViewModelInstance?.let { vm ->
            val isAutoPip = vm.isAutoPipEnabled.value
            val isWatchingTv = currentTabState == NavTab.TV && vm.selectedChannel.value != null
            if (isAutoPip && isWatchingTv) {
                enterPipMode()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra("OPEN_TAB") == "TV") {
            currentTabState = NavTab.TV
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val tvViewModel: TvViewModel = viewModel()
            tvViewModelInstance = tvViewModel
            val isDarkMode by tvViewModel.isDarkMode.collectAsStateWithLifecycle()
            val appStyle by tvViewModel.appStyle.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = isDarkMode,
                style = appStyle
            ) {
                val vmaTheme = LocalVmaTheme.current
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isInPipMode) Modifier
                            else Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        ),
                    color = if (isInPipMode) androidx.compose.ui.graphics.Color.Black else vmaTheme.background
                ) {
                    VmaApp(
                        viewModel = tvViewModel,
                        isDarkMode = isDarkMode,
                        appStyle = appStyle,
                        isInPipMode = isInPipMode,
                        requestedTab = currentTabState,
                        onEnterPip = { enterPipMode() },
                        onTabChanged = { currentTabState = it }
                    )
                }
            }
        }
    }
}

@Composable
fun VmaApp(
    viewModel: TvViewModel = viewModel(),
    isDarkMode: Boolean = true,
    appStyle: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
    isInPipMode: Boolean = false,
    requestedTab: NavTab? = null,
    onEnterPip: () -> Unit = {},
    onTabChanged: (NavTab) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentTab by remember { mutableStateOf(requestedTab ?: NavTab.HOME) }
    var isSplashActive by remember { mutableStateOf(true) }
    var initialSettingsSheet by remember { mutableStateOf("NONE") }

    LaunchedEffect(requestedTab) {
        if (requestedTab != null && requestedTab != currentTab) {
            currentTab = requestedTab
            isSplashActive = false
        }
    }

    LaunchedEffect(currentTab) {
        onTabChanged(currentTab)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isChatbotEnabled by viewModel.isChatbotEnabled.collectAsStateWithLifecycle()
    val isAutoPipEnabled by viewModel.isAutoPipEnabled.collectAsStateWithLifecycle()
    val isBackgroundAudioEnabled by viewModel.isBackgroundAudioEnabled.collectAsStateWithLifecycle()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
    val otaUpdateInfo by viewModel.otaUpdateInfo.collectAsStateWithLifecycle()
    var isShowingSetupWizard by remember { mutableStateOf(false) }

    LaunchedEffect(isSetupCompleted) {
        if (!isSetupCompleted) {
            isShowingSetupWizard = true
        }
    }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val programsForActiveChannel by viewModel.programsForActiveChannel.collectAsStateWithLifecycle()

    val activeCatchupProgram by viewModel.activeCatchupProgram.collectAsStateWithLifecycle()
    val activeCatchupUrl by viewModel.activeCatchupUrl.collectAsStateWithLifecycle()

    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val geminiModel by viewModel.geminiModel.collectAsStateWithLifecycle()

    // PVR & Stream Recording States
    val isPvrRecording by viewModel.isPvrRecording.collectAsStateWithLifecycle()
    val pvrActiveRecordings by viewModel.pvrActiveRecordings.collectAsStateWithLifecycle()
    val pvrRecordingChannel by viewModel.pvrRecordingChannel.collectAsStateWithLifecycle()
    val pvrDurationSec by viewModel.pvrDurationSeconds.collectAsStateWithLifecycle()
    val pvrBytes by viewModel.pvrRecordedBytes.collectAsStateWithLifecycle()
    val pvrUploadProgress by viewModel.pvrUploadProgress.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val pvrDestination by viewModel.pvrTargetDestination.collectAsStateWithLifecycle()
    val pvrLocalPath by viewModel.pvrLocalPath.collectAsStateWithLifecycle()
    val pvrDriveToken by viewModel.googleDriveToken.collectAsStateWithLifecycle()
    val isGoogleDriveSignedIn by viewModel.isGoogleDriveSignedIn.collectAsStateWithLifecycle()
    val googleDriveUserEmail by viewModel.googleDriveUserEmail.collectAsStateWithLifecycle()
    val googleDriveUserName by viewModel.googleDriveUserName.collectAsStateWithLifecycle()
    val googleDriveQuota by viewModel.googleDriveQuota.collectAsStateWithLifecycle()
    val pvrKeepLocalCopy by viewModel.pvrKeepLocalCopy.collectAsStateWithLifecycle()
    val pvrAutoStopMinutes by viewModel.pvrAutoStopMinutes.collectAsStateWithLifecycle()

    var otaStatusMessage by remember { mutableStateOf<String?>(null) }

    // Auto-select first channel when channels load
    LaunchedEffect(channels) {
        if (selectedChannel == null && channels.isNotEmpty()) {
            val defaultChannel = channels.firstOrNull { it.channelName.contains("VTV1", ignoreCase = true) }
                ?: channels.first()
            viewModel.selectChannel(defaultChannel)
        }
    }

    val activeChannel = selectedChannel ?: channels.firstOrNull()

    // CRITICAL: When in PiP mode, ALWAYS render strictly the VideoPlayer directly (never the app chrome or other tabs)
    if (isInPipMode) {
        if (activeChannel != null) {
            val isCurrentPipRecording = pvrActiveRecordings.any { it.channel.streamUrl == activeChannel.streamUrl }
            val currentPipRec = pvrActiveRecordings.firstOrNull { it.channel.streamUrl == activeChannel.streamUrl }
            VideoPlayerScreen(
                channel = activeChannel,
                allChannels = channels,
                programs = programsForActiveChannel,
                groups = groups,
                isRecording = isCurrentPipRecording,
                recordingDurationSec = currentPipRec?.durationSeconds ?: 0L,
                recordingBytes = currentPipRec?.recordedBytes ?: 0L,
                onStartRecording = { viewModel.startRecording(activeChannel) },
                onStopRecording = { viewModel.stopRecording(activeChannel) },
                onChannelSelect = { ch -> viewModel.selectChannel(ch) },
                onNextChannel = { viewModel.selectNextChannel() },
                onPreviousChannel = { viewModel.selectPreviousChannel() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBack = { currentTab = NavTab.HOME },
                onScheduleReminder = { program -> viewModel.scheduleProgramReminder(program, activeChannel.channelName) },
                onTriggerEpgSync = { viewModel.syncEpgOnly() },
                isInPipMode = true,
                onEnterPip = onEnterPip,
                isAutoPipEnabled = isAutoPipEnabled,
                onToggleAutoPip = { viewModel.setAutoPipEnabled(it) },
                isBackgroundAudioEnabled = isBackgroundAudioEnabled,
                onToggleBackgroundAudio = { viewModel.setBackgroundAudioEnabled(it) },
                activeCatchupProgram = activeCatchupProgram,
                activeCatchupUrl = activeCatchupUrl,
                onPlayCatchup = { prog -> viewModel.playCatchup(activeChannel, prog) },
                onExitCatchup = { viewModel.exitCatchup() },
                onOpenSettings = { currentTab = NavTab.SETTINGS }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
        }
        return
    }

    AnimatedContent(
        targetState = isSplashActive,
        transitionSpec = {
            if (targetState) {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            } else {
                // Hiệu ứng "chuyển nhá" mượt mà từ màn hình khởi động sang giao diện chính
                (fadeIn(animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)) +
                 scaleIn(initialScale = 0.94f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 1.08f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
                    )
            }
        },
        label = "splash_to_main_transition",
        modifier = Modifier.fillMaxSize()
    ) { splashActive ->
        if (splashActive) {
            SplashScreen(
                channels = channels,
                syncState = syncState,
                onFinish = { hasChannels ->
                    if (!isSetupCompleted) {
                        isShowingSetupWizard = true
                    } else if (hasChannels) {
                        currentTab = NavTab.HOME
                    } else {
                        currentTab = NavTab.HOME
                    }
                    isSplashActive = false
                }
            )
        } else if (isShowingSetupWizard) {
            SetupWizardScreen(
                onFinishSetup = { m3uUrl, epgUrl ->
                    if (m3uUrl.isNotBlank()) {
                        viewModel.addPlaylist("Danh sách phát", m3uUrl)
                    }
                    if (epgUrl.isNotBlank()) {
                        viewModel.updateSources(m3uUrl, epgUrl)
                    }
                    viewModel.completeSetup()
                    isShowingSetupWizard = false
                    currentTab = NavTab.HOME
                },
                onPerformRealScan = { m3uUrl, epgUrl, onCh, onProg, onProgress ->
                    viewModel.performRealScan(m3uUrl, epgUrl, onCh, onProg, onProgress)
                }
            )
        } else {
            // Handle back button behavior
            BackHandler(enabled = currentTab != NavTab.HOME) {
                currentTab = NavTab.HOME
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalVmaTheme.current.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Main content area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    when (currentTab) {
                        NavTab.HOME -> {
                            HomeScreen(
                                channels = channels,
                                appStyle = appStyle,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onToggleAppStyle = { viewModel.toggleAppStyle() },
                                onSelectChannel = { ch ->
                                    viewModel.selectChannel(ch)
                                    currentTab = NavTab.TV
                                },
                                onOpenTvTab = { currentTab = NavTab.TV },
                                onOpenSettings = {
                                    currentTab = NavTab.SETTINGS
                                    initialSettingsSheet = "IPTV_ORG"
                                },
                                isRefreshing = syncState.isSyncingM3u || syncState.isSyncingEpg,
                                onRefresh = { viewModel.syncAll() }
                            )
                        }

                        NavTab.EPG -> {
                            MultiChannelEpgScreen(
                                viewModel = viewModel,
                                channels = channels,
                                syncState = syncState,
                                onSelectChannel = { ch ->
                                    viewModel.selectChannel(ch)
                                    currentTab = NavTab.TV
                                },
                                onPlayCatchup = { ch, prog ->
                                    viewModel.playCatchup(ch, prog)
                                    currentTab = NavTab.TV
                                }
                            )
                        }

                        NavTab.TV -> {
                            if (activeChannel != null) {
                                val isCurrentTvRecording = pvrActiveRecordings.any { it.channel.streamUrl == activeChannel.streamUrl }
                                val currentTvRec = pvrActiveRecordings.firstOrNull { it.channel.streamUrl == activeChannel.streamUrl }
                                VideoPlayerScreen(
                                    channel = activeChannel,
                                    allChannels = channels,
                                    programs = programsForActiveChannel,
                                    groups = groups,
                                    isRecording = isCurrentTvRecording,
                                    recordingDurationSec = currentTvRec?.durationSeconds ?: 0L,
                                    recordingBytes = currentTvRec?.recordedBytes ?: 0L,
                                    onStartRecording = {
                                        viewModel.startRecording(activeChannel)
                                    },
                                    onStopRecording = {
                                        viewModel.stopRecording(activeChannel)
                                    },
                                    onChannelSelect = { ch ->
                                        viewModel.selectChannel(ch)
                                    },
                                    onNextChannel = { viewModel.selectNextChannel() },
                                    onPreviousChannel = { viewModel.selectPreviousChannel() },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onBack = {
                                        currentTab = NavTab.HOME
                                    },
                                    onScheduleReminder = { program ->
                                        viewModel.scheduleProgramReminder(program, activeChannel.channelName)
                                    },
                                    onTriggerEpgSync = { viewModel.syncEpgOnly() },
                                    isInPipMode = isInPipMode,
                                    onEnterPip = onEnterPip,
                                    isAutoPipEnabled = isAutoPipEnabled,
                                    onToggleAutoPip = { viewModel.setAutoPipEnabled(it) },
                                    isBackgroundAudioEnabled = isBackgroundAudioEnabled,
                                    onToggleBackgroundAudio = { viewModel.setBackgroundAudioEnabled(it) },
                                    activeCatchupProgram = activeCatchupProgram,
                                    activeCatchupUrl = activeCatchupUrl,
                                    onPlayCatchup = { prog -> viewModel.playCatchup(activeChannel, prog) },
                                    onExitCatchup = { viewModel.exitCatchup() },
                                    onOpenSettings = { currentTab = NavTab.SETTINGS }
                                )
                            } else {
                                HomeScreen(
                                    channels = channels,
                                    appStyle = appStyle,
                                    onToggleAppStyle = { viewModel.toggleAppStyle() },
                                    onSelectChannel = { ch ->
                                        viewModel.selectChannel(ch)
                                        currentTab = NavTab.TV
                                    },
                                    onOpenTvTab = { currentTab = NavTab.TV },
                                    onOpenChat = { viewModel.openChat() },
                                    isRefreshing = syncState.isSyncingM3u || syncState.isSyncingEpg,
                                    onRefresh = { viewModel.syncAll() }
                                )
                            }
                        }

                        NavTab.PVR -> {
                            PvrScreen(
                                recordings = recordings,
                                isRecording = isPvrRecording,
                                activeRecordings = pvrActiveRecordings,
                                recordingChannel = pvrRecordingChannel,
                                recordingDurationSec = pvrDurationSec,
                                recordingBytes = pvrBytes,
                                uploadProgress = pvrUploadProgress,
                                isGoogleDriveSignedIn = isGoogleDriveSignedIn,
                                googleDriveUserEmail = googleDriveUserEmail,
                                onSignInGoogleDrive = { email, name ->
                                    viewModel.signInGoogleDrive(email, name)
                                },
                                onStopRecording = { viewModel.stopRecording() },
                                onStopRecordingChannel = { ch -> viewModel.stopRecording(ch) },
                                onStopAllRecordings = { viewModel.stopAllRecordings() },
                                onPlayRecording = { rec ->
                                    val file = File(rec.filePath)
                                    if (file.exists()) {
                                        val pvrChannel = ChannelItem(
                                            channelName = rec.channelName,
                                            streamUrl = rec.filePath,
                                            tvgId = rec.channelId,
                                            tvgName = rec.channelName,
                                            tvgLogo = rec.channelLogo ?: "",
                                            groupTitle = "Bản ghi PVR",
                                            currentProgramTitle = rec.programTitle ?: "Bản ghi PVR"
                                        )
                                        viewModel.selectChannel(pvrChannel)
                                        currentTab = NavTab.TV
                                    }
                                },
                                onDeleteRecording = { rec -> viewModel.deleteRecording(rec) },
                                onUploadToDrive = { rec -> viewModel.uploadRecordingToDrive(rec) },
                                onOpenSettings = { currentTab = NavTab.SETTINGS },
                                onGoToLiveTv = { currentTab = NavTab.TV }
                            )
                        }

                        NavTab.SETTINGS -> {
                            SettingsScreen(
                                initialM3uUrl = viewModel.repository.m3uUrl,
                                initialEpgUrl = viewModel.repository.epgUrl,
                                syncState = syncState,
                                playlists = playlists,
                                onAddPlaylist = { name, url -> viewModel.addPlaylist(name, url) },
                                onUpdatePlaylist = { pl -> viewModel.updatePlaylist(pl) },
                                onTogglePlaylist = { id, enabled -> viewModel.togglePlaylist(id, enabled) },
                                onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                onRestoreDefaultPlaylists = { viewModel.restoreDefaultPlaylists() },
                                onAdminLogin = { u, p -> viewModel.verifyAndActivateAdminM3u(u, p) },
                                initialSubSheet = initialSettingsSheet,
                                isAutoPipEnabled = isAutoPipEnabled,
                                onToggleAutoPip = { viewModel.setAutoPipEnabled(it) },
                                isBackgroundAudioEnabled = isBackgroundAudioEnabled,
                                onToggleBackgroundAudio = { viewModel.setBackgroundAudioEnabled(it) },
                                geminiApiKey = geminiApiKey,
                                geminiModel = geminiModel,
                                isDarkMode = isDarkMode,
                                appStyle = appStyle,
                                isChatbotEnabled = isChatbotEnabled,
                                pvrTargetDestination = pvrDestination,
                                pvrLocalPath = pvrLocalPath,
                                googleDriveToken = pvrDriveToken,
                                isGoogleDriveSignedIn = isGoogleDriveSignedIn,
                                googleDriveUserEmail = googleDriveUserEmail,
                                googleDriveUserName = googleDriveUserName,
                                googleDriveQuota = googleDriveQuota,
                                pvrKeepLocalCopy = pvrKeepLocalCopy,
                                pvrAutoStopMinutes = pvrAutoStopMinutes,
                                defaultRecordingsPath = viewModel.getDefaultRecordingsDirPath(),
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onSetAppStyle = { viewModel.setAppStyle(it) },
                                onSetChatbotEnabled = { viewModel.setChatbotEnabled(it) },
                                onSaveGeminiConfig = { key, model ->
                                    viewModel.setGeminiApiKey(key)
                                    viewModel.setGeminiModel(model)
                                },
                                onTestGeminiConnection = { key, model, callback ->
                                    viewModel.runGeminiDiagnostics(key, model, callback)
                                },
                                onSavePvrConfig = { dest, local, token, keep, autoStop ->
                                    viewModel.savePvrConfig(dest, local, token, keep, autoStop)
                                },
                                onTestGoogleDriveConnection = { token, callback ->
                                    viewModel.testGoogleDriveConnection(token, callback)
                                },
                                onSignInGoogleDrive = { email, name ->
                                    viewModel.signInGoogleDrive(email, name)
                                },
                                onSignOutGoogleDrive = {
                                    viewModel.signOutGoogleDrive()
                                },
                                onSaveAndSync = { m3u, epg ->
                                    viewModel.updateSources(m3u, epg)
                                },
                                onSyncM3uOnly = { viewModel.syncM3uOnly() },
                                onSyncEpgOnly = { viewModel.syncEpgOnly() },
                                onReopenSetupWizard = {
                                    isShowingSetupWizard = true
                                },
                                onCheckOtaUpdate = { manual ->
                                    viewModel.checkOtaUpdate(manual = manual) { info, err ->
                                        if (manual) {
                                            if (info != null && !info.hasUpdate) {
                                                otaStatusMessage = "Bạn đang dùng phiên bản mới nhất (${info.currentVersion}). Chưa có bản cập nhật nào mới hơn trên GitHub."
                                            } else if (err != null) {
                                                otaStatusMessage = err
                                            }
                                        }
                                    }
                                },
                                onLoadAdminM3u = { viewModel.loadAdminM3uContent() },
                                onSaveAdminM3u = { content -> viewModel.saveAdminM3uContent(content) },
                                onPerformRealScan = { m3uUrl, epgUrl, onCh, onProg, onProgress ->
                                    viewModel.performRealScan(m3uUrl, epgUrl, onCh, onProg, onProgress)
                                },
                                onBack = { currentTab = NavTab.HOME }
                            )
                        }
                    }
                }

                // Bottom Navigation Bar (Shown in portrait mode and NOT in PiP)
                if (!isLandscape && !isInPipMode) {
                    VmaBottomNavBar(
                        selectedTab = currentTab,
                        onSelectTab = { tab ->
                            currentTab = tab
                        },
                        isPvrRecording = isPvrRecording
                    )
                }
            }
        }
    }

    // OTA Update Dialog
    otaUpdateInfo?.let { updateInfo ->
        OtaUpdateDialog(
            updateInfo = updateInfo,
            onDismiss = { viewModel.dismissOtaDialog() }
        )
    }

    // OTA Manual Check Result Dialog (e.g. up to date or detailed error info)
    otaStatusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { otaStatusMessage = null },
            title = {
                Text(
                    text = "Thông tin cập nhật OTA",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { otaStatusMessage = null }
                ) {
                    Text("Đã hiểu")
                }
            }
        )
    }
}
}

/**
 * Kept for screenshot test compatibility
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
