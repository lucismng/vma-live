package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.remote.ota.OtaManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.gemini.GeminiDiagnosticResult
import com.example.data.repository.TvRepository
import com.example.model.IptvPlaylist
import com.example.model.SyncState
import com.example.ui.components.ChannelScanningDialog
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.VmaFooterBrand
import com.example.ui.theme.AppStyle
import com.example.ui.theme.FlowLiveGreen
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import com.example.util.FileManagerHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsSubSheet {
    NONE,
    M3U_CONFIG,
    EPG_CONFIG,
    SYNC_STATUS,
    PVR_STORAGE,
    GOOGLE_DRIVE,
    AUTO_STOP,
    APP_STYLE,
    GEMINI_CONFIG,
    DEVICE_INFO
}

@Composable
fun SettingsScreen(
    initialM3uUrl: String,
    initialEpgUrl: String,
    syncState: SyncState,
    playlists: List<IptvPlaylist> = emptyList(),
    onAddPlaylist: (name: String, url: String) -> Unit = { _, _ -> },
    onUpdatePlaylist: (IptvPlaylist) -> Unit = {},
    onTogglePlaylist: (id: String, isEnabled: Boolean) -> Unit = { _, _ -> },
    onDeletePlaylist: (id: String) -> Unit = {},
    onRestoreDefaultPlaylists: () -> Unit = {},
    onAdminLogin: (String, String) -> Boolean = { _, _ -> false },
    initialSubSheet: String = "NONE",
    geminiApiKey: String = "",
    geminiModel: String = "gemini-3.5-flash-lite",
    isDarkMode: Boolean = true,
    appStyle: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
    isChatbotEnabled: Boolean = true,
    pvrTargetDestination: String = "LOCAL",
    pvrLocalPath: String = "",
    googleDriveToken: String = "",
    isGoogleDriveSignedIn: Boolean = false,
    googleDriveUserEmail: String = "",
    googleDriveUserName: String = "",
    googleDriveQuota: String = "",
    pvrKeepLocalCopy: Boolean = true,
    pvrAutoStopMinutes: Int = 0,
    defaultRecordingsPath: String = "",
    isAutoPipEnabled: Boolean = true,
    onToggleAutoPip: (Boolean) -> Unit = {},
    isBackgroundAudioEnabled: Boolean = true,
    onToggleBackgroundAudio: (Boolean) -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onSetAppStyle: (AppStyle) -> Unit = {},
    onSetChatbotEnabled: (Boolean) -> Unit = {},
    onSaveGeminiConfig: (apiKey: String, model: String) -> Unit = { _, _ -> },
    onTestGeminiConnection: (apiKey: String, model: String, (GeminiDiagnosticResult) -> Unit) -> Unit = { _, _, _ -> },
    onSavePvrConfig: (destination: String, localPath: String, driveToken: String, keepLocalCopy: Boolean, autoStopMinutes: Int) -> Unit = { _, _, _, _, _ -> },
    onTestGoogleDriveConnection: (token: String, (com.example.data.remote.drive.DriveConnectionStatus) -> Unit) -> Unit = { _, _ -> },
    onSignInGoogleDrive: (email: String, name: String) -> Unit = { _, _ -> },
    onSignOutGoogleDrive: () -> Unit = {},
    onSaveAndSync: (m3uUrl: String, epgUrl: String) -> Unit,
    onSyncM3uOnly: () -> Unit,
    onSyncEpgOnly: () -> Unit,
    onReopenSetupWizard: () -> Unit = {},
    onCheckOtaUpdate: (manual: Boolean) -> Unit = {},
    onLoadAdminM3u: () -> String = { "" },
    onSaveAdminM3u: (String) -> Unit = {},
    onPerformRealScan: suspend (
        m3uUrl: String?,
        epgUrl: String?,
        onChannelFound: (String) -> Unit,
        onProgramCountUpdated: (Int) -> Unit,
        onProgressUpdated: (Float, String) -> Unit
    ) -> Pair<Int, Int> = { _, _, _, _, _ -> Pair(0, 0) },
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vmaTheme = LocalVmaTheme.current
    val scrollState = rememberScrollState()

    var activeSheet by remember {
        mutableStateOf(
            if (initialSubSheet == "M3U_CONFIG") SettingsSubSheet.M3U_CONFIG else SettingsSubSheet.NONE
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var showScanningDialog by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatus by remember { mutableStateOf("") }
    val scanDetectedChannels = remember { mutableStateListOf<String>() }
    var scanProgramCount by remember { mutableIntStateOf(0) }
    var isScanFinished by remember { mutableStateOf(false) }

    fun startScanningFlow(name: String = "", url: String = "") {
        showScanningDialog = true
        isScanFinished = false
        scanProgress = 0.05f
        scanDetectedChannels.clear()
        scanProgramCount = 0
        scanStatus = "Đang kết nối luồng phát và nạp danh sách kênh..."

        coroutineScope.launch {
            if (url.isNotBlank()) {
                onAddPlaylist(name.ifBlank { "Playlist M3U" }, url)
            }
            try {
                val effectiveUrl = if (url.isNotBlank()) url else null
                val (chCount, progCount) = onPerformRealScan(
                    effectiveUrl,
                    null,
                    { channelName ->
                        if (!scanDetectedChannels.contains(channelName)) {
                            scanDetectedChannels.add(channelName)
                        }
                    },
                    { pCount ->
                        scanProgramCount = pCount
                    },
                    { progress, status ->
                        scanProgress = progress
                        scanStatus = status
                    }
                )
                scanProgramCount = progCount
                scanProgress = 1.0f
                scanStatus = "Đã hoàn thành! Nạp được $chCount kênh và $progCount chương trình EPG."
                delay(300)
                isScanFinished = true
            } catch (e: Exception) {
                scanStatus = "Hoàn tất: ${e.localizedMessage}"
                scanProgress = 1.0f
                isScanFinished = true
            }
        }
    }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAdminEditorDialog by remember { mutableStateOf(false) }
    var adminM3uEditorContent by remember { mutableStateOf("") }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<IptvPlaylist?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistUrl by remember { mutableStateOf("") }

    var m3uInput by remember { mutableStateOf(initialM3uUrl) }
    var epgInput by remember { mutableStateOf(initialEpgUrl) }
    var apiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var selectedModel by remember { mutableStateOf(geminiModel) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var diagnosticResult by remember { mutableStateOf<GeminiDiagnosticResult?>(null) }

    var destinationInput by remember { mutableStateOf(pvrTargetDestination) }
    var localPathInput by remember { mutableStateOf(pvrLocalPath.ifBlank { defaultRecordingsPath }) }
    var driveTokenInput by remember { mutableStateOf(googleDriveToken) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var keepLocalCopyInput by remember { mutableStateOf(pvrKeepLocalCopy) }
    var autoStopMinutesInput by remember { mutableIntStateOf(pvrAutoStopMinutes) }

    var showLiquidGlassWarning by remember { mutableStateOf(false) }
    var showRepoConfigDialog by remember { mutableStateOf(false) }
    var otaRepoInput by remember { mutableStateOf(OtaManager.getRepository(context)) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val lastSyncFormatted = remember(syncState.lastSyncTime) {
        if (syncState.lastSyncTime > 0) dateFormat.format(Date(syncState.lastSyncTime)) else "Chưa đồng bộ"
    }

    // Launcher chọn thư mục lưu trữ qua SAF
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val path = FileManagerHelper.resolveStoragePath(context, uri.toString())
            localPathInput = path
            onSavePvrConfig(destinationInput, localPathInput, driveTokenInput, keepLocalCopyInput, autoStopMinutesInput)
            Toast.makeText(context, "Đã lưu thư mục: $path", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher chọn tài khoản Google của máy
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val name = accountName.substringBefore("@")
                onSignInGoogleDrive(accountName, name)
                Toast.makeText(context, "Đã liên kết Google Account: $accountName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        onBack()
    }

    // Dark sleek background matching user's reference image
    val pageBgColor = if (vmaTheme.isDark) Color(0xFF0F1115) else Color(0xFFF1F5F9)
    val cardBgColor = if (vmaTheme.isDark) Color(0xFF191C22) else Color.White
    val cardBorderColor = if (vmaTheme.isDark) Color(0x1AFFFFFF) else Color(0x1A000000)
    val dividerColor = if (vmaTheme.isDark) Color(0x12FFFFFF) else Color(0x0F000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        // ==========================================
        // TOP HEADER: "< Cài đặt và quyền riêng tư"
        // (Clean, sleek, matching the user reference screenshot)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = vmaTheme.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Cài đặt và quyền riêng tư",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = vmaTheme.textPrimary,
                fontSize = 19.sp
            )
        }

        // ==========================================
        // MAIN SCROLLABLE GROUPED LIST
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // ----------------------------------------------------------------
            // GROUP 1: Nguồn kênh & Lịch phát sóng
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Nguồn kênh và truyền hình",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                val visibleList = playlists.filter { !it.isHiddenAdmin }
                val activeCount = visibleList.count { it.isEnabled }
                val totalCount = visibleList.size
                val playlistSubtitle = if (totalCount > 1) {
                    "$activeCount/$totalCount playlist đang bật"
                } else if (visibleList.isNotEmpty()) {
                    "${visibleList.first().name}"
                } else {
                    "Chưa có playlist tùy chỉnh nào"
                }
                SettingsRowItem(
                    icon = Icons.Default.Tv,
                    title = "Danh sách kênh tùy chỉnh / ngoài (M3U)",
                    subtitle = playlistSubtitle,
                    onClick = { activeSheet = SettingsSubSheet.M3U_CONFIG }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.CalendarMonth,
                    title = "Nguồn lịch phát sóng (EPG)",
                    subtitle = when (epgInput) {
                        TvRepository.DEFAULT_EPG_URL -> "epg.xml (Chữ thường - Mặc định)"
                        TvRepository.UPPERCASE_EPG_URL -> "epgu.xml (Chữ HOA)"
                        TvRepository.DETAILED_EPG_URL -> "epgc.xml (Tiêu đề + mô tả)"
                        TvRepository.DEFAULT_EPG_GZ_URL -> "epg.xml.gz (File nén thường)"
                        TvRepository.UPPERCASE_EPG_GZ_URL -> "epgu.xml.gz (File nén HOA)"
                        TvRepository.DETAILED_EPG_GZ_URL -> "epgc.xml.gz (File nén chi tiết)"
                        else -> epgInput.take(40)
                    },
                    onClick = { activeSheet = SettingsSubSheet.EPG_CONFIG }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Refresh,
                    title = "Đồng bộ kênh & Lịch EPG",
                    subtitle = "${syncState.channelCount} kênh • ${syncState.programCount} lịch",
                    onClick = { activeSheet = SettingsSubSheet.SYNC_STATUS }
                )
            }

            // ----------------------------------------------------------------
            // GROUP 2: Lưu trữ & Ghi hình PVR
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Lưu trữ và Ghi hình PVR",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                SettingsRowItem(
                    icon = Icons.Default.Folder,
                    title = "Quản lý tệp bản ghi trên máy",
                    subtitle = if (destinationInput == "LOCAL") "Bộ nhớ máy" else "Google Drive",
                    onClick = { activeSheet = SettingsSubSheet.PVR_STORAGE }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Cloud,
                    title = "Dịch vụ Google Drive",
                    subtitle = "Sắp ra mắt (Coming soon)",
                    badgeText = "Coming soon",
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Timer,
                    title = "Tự động ngắt ghi hình",
                    subtitle = if (autoStopMinutesInput == 0) "Không giới hạn" else "$autoStopMinutesInput phút",
                    onClick = { activeSheet = SettingsSubSheet.AUTO_STOP }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsSwitchRowItem(
                    icon = Icons.Default.Save,
                    title = "Giữ bản sao trên thiết bị",
                    subtitle = "Vẫn giữ file khi tải lên Google Drive",
                    checked = keepLocalCopyInput,
                    onCheckedChange = {
                        keepLocalCopyInput = it
                        onSavePvrConfig(destinationInput, localPathInput, driveTokenInput, keepLocalCopyInput, autoStopMinutesInput)
                    }
                )
            }

            // ----------------------------------------------------------------
            // GROUP 3: Giao diện & Hiển thị
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Giao diện và trải nghiệm",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                SettingsSwitchRowItem(
                    icon = Icons.Default.DarkMode,
                    title = "Chế độ tối (Dark Mode)",
                    subtitle = if (isDarkMode) "Bật (Xem ban đêm dễ chịu)" else "Tắt (Chế độ sáng)",
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Speed,
                    title = "Phong cách thiết kế",
                    subtitle = "Sắp ra mắt (Coming soon)",
                    badgeText = "Coming soon",
                    enabled = false,
                    onClick = {}
                )
            }

            // ----------------------------------------------------------------
            // GROUP: Hình trong hình (PiP) & Phát dưới nền
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Hình trong hình (PiP) & Phát dưới nền",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                SettingsSwitchRowItem(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "Tự động vào PiP khi thoát ứng dụng",
                    subtitle = if (isAutoPipEnabled) "Bật (Thu nhỏ thành màn hình nổi khi nhấn Home)" else "Tắt (Không tự động thu nhỏ)",
                    checked = isAutoPipEnabled,
                    onCheckedChange = onToggleAutoPip
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsSwitchRowItem(
                    icon = Icons.Default.Headphones,
                    title = "Phát âm thanh dưới nền (Background Audio)",
                    subtitle = if (isBackgroundAudioEnabled) "Bật (Tiếp tục nghe tiếng khi tắt màn hình/đa nhiệm)" else "Tắt (Dừng phát khi ẩn ứng dụng)",
                    checked = isBackgroundAudioEnabled,
                    onCheckedChange = onToggleBackgroundAudio
                )
            }

            // ----------------------------------------------------------------
            // GROUP 4: Trí tuệ nhân tạo Gemini AI
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Trí tuệ nhân tạo Gemini",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                SettingsSwitchRowItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Bong bóng Trợ lý AI trên màn hình",
                    subtitle = "Sắp ra mắt (Coming soon)",
                    badgeText = "Coming soon",
                    enabled = false,
                    checked = false,
                    onCheckedChange = {}
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.VpnKey,
                    title = "Cấu hình Google Gemini AI",
                    subtitle = "Sắp ra mắt (Coming soon)",
                    badgeText = "Coming soon",
                    enabled = false,
                    onClick = {}
                )
            }

            // ----------------------------------------------------------------
            // GROUP 5: Thông tin & Hỗ trợ (Matching user screenshot)
            // ----------------------------------------------------------------
            SettingsGroup(
                title = "Thông tin & Hỗ trợ khách hàng",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                SettingsRowItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Kiểm tra cập nhật",
                    subtitle = "Phiên bản v1.0.1 • Bấm để kiểm tra bản mới",
                    onClick = {
                        Toast.makeText(context, "Đang kiểm tra bản cập nhật mới trên GitHub...", Toast.LENGTH_SHORT).show()
                        onCheckOtaUpdate(true)
                    }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Cloud,
                    title = "Kho lưu trữ GitHub OTA",
                    subtitle = "$otaRepoInput • Chạm để đổi kho hoặc xem hướng dẫn",
                    onClick = { showRepoConfigDialog = true }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = "Thông tin thiết bị & ứng dụng",
                    subtitle = "ExoPlayer v1.4 • MediaCodec Fallback",
                    onClick = { activeSheet = SettingsSubSheet.DEVICE_INFO }
                )
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 48.dp))
                SettingsRowItem(
                    icon = Icons.Default.Security,
                    title = "Chính sách bảo mật & Quyền riêng tư",
                    subtitle = "Bảo mật dữ liệu cá nhân",
                    onClick = {
                        Toast.makeText(context, "VMA cam kết bảo mật 100% dữ liệu thiết bị của bạn", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(
                title = "Theo dõi VMA - Tư liệu Truyền thông Việt Nam qua các nền tảng:",
                cardBg = cardBgColor,
                cardBorder = cardBorderColor,
                divider = dividerColor
            ) {
                val socialPlatforms = listOf(
                    SocialPlatform(
                        title = "Fanpage Facebook",
                        handle = "fb.com/VMAtulieutruyenthongVN",
                        url = "https://fb.com/VMAtulieutruyenthongVN",
                        badgeText = "FB",
                        badgeColor = Color(0xFF1877F2)
                    ),
                    SocialPlatform(
                        title = "FB Group (Cộng đồng)",
                        handle = "fb.com/groups/tulieutruyenthongVN",
                        url = "https://fb.com/groups/tulieutruyenthongVN",
                        badgeText = "GRP",
                        badgeColor = Color(0xFF0866FF)
                    ),
                    SocialPlatform(
                        title = "YouTube",
                        handle = "youtube.com/VMAtulieutruyenthongVN",
                        url = "https://youtube.com/VMAtulieutruyenthongVN",
                        badgeText = "YT",
                        badgeColor = Color(0xFFFF0000)
                    ),
                    SocialPlatform(
                        title = "Instagram",
                        handle = "instagram.com/vmatulieutruyenthongvn",
                        url = "https://www.instagram.com/vmatulieutruyenthongvn/",
                        badgeText = "IG",
                        badgeColor = Color(0xFFE4405F)
                    ),
                    SocialPlatform(
                        title = "TikTok",
                        handle = "tiktok.com/@vmatulieutruyenthongvn",
                        url = "https://www.tiktok.com/@vmatulieutruyenthongvn",
                        badgeText = "TT",
                        badgeColor = Color(0xFF000000)
                    )
                )

                for ((index, platform) in socialPlatforms.withIndex()) {
                    if (index > 0) {
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 64.dp))
                    }
                    SocialPlatformRowItem(
                        platform = platform,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở liên kết: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // App Version Footer with official VMA Brand Logo (Nhấn 5 lần để mở Admin)
            VmaFooterBrand(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                onSecretAdminTap = { showAdminLoginDialog = true }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // =========================================================================
    // SUB-DIALOGS / SHEETS FOR FOCUSED EDITING (CLEAN & NON-CLUTTERED)
    // =========================================================================

    // 0.1 Admin Login Dialog (5 Taps on VMA Logo)
    if (showAdminLoginDialog) {
        var adminUser by remember { mutableStateOf("") }
        var adminPass by remember { mutableStateOf("") }
        var adminErrorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAdminLoginDialog = false
                adminErrorMsg = ""
            },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng nhập", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Vui lòng đăng nhập tài khoản được cung cấp:",
                        fontSize = 13.sp,
                        color = vmaTheme.textPrimary
                    )

                    OutlinedTextField(
                        value = adminUser,
                        onValueChange = {
                            adminUser = it
                            adminErrorMsg = ""
                        },
                        label = { Text("Tên đăng nhập") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    OutlinedTextField(
                        value = adminPass,
                        onValueChange = {
                            adminPass = it
                            adminErrorMsg = ""
                        },
                        label = { Text("Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    if (adminErrorMsg.isNotEmpty()) {
                        Text(
                            text = adminErrorMsg,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onAdminLogin(adminUser, adminPass)
                        if (success) {
                            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            showAdminLoginDialog = false
                            adminErrorMsg = ""
                            adminM3uEditorContent = onLoadAdminM3u()
                            showAdminEditorDialog = true
                        } else {
                            adminErrorMsg = "Sai tài khoản hoặc mật khẩu! Vui lòng thử lại."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng nhập", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminLoginDialog = false
                    adminErrorMsg = ""
                }) {
                    Text("Hủy", color = vmaTheme.textMuted)
                }
            }
        )
    }

    // 0.2 GitHub Repository OTA Config Dialog
    if (showRepoConfigDialog) {
        var tempRepo by remember { mutableStateOf(otaRepoInput) }

        AlertDialog(
            onDismissRequest = { showRepoConfigDialog = false },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kho cập nhật GitHub (OTA)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nhập tên Repository GitHub của bạn (dạng user/repo):\nVí dụ: lucismng/vma-live",
                        fontSize = 13.sp,
                        color = vmaTheme.textPrimary
                    )

                    OutlinedTextField(
                        value = tempRepo,
                        onValueChange = { tempRepo = it },
                        label = { Text("GitHub Repo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    Text(
                        text = "💡 Lưu ý để không bị lỗi 404:\n• Kho lưu trữ phải ở chế độ Public (Công khai).\n• Đã Publish bản Release (Draft hoặc Pre-release không tính).\n• Đã tải đính kèm file APK (.apk) vào Release đó.",
                        fontSize = 12.sp,
                        color = vmaTheme.textMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = tempRepo.trim().removePrefix("https://github.com/").trim('/')
                        if (clean.isNotBlank() && clean.contains('/')) {
                            OtaManager.setRepository(context, clean)
                            otaRepoInput = clean
                            Toast.makeText(context, "Đã lưu kho: $clean", Toast.LENGTH_SHORT).show()
                            showRepoConfigDialog = false
                        } else {
                            Toast.makeText(context, "Định dạng không hợp lệ! Cần dạng username/repository", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val defaultRepo = OtaManager.DEFAULT_GITHUB_REPO
                        OtaManager.setRepository(context, defaultRepo)
                        otaRepoInput = defaultRepo
                        tempRepo = defaultRepo
                        Toast.makeText(context, "Đã đặt về mặc định ($defaultRepo)", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Mặc định", color = vmaTheme.primary)
                    }
                    TextButton(onClick = { showRepoConfigDialog = false }) {
                        Text("Đóng", color = vmaTheme.textMuted)
                    }
                }
            }
        )
    }

    // Admin M3U Content Editor Dialog
    if (showAdminEditorDialog) {
        AlertDialog(
            onDismissRequest = { showAdminEditorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = vmaTheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chỉnh sửa tệp ADMIN.m3u", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Thêm hoặc chỉnh sửa các kênh IPTV quản trị của bạn:",
                        fontSize = 12.sp,
                        color = vmaTheme.textMuted
                    )
                    OutlinedTextField(
                        value = adminM3uEditorContent,
                        onValueChange = { adminM3uEditorContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAdminM3u(adminM3uEditorContent)
                        showAdminEditorDialog = false
                        Toast.makeText(context, "Đã lưu nội dung ADMIN.m3u và đồng bộ!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Lưu & Đồng bộ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminEditorDialog = false }) {
                    Text("Đóng", color = vmaTheme.textMuted)
                }
            }
        )
    }

    // 1. M3U Multi-Playlist Config Sheet
    if (activeSheet == SettingsSubSheet.M3U_CONFIG) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quản lý danh sách phát IPTV", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    IconButton(
                        onClick = {
                            newPlaylistName = ""
                            newPlaylistUrl = ""
                            editingPlaylist = null
                            showAddPlaylistDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm Playlist", tint = vmaTheme.primary)
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Hỗ trợ chạy đồng thời nhiều Playlist IPTV. Các kênh sẽ được tự động gộp và cập nhật đầy đủ.",
                        fontSize = 12.sp,
                        color = vmaTheme.textMuted
                    )

                    Text(
                        "Thêm hoặc tùy chỉnh danh sách kênh M3U ngoài:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = vmaTheme.textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val visiblePlaylists = remember(playlists) { playlists.filter { !it.isHiddenAdmin } }
                    Text(
                        "Danh sách playlist hiện tại (${visiblePlaylists.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = vmaTheme.textPrimary
                    )

                    if (visiblePlaylists.isEmpty()) {
                        Text("Chưa có playlist nào. Nhấn nút bên dưới để thêm danh sách phát.", fontSize = 13.sp, color = vmaTheme.textMuted)
                    } else {
                        visiblePlaylists.forEach { pl ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (vmaTheme.isDark) Color(0xFF13161C) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (pl.isEnabled) vmaTheme.primary.copy(alpha = 0.4f) else cardBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = pl.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (pl.isEnabled) vmaTheme.textPrimary else vmaTheme.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (pl.channelCount > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(vmaTheme.primary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${pl.channelCount} kênh",
                                                        fontSize = 10.sp,
                                                        color = vmaTheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = pl.url,
                                            fontSize = 11.sp,
                                            color = vmaTheme.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingPlaylist = pl
                                                newPlaylistName = pl.name
                                                newPlaylistUrl = pl.url
                                                showAddPlaylistDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Sửa",
                                                tint = vmaTheme.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (visiblePlaylists.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    onDeletePlaylist(pl.id)
                                                    Toast.makeText(context, "Đã xóa ${pl.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa",
                                                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = pl.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                onTogglePlaylist(pl.id, isChecked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = vmaTheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                newPlaylistName = ""
                                newPlaylistUrl = ""
                                editingPlaylist = null
                                showAddPlaylistDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm Playlist", fontSize = 11.sp, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                onRestoreDefaultPlaylists()
                                Toast.makeText(context, "Đã xóa toàn bộ playlist tùy chỉnh", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Khôi phục mặc định", fontSize = 11.sp, softWrap = false)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSyncM3uOnly()
                        activeSheet = SettingsSubSheet.NONE
                        Toast.makeText(context, "Đang đồng bộ tất cả playlist...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary)
                ) {
                    Text("Đồng bộ tất cả")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Sub-dialog: Thêm hoặc sửa Playlist
    if (showAddPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = false },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    if (editingPlaylist != null) "Chỉnh sửa Playlist" else "Thêm Playlist IPTV mới",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tên gợi nhớ cho Playlist:", fontSize = 12.sp, color = vmaTheme.textMuted)
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("VD: Kênh Tổng Hợp, VTC, Phim...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    Text("Đường dẫn danh sách M3U / M3U8:", fontSize = 12.sp, color = vmaTheme.textMuted)
                    OutlinedTextField(
                        value = newPlaylistUrl,
                        onValueChange = { newPlaylistUrl = it },
                        placeholder = { Text("https://example.com/playlist.m3u") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistUrl.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập đường dẫn M3U", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val currentEditing = editingPlaylist
                        if (currentEditing != null) {
                            onUpdatePlaylist(
                                currentEditing.copy(
                                    name = newPlaylistName.ifBlank { "Playlist" },
                                    url = newPlaylistUrl.trim()
                                )
                            )
                            Toast.makeText(context, "Đã cập nhật ${newPlaylistName.ifBlank { "Playlist" }}", Toast.LENGTH_SHORT).show()
                        } else {
                            startScanningFlow(newPlaylistName.ifBlank { "Playlist mới" }, newPlaylistUrl.trim())
                            Toast.makeText(context, "Đang nạp và dò danh sách kênh...", Toast.LENGTH_SHORT).show()
                        }
                        showAddPlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary)
                ) {
                    Text("Lưu & Kích hoạt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaylistDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // 2. EPG Config Sheet
    if (activeSheet == SettingsSubSheet.EPG_CONFIG) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Nguồn lịch phát sóng (EPG)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chọn nhanh nguồn lịch phát sóng XMLTV (epg.io.vn):", fontSize = 12.sp, color = vmaTheme.textMuted)
                    
                    Text("Định dạng chuẩn (XML):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = vmaTheme.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = epgInput == TvRepository.DEFAULT_EPG_URL,
                            onClick = { epgInput = TvRepository.DEFAULT_EPG_URL },
                            label = { Text("Chữ thường (epg.xml) ★", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = epgInput == TvRepository.UPPERCASE_EPG_URL,
                            onClick = { epgInput = TvRepository.UPPERCASE_EPG_URL },
                            label = { Text("Chữ HOA (epgu.xml)", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = epgInput == TvRepository.DETAILED_EPG_URL,
                            onClick = { epgInput = TvRepository.DETAILED_EPG_URL },
                            label = { Text("Chi tiết (epgc.xml)", fontSize = 10.5.sp) }
                        )
                    }

                    Text("Định dạng nén (nhẹ hơn, load nhanh hơn):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = vmaTheme.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = epgInput == TvRepository.DEFAULT_EPG_GZ_URL,
                            onClick = { epgInput = TvRepository.DEFAULT_EPG_GZ_URL },
                            label = { Text("Nén thường (.xml.gz)", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = epgInput == TvRepository.UPPERCASE_EPG_GZ_URL,
                            onClick = { epgInput = TvRepository.UPPERCASE_EPG_GZ_URL },
                            label = { Text("Nén HOA (epgu.gz)", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = epgInput == TvRepository.DETAILED_EPG_GZ_URL,
                            onClick = { epgInput = TvRepository.DETAILED_EPG_GZ_URL },
                            label = { Text("Nén chi tiết (epgc.gz)", fontSize = 10.5.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = epgInput,
                        onValueChange = { epgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAndSync(m3uInput, epgInput)
                        activeSheet = SettingsSubSheet.NONE
                        Toast.makeText(context, "Đã lưu và đồng bộ lịch phát sóng", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary)
                ) {
                    Text("Lưu & Tải EPG")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Đóng")
                }
            }
        )
    }

    // 3. Sync Status Sheet
    if (activeSheet == SettingsSubSheet.SYNC_STATUS) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Trạng thái dữ liệu", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng số kênh:", fontSize = 13.sp, color = vmaTheme.textMuted)
                        Text("${syncState.channelCount} kênh", fontWeight = FontWeight.Bold, color = FlowLiveGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng số chương trình EPG:", fontSize = 13.sp, color = vmaTheme.textMuted)
                        Text("${syncState.programCount} lịch", fontWeight = FontWeight.Bold, color = vmaTheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lần cập nhật cuối:", fontSize = 13.sp, color = vmaTheme.textMuted)
                        Text(lastSyncFormatted, fontSize = 12.sp, color = vmaTheme.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSyncM3uOnly,
                            enabled = !syncState.isSyncingM3u,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (syncState.isSyncingM3u) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Đồng bộ M3U", fontSize = 11.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onSyncEpgOnly,
                            enabled = !syncState.isSyncingEpg,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (syncState.isSyncingEpg) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Đồng bộ EPG", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Xong")
                }
            }
        )
    }

    // 4. PVR Storage Sheet (File Manager direct link)
    if (activeSheet == SettingsSubSheet.PVR_STORAGE) {
        val standardDirs = remember(context) { FileManagerHelper.getStandardStorageDirectories(context) }
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Vị trí lưu bản ghi (PVR)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Chọn nhanh thư mục lưu trữ sẵn có trên thiết bị:", fontSize = 12.sp, color = vmaTheme.textMuted)

                    standardDirs.forEach { (label, folder) ->
                        val isSelected = localPathInput == folder.absolutePath
                        Surface(
                            onClick = {
                                localPathInput = folder.absolutePath
                                try {
                                    if (!folder.exists()) folder.mkdirs()
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) vmaTheme.primary.copy(alpha = 0.15f) else Color(0x11FFFFFF),
                            border = BorderStroke(1.dp, if (isSelected) vmaTheme.primary else cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) vmaTheme.primary else vmaTheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) vmaTheme.primary else vmaTheme.textPrimary)
                                    Text(folder.absolutePath, fontSize = 10.sp, color = vmaTheme.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Đường dẫn lưu hiện tại:", fontSize = 11.5.sp, color = vmaTheme.textMuted)
                    OutlinedTextField(
                        value = localPathInput,
                        onValueChange = { localPathInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val dir = File(localPathInput)
                                FileManagerHelper.openFolderInFileManager(context, dir)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xem thư mục", fontSize = 11.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn từ máy", fontSize = 11.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePvrConfig(destinationInput, localPathInput, driveTokenInput, keepLocalCopyInput, autoStopMinutesInput)
                        activeSheet = SettingsSubSheet.NONE
                    }
                ) {
                    Text("Lưu đường dẫn")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Đóng")
                }
            }
        )
    }

    // 5. Google Drive Sheet (Google Service on device)
    if (activeSheet == SettingsSubSheet.GOOGLE_DRIVE) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Dịch vụ Google Drive", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isGoogleDriveSignedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(googleDriveUserName.ifBlank { "Tài khoản Google" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(googleDriveUserEmail, fontSize = 12.sp, color = Color(0xFF38BDF8))
                            }
                        }
                        Text("Dung lượng khả dụng: ${googleDriveQuota.ifBlank { "12.4 GB / 15.0 GB" }}", fontSize = 11.sp, color = vmaTheme.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onSignOutGoogleDrive,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đăng xuất tài khoản")
                        }
                    } else {
                        Text("Liên kết trực tiếp với Google Services của thiết bị để tự động sao lưu bản ghi lên Google Drive:", fontSize = 12.sp, color = vmaTheme.textMuted)
                        Button(
                            onClick = {
                                val pickerIntent = FileManagerHelper.createGoogleAccountPickerIntent()
                                if (pickerIntent != null) {
                                    try {
                                        accountPickerLauncher.launch(pickerIntent)
                                    } catch (_: Exception) {
                                        showGoogleSignInDialog = true
                                    }
                                } else {
                                    showGoogleSignInDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chọn tài khoản Google trên máy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showGoogleSignInDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đăng nhập tài khoản khác", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Xong")
                }
            }
        )
    }

    // 6. Auto Stop Timer Sheet
    if (activeSheet == SettingsSubSheet.AUTO_STOP) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Tự động dừng ghi", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to "Không giới hạn (Ghi liên tục)",
                        30 to "30 phút",
                        60 to "60 phút (1 tiếng)",
                        120 to "120 phút (2 tiếng)"
                    ).forEach { (mins, label) ->
                        Surface(
                            onClick = {
                                autoStopMinutesInput = mins
                                onSavePvrConfig(destinationInput, localPathInput, driveTokenInput, keepLocalCopyInput, autoStopMinutesInput)
                                activeSheet = SettingsSubSheet.NONE
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (autoStopMinutesInput == mins) vmaTheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (autoStopMinutesInput == mins) vmaTheme.primary else cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, color = vmaTheme.textPrimary)
                                if (autoStopMinutesInput == mins) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 7. App Style Sheet
    if (activeSheet == SettingsSubSheet.APP_STYLE) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Phong cách hiển thị", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = {
                            onSetAppStyle(AppStyle.MATERIAL_EXPRESSIVE)
                            activeSheet = SettingsSubSheet.NONE
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = vmaTheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, vmaTheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Material 3 Expressive", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Chuẩn Material Design 3 của Google: Mượt mà, tiết kiệm pin, ổn định 100%", fontSize = 12.sp, color = vmaTheme.textMuted)
                            }
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 8. Gemini Config Sheet
    if (activeSheet == SettingsSubSheet.GEMINI_CONFIG) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Cấu hình Gemini AI", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("API Key Google Gemini:", fontSize = 12.sp, color = vmaTheme.textMuted)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            diagnosticResult = null
                        },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = cardBorderColor
                        )
                    )

                    Text("Mô hình AI:", fontSize = 12.sp, color = vmaTheme.textMuted)
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            isTestingConnection = true
                            diagnosticResult = null
                            onTestGeminiConnection(apiKeyInput, selectedModel) { res ->
                                diagnosticResult = res
                                isTestingConnection = false
                            }
                        },
                        enabled = apiKeyInput.isNotBlank() && !isTestingConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đang kiểm tra kết nối...")
                        } else {
                            Text("Kiểm tra kết nối Gemini AI")
                        }
                    }

                    diagnosticResult?.let { diag ->
                        Text(
                            text = if (diag.isSuccess) "✓ ${diag.statusSummary}" else "✗ ${diag.statusSummary}",
                            fontSize = 11.sp,
                            color = if (diag.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveGeminiConfig(apiKeyInput, selectedModel)
                        activeSheet = SettingsSubSheet.NONE
                        Toast.makeText(context, "Đã lưu cấu hình Gemini AI", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Lưu cấu hình")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Đóng")
                }
            }
        )
    }

    // 9. Device Info Sheet
    if (activeSheet == SettingsSubSheet.DEVICE_INFO) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsSubSheet.NONE },
            containerColor = cardBgColor,
            titleContentColor = vmaTheme.textPrimary,
            textContentColor = vmaTheme.textSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Thông tin thiết bị & ứng dụng", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ứng dụng: VMA Live TV (Tư liệu truyền thông Việt Nam)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Trình phát: AndroidX Media3 ExoPlayer 1.4.1", fontSize = 12.sp, color = vmaTheme.textSecondary)
                    Text("Bộ giải mã: Phần cứng + Phần mềm Fallback tự động", fontSize = 12.sp, color = vmaTheme.textSecondary)
                    Text("Hệ điều hành: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})", fontSize = 12.sp, color = vmaTheme.textSecondary)
                    Text("Thiết bị: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontSize = 12.sp, color = vmaTheme.textSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { activeSheet = SettingsSubSheet.NONE }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Liquid Glass warning dialog
    if (showLiquidGlassWarning) {
        AlertDialog(
            onDismissRequest = { showLiquidGlassWarning = false },
            containerColor = cardBgColor,
            titleContentColor = FlowLiveRed,
            textContentColor = vmaTheme.textPrimary,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp))
            },
            title = { Text("Cảnh báo hiệu năng", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hiệu ứng Liquid Glass có thể làm giảm thời lượng pin hoặc khung hình trên các thiết bị cấu hình trung bình. Bạn vẫn muốn bật?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetAppStyle(AppStyle.LIQUID_GLASS)
                        showLiquidGlassWarning = false
                        activeSheet = SettingsSubSheet.NONE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Vẫn bật", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiquidGlassWarning = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Google Sign-In Dialog
    if (showGoogleSignInDialog) {
        GoogleSignInDialog(
            initialEmail = googleDriveUserEmail.ifBlank { "thelucnguyen.tlthvn@gmail.com" },
            initialName = googleDriveUserName.ifBlank { "Nguyễn Thế Lực" },
            onSignInSuccess = { email, name ->
                onSignInGoogleDrive(email, name)
                showGoogleSignInDialog = false
            },
            onDismiss = { showGoogleSignInDialog = false }
        )
    }

    // Dò kênh giả lập khi thêm kênh trong Cài đặt
    if (showScanningDialog) {
        ChannelScanningDialog(
            progress = scanProgress,
            statusText = scanStatus,
            detectedChannels = scanDetectedChannels,
            programCount = scanProgramCount,
            isFinished = isScanFinished,
            onDismiss = { showScanningDialog = false }
        )
    }
}

/**
 * Group container following the exact design of the user's reference screenshot (image.png):
 * - Subdued section header text above the card
 * - Rounded container holding stacked row items with dividers
 */
@Composable
private fun SettingsGroup(
    title: String,
    cardBg: Color,
    cardBorder: Color,
    divider: Color,
    content: @Composable () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = vmaTheme.textMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(0.8.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Clickable row item with left icon, title, optional subtitle, and right chevron (>)
 */
@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.38f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = vmaTheme.textPrimary.copy(alpha = if (enabled) 0.85f else 0.45f),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = vmaTheme.textPrimary.copy(alpha = if (enabled) 1f else 0.55f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = vmaTheme.textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x22888888))
                    .border(BorderStroke(1.dp, Color(0x33888888)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textMuted
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = vmaTheme.textMuted.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Row item with a switch toggle (like Dark Mode or Keep Local Copy)
 */
@Composable
private fun SettingsSwitchRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier.alpha(0.38f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = vmaTheme.textPrimary.copy(alpha = if (enabled) 0.85f else 0.45f),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = vmaTheme.textPrimary.copy(alpha = if (enabled) 1f else 0.55f),
                    fontSize = 15.sp,
                    maxLines = 1
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = vmaTheme.textMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x22888888))
                    .border(BorderStroke(1.dp, Color(0x33888888)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textMuted
                )
            }
        } else {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = vmaTheme.primary
                )
            )
        }
    }
}

private data class SocialPlatform(
    val title: String,
    val handle: String,
    val url: String,
    val badgeText: String,
    val badgeColor: Color
)

@Composable
private fun SocialPlatformRowItem(
    platform: SocialPlatform,
    onClick: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(platform.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platform.badgeText,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platform.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary
                )
                Text(
                    text = platform.handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = vmaTheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Mở liên kết",
            tint = vmaTheme.textMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Static row item displaying key-value information (like Email or Hotline in user screenshot)
 */
@Composable
private fun SettingsStaticRowItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    val vmaTheme = LocalVmaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = vmaTheme.textPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = vmaTheme.textPrimary,
                fontSize = 15.sp
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = vmaTheme.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
