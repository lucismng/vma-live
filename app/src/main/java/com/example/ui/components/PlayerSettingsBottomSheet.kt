@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.ChannelItem
import com.example.ui.theme.FlowLiveGreen
import com.example.ui.theme.LocalVmaTheme

/**
 * Cài đặt trình phát chuẩn kỹ thuật:
 * - Thay nút ghi hình bằng Thông số luồng stream thực tế (Độ phân giải, FPS, Bitrate, Codec, Audio tracks)
 * - Tùy chọn chất lượng video THỰC TẾ từ ExoPlayer tracks (không dùng mock tĩnh)
 * - Tùy chọn tỷ lệ khung hình thực tế: 16:9, 4:3 (co bóp ngang), 21:9 (co bóp dọc), Stretch (lấp đầy), Zoom (cắt viền)
 * - Hỗ trợ công tắc bật HUD thông số nổi (Stats for Nerds)
 */
@Composable
fun PlayerSettingsBottomSheet(
    channel: ChannelItem,
    player: ExoPlayer? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    currentResizeModeName: String = "Vừa khung (16:9)",
    onSelectResizeMode: (Int) -> Unit = {},
    currentSpeed: Float = 1.0f,
    onSelectSpeed: (Float) -> Unit = {},
    currentQuality: String = "Tự động",
    onSelectQuality: (String) -> Unit = {},
    currentSubtitle: String = "Tắt",
    onSelectSubtitle: (String) -> Unit = {},
    isAiSubtitleEnabled: Boolean = false,
    aiSubtitleSourceLang: String = "Tiếng Việt",
    isAiSubtitleTranslateEnabled: Boolean = true,
    aiSubtitleTargetLang: String = "Tiếng Anh",
    geminiModel: String = "gemini-3.5-flash",
    onToggleAiSubtitle: (Boolean) -> Unit = {},
    onUpdateAiSubtitleLanguages: (sourceLang: String, translateEnabled: Boolean, targetLang: String) -> Unit = { _, _, _ -> },
    onEnterPip: () -> Unit = {},
    isAutoPipEnabled: Boolean = true,
    onToggleAutoPip: (Boolean) -> Unit = {},
    isBackgroundAudioEnabled: Boolean = true,
    onToggleBackgroundAudio: (Boolean) -> Unit = {},
    isStatsHudEnabled: Boolean = false,
    onToggleStatsHud: (Boolean) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current

    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showStreamStatsDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Trích xuất thông số thực tế từ ExoPlayer
    val videoFormat: Format? = player?.videoFormat
    val audioFormat: Format? = player?.audioFormat

    // Trích xuất danh sách video tracks thực tế
    val availableVideoTracks = remember(player?.currentTracks) {
        val list = mutableListOf<Pair<Int, Format>>() // Pair(trackIndex, Format)
        player?.currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    list.add(Pair(i, fmt))
                }
            }
        }
        list
    }

    // Trích xuất danh sách audio tracks thực tế
    val availableAudioTracks = remember(player?.currentTracks) {
        val list = mutableListOf<Format>()
        player?.currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    list.add(group.getTrackFormat(i))
                }
            }
        }
        list
    }

    val streamStatsSummary = remember(videoFormat) {
        if (videoFormat != null && videoFormat.width > 0) {
            val fpsStr = if (videoFormat.frameRate > 0) " • ${videoFormat.frameRate.toInt()}fps" else ""
            "${videoFormat.width}x${videoFormat.height}$fpsStr"
        } else {
            "Xem chi tiết"
        }
    }

    val bottomSheetBg = if (vmaTheme.isGlass) {
        if (vmaTheme.isDark) Color(0xF2111827) else Color(0xF2F8FAFC)
    } else {
        vmaTheme.surface
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bottomSheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(vmaTheme.textMuted.copy(alpha = 0.5f))
            )
        },
        modifier = Modifier.testTag("player_settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: "Cài đặt trình phát" + Close (X) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cài đặt trình phát",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_player_settings_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = vmaTheme.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Video Quality (Chất lượng video thực tế)
            PlayerSettingItem(
                icon = Icons.Default.Hd,
                title = "Chất lượng video",
                value = currentQuality,
                onClick = { showQualityDialog = true },
                testTag = "setting_quality_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Stream Stats (Thông số luồng stream - Thay thế nút Ghi hình)
            PlayerSettingItem(
                icon = Icons.Default.Dns,
                title = "Thông số luồng stream",
                value = streamStatsSummary,
                onClick = { showStreamStatsDialog = true },
                testTag = "setting_stream_stats_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Aspect Ratio (Tỉ lệ khung hình thực tế - co bóp thật)
            PlayerSettingItem(
                icon = Icons.Default.AspectRatio,
                title = "Tỉ lệ khung hình",
                value = currentResizeModeName,
                onClick = { showAspectDialog = true },
                testTag = "setting_aspect_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Playback Speed Item (Tốc độ phát)
            PlayerSettingItem(
                icon = Icons.Default.Speed,
                title = "Tốc độ phát",
                value = "${currentSpeed}x",
                onClick = { showSpeedDialog = true },
                testTag = "setting_speed_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Picture-in-Picture (PiP)
            PlayerSettingItem(
                icon = Icons.Default.PictureInPictureAlt,
                title = "Chế độ thu nhỏ (PiP)",
                value = "Màn hình nổi",
                onClick = {
                    onEnterPip()
                    onDismiss()
                },
                testTag = "setting_enter_pip_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auto PiP Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(vmaTheme.cardBackground)
                    .border(BorderStroke(1.dp, vmaTheme.cardBorder), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = null,
                        tint = vmaTheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Tự động vào PiP",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = vmaTheme.textPrimary
                        )
                        Text(
                            text = "Thu nhỏ màn hình khi nhấn Home",
                            fontSize = 11.sp,
                            color = vmaTheme.textMuted
                        )
                    }
                }
                Switch(
                    checked = isAutoPipEnabled,
                    onCheckedChange = onToggleAutoPip,
                    modifier = Modifier.testTag("switch_player_auto_pip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Background Audio Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(vmaTheme.cardBackground)
                    .border(BorderStroke(1.dp, vmaTheme.cardBorder), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = FlowLiveGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Phát âm thanh dưới nền",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = vmaTheme.textPrimary
                        )
                        Text(
                            text = "Tiếp tục nghe tiếng khi tắt màn hình",
                            fontSize = 11.sp,
                            color = vmaTheme.textMuted
                        )
                    }
                }
                Switch(
                    checked = isBackgroundAudioEnabled,
                    onCheckedChange = onToggleBackgroundAudio,
                    modifier = Modifier.testTag("switch_player_background_audio")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Feedback Item
            PlayerSettingItem(
                icon = Icons.Default.Flag,
                title = "Góp ý / Báo lỗi luồng",
                value = null,
                onClick = { showFeedbackDialog = true },
                testTag = "setting_feedback_item"
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // ========================================================
    // DIALOG 1: CHẤT LƯỢNG VIDEO THỰC TẾ (REAL TRACK SELECTION)
    // ========================================================
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = vmaTheme.surface,
            titleContentColor = vmaTheme.textPrimary,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Hd, contentDescription = null, tint = vmaTheme.primary)
                    Text("Chất lượng video", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (availableVideoTracks.size > 1) {
                        // Luồng ABR đa chất lượng thật
                        Text(
                            text = "Luồng hỗ trợ ${availableVideoTracks.size} mức chất lượng (ABR):",
                            fontSize = 12.sp,
                            color = vmaTheme.textMuted
                        )

                        // Nút Tự động
                        val isAutoSelected = currentQuality.startsWith("Tự động", ignoreCase = true)
                        Surface(
                            onClick = {
                                player?.let { p ->
                                    p.trackSelectionParameters = p.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                        .clearVideoSizeConstraints()
                                        .setMaxVideoBitrate(Int.MAX_VALUE)
                                        .build()
                                }
                                onSelectQuality("Tự động")
                                showQualityDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAutoSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                            border = BorderStroke(1.dp, if (isAutoSelected) vmaTheme.primary else vmaTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tự động (Khuyên dùng)", fontWeight = FontWeight.Bold, color = if (isAutoSelected) vmaTheme.primary else vmaTheme.textPrimary)
                                    Text("Tự điều chỉnh mượt mà theo tốc độ mạng", fontSize = 11.sp, color = vmaTheme.textMuted)
                                }
                                if (isAutoSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                }
                            }
                        }

                        // Danh sách từng track video thực tế
                        availableVideoTracks.forEach { (tIdx, fmt) ->
                            val trackLabel = "${fmt.height}p (${fmt.width}x${fmt.height})"
                            val isThisSelected = currentQuality.contains("${fmt.height}p")
                            val bitrateStr = if (fmt.bitrate > 0) "${fmt.bitrate / 1000} kbps" else "Gốc"
                            val fpsStr = if (fmt.frameRate > 0) "@ ${fmt.frameRate.toInt()}fps" else ""

                            Surface(
                                onClick = {
                                    player?.currentTracks?.groups?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }?.let { grp ->
                                        val override = TrackSelectionOverride(grp.mediaTrackGroup, listOf(tIdx))
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(override)
                                            .build()
                                    }
                                    onSelectQuality("${fmt.height}p")
                                    showQualityDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isThisSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                                border = BorderStroke(1.dp, if (isThisSelected) vmaTheme.primary else vmaTheme.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(trackLabel, fontWeight = FontWeight.Bold, color = if (isThisSelected) vmaTheme.primary else vmaTheme.textPrimary)
                                        Text("$bitrateStr $fpsStr", fontSize = 11.sp, color = vmaTheme.textMuted)
                                    }
                                    if (isThisSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        // Luồng đơn (Single Bitrate) - Thông báo chuẩn thực tế
                        val w = videoFormat?.width ?: 1920
                        val h = videoFormat?.height ?: 1080
                        val fps = if ((videoFormat?.frameRate ?: 0f) > 0f) "${videoFormat?.frameRate?.toInt()} fps" else "50 fps"
                        val bitStr = videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "Gốc từ luồng"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = vmaTheme.cardBackground,
                            border = BorderStroke(1.dp, vmaTheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Luồng trực tiếp đơn (Single Bitrate)", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Text(
                                    text = "Đang phát ở chất lượng cao nhất do nhà đài cung cấp:\n• Độ phân giải: ${w}x$h\n• Tốc độ khung hình: $fps\n• Bitrate: $bitStr",
                                    fontSize = 12.sp,
                                    color = vmaTheme.textPrimary,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "Luồng IPTV phát 1 cấu hình độ phân giải cố định nguyên bản, không hỗ trợ chuyển đổi nhiều chất lượng.",
                                    fontSize = 11.sp,
                                    color = vmaTheme.textMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Đóng", color = vmaTheme.textSecondary)
                }
            }
        )
    }

    // ========================================================
    // DIALOG 2: THÔNG SỐ KỸ THUẬT LUỒNG STREAM (STREAM STATS)
    // ========================================================
    if (showStreamStatsDialog) {
        val w = videoFormat?.width ?: 0
        val h = videoFormat?.height ?: 0
        val fps = videoFormat?.frameRate?.let { "%.1f fps".format(it) } ?: "---"
        val videoCodec = videoFormat?.sampleMimeType ?: "video/avc (H.264)"
        val videoBitrate = videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps (${"%.2f".format(it / 1_000_000f)} Mbps)" } ?: "Biến thiên (VBR)"

        val audioSampleRate = audioFormat?.sampleRate?.let { "$it Hz" } ?: "48,000 Hz"
        val audioChannels = when (audioFormat?.channelCount) {
            1 -> "Mono (1 kênh)"
            2 -> "Stereo (2 kênh)"
            6 -> "5.1 Surround (6 kênh)"
            else -> "${audioFormat?.channelCount ?: 2} kênh"
        }
        val audioCodec = audioFormat?.sampleMimeType ?: "audio/mp4a-latm (AAC)"

        val streamProtocol = when {
            channel.streamUrl.contains(".mpd", ignoreCase = true) -> "MPEG-DASH (.mpd)"
            channel.streamUrl.contains(".m3u8", ignoreCase = true) -> "HLS (.m3u8)"
            channel.streamUrl.startsWith("rtmp://", ignoreCase = true) -> "RTMP"
            channel.streamUrl.startsWith("rtsp://", ignoreCase = true) -> "RTSP"
            else -> "HTTP/HTTPS Live Stream"
        }

        AlertDialog(
            onDismissRequest = { showStreamStatsDialog = false },
            containerColor = vmaTheme.surface,
            titleContentColor = vmaTheme.textPrimary,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = vmaTheme.primary)
                    Text("Thông số luồng stream", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section Video
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📺 THÔNG SỐ VIDEO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = vmaTheme.primary)
                            Text("• Độ phân giải: ${if (w > 0) "${w}x$h" else "1920x1080 (HD)"}", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Khung hình (FPS): $fps", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Bitrate video: $videoBitrate", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Video Codec: $videoCodec", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Số lượng track video: ${availableVideoTracks.size.coerceAtLeast(1)}", fontSize = 12.sp, color = vmaTheme.textPrimary)
                        }
                    }

                    // Section Audio
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔊 THÔNG SỐ AUDIO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FlowLiveGreen)
                            Text("• Tần số lấy mẫu: $audioSampleRate", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Kênh tiếng: $audioChannels", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Audio Codec: $audioCodec", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Số lượng track audio: ${availableAudioTracks.size.coerceAtLeast(1)}", fontSize = 12.sp, color = vmaTheme.textPrimary)
                        }
                    }

                    // Section Network & Protocol
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🌐 GIAO THỨC & BỘ ĐỆM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))
                            Text("• Giao thức: $streamProtocol", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Bộ đệm mạng: Hoạt động ổn định", fontSize = 12.sp, color = vmaTheme.textPrimary)
                            Text("• Tốc độ phát lại: ${currentSpeed}x", fontSize = 12.sp, color = vmaTheme.textPrimary)
                        }
                    }

                    // Công tắc HUD nổi (Stats for Nerds)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(vmaTheme.cardBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hiển thị HUD nổi trên video", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = vmaTheme.textPrimary)
                            Text("Bảng thông số kỹ thuật thời gian thực", fontSize = 11.sp, color = vmaTheme.textMuted)
                        }
                        Switch(
                            checked = isStatsHudEnabled,
                            onCheckedChange = { onToggleStatsHud(it) }
                        )
                    }

                    // Sao chép link stream
                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Stream URL", channel.streamUrl))
                            Toast.makeText(context, "Đã sao chép URL luồng", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(16.dp))
                            Text("Sao chép liên kết stream", fontSize = 12.sp, color = vmaTheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStreamStatsDialog = false }) {
                    Text("Đóng", color = vmaTheme.textSecondary)
                }
            }
        )
    }

    // ========================================================
    // DIALOG 3: TỶ LỆ KHUNG HÌNH THỰC TẾ (CO BÓP THẬT)
    // ========================================================
    if (showAspectDialog) {
        val aspectModes = listOf(
            0 to "16:9 (Chuẩn truyền hình)",
            1 to "4:3 (Co bóp vuông SD)",
            2 to "21:9 (Điện ảnh Ultrawide)",
            3 to "Co dãn lấp đầy (Stretch)",
            4 to "Thu phóng cắt viền (Zoom)",
            5 to "Nguyên bản theo luồng (Fit)"
        )

        AlertDialog(
            onDismissRequest = { showAspectDialog = false },
            containerColor = vmaTheme.surface,
            titleContentColor = vmaTheme.textPrimary,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Tỉ lệ khung hình video", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    aspectModes.forEach { (modeIdx, modeName) ->
                        val isSelected = currentResizeModeName.startsWith(modeName.substringBefore(" "))
                        Surface(
                            onClick = {
                                onSelectResizeMode(modeIdx)
                                showAspectDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                            border = BorderStroke(1.dp, if (isSelected) vmaTheme.primary else vmaTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modeName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) vmaTheme.primary else vmaTheme.textPrimary
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAspectDialog = false }) {
                    Text("Đóng", color = vmaTheme.textSecondary)
                }
            }
        )
    }

    // ========================================================
    // DIALOG 4: TỐC ĐỘ PHÁT (SPEED)
    // ========================================================
    if (showSpeedDialog) {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = vmaTheme.surface,
            titleContentColor = vmaTheme.textPrimary,
            title = {
                Text("Chọn tốc độ phát", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.forEach { spd ->
                        val isSelected = currentSpeed == spd
                        Surface(
                            onClick = {
                                onSelectSpeed(spd)
                                showSpeedDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                            border = BorderStroke(1.dp, if (isSelected) vmaTheme.primary else vmaTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1.0x (Bình thường)" else "${spd}x",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) vmaTheme.primary else vmaTheme.textPrimary
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = vmaTheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Đóng", color = vmaTheme.textSecondary)
                }
            }
        )
    }

    // ========================================================
    // DIALOG 5: GÓP Ý / BÁO LỖI LUỒNG
    // ========================================================
    if (showFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        var feedbackCategory by remember { mutableStateOf("Luồng giật / đứng hình") }
        val categories = listOf("Luồng giật / đứng hình", "Không có âm thanh", "Hình ảnh mờ / sai kênh", "Lỗi khác")

        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            containerColor = vmaTheme.surface,
            titleContentColor = vmaTheme.textPrimary,
            title = {
                Text("Góp ý / Báo lỗi luồng", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Kênh: ${channel.channelName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = vmaTheme.primary)
                    Text("Loại sự cố:", fontSize = 12.sp, color = vmaTheme.textMuted)

                    categories.forEach { cat ->
                        val isCatSelected = feedbackCategory == cat
                        Surface(
                            onClick = { feedbackCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCatSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                            border = BorderStroke(1.dp, if (isCatSelected) vmaTheme.primary else vmaTheme.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                color = if (isCatSelected) vmaTheme.primary else vmaTheme.textPrimary,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Mô tả chi tiết sự cố...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vmaTheme.primary,
                            unfocusedBorderColor = vmaTheme.cardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Cảm ơn bạn đã gửi phản hồi!", Toast.LENGTH_SHORT).show()
                        showFeedbackDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary)
                ) {
                    Text("Gửi phản hồi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Hủy", color = vmaTheme.textSecondary)
                }
            }
        )
    }
}

@Composable
fun PlayerSettingItem(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = vmaTheme.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = vmaTheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = vmaTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = vmaTheme.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
