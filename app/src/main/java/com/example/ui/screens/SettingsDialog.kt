package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.gemini.GeminiAiService
import com.example.data.remote.gemini.GeminiDiagnosticResult
import com.example.data.repository.TvRepository
import com.example.model.SyncState
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.components.VmaFooterBrand
import com.example.ui.components.liquidGlass
import com.example.ui.theme.AppStyle
import com.example.ui.theme.FlowLiveGreen
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    initialM3uUrl: String,
    initialEpgUrl: String,
    syncState: SyncState,
    geminiApiKey: String = "",
    geminiModel: String = "gemini-3.5-flash-lite",
    isDarkMode: Boolean = true,
    appStyle: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
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
    onToggleDarkMode: () -> Unit = {},
    onSetAppStyle: (AppStyle) -> Unit = {},
    onSaveGeminiConfig: (apiKey: String, model: String) -> Unit = { _, _ -> },
    onTestGeminiConnection: (apiKey: String, model: String, (GeminiDiagnosticResult) -> Unit) -> Unit = { _, _, _ -> },
    onSavePvrConfig: (destination: String, localPath: String, driveToken: String, keepLocalCopy: Boolean, autoStopMinutes: Int) -> Unit = { _, _, _, _, _ -> },
    onTestGoogleDriveConnection: (token: String, (com.example.data.remote.drive.DriveConnectionStatus) -> Unit) -> Unit = { _, _ -> },
    onSignInGoogleDrive: (email: String, name: String) -> Unit = { _, _ -> },
    onSignOutGoogleDrive: () -> Unit = {},
    onSaveAndSync: (m3uUrl: String, epgUrl: String) -> Unit,
    onSyncM3uOnly: () -> Unit,
    onSyncEpgOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    var m3uInput by remember { mutableStateOf(initialM3uUrl) }
    var epgInput by remember { mutableStateOf(initialEpgUrl) }
    var apiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var selectedModel by remember { mutableStateOf(geminiModel) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var diagnosticResult by remember { mutableStateOf<GeminiDiagnosticResult?>(null) }
    var showRawDetails by remember { mutableStateOf(false) }

    // PVR & Stream Recording State
    var destinationInput by remember { mutableStateOf(pvrTargetDestination) }
    var localPathInput by remember { mutableStateOf(pvrLocalPath.ifBlank { defaultRecordingsPath }) }
    var driveTokenInput by remember { mutableStateOf(googleDriveToken) }
    var isDriveTokenVisible by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var showAdvancedDriveSettings by remember { mutableStateOf(false) }
    var keepLocalCopyInput by remember { mutableStateOf(pvrKeepLocalCopy) }
    var autoStopMinutesInput by remember { androidx.compose.runtime.mutableIntStateOf(pvrAutoStopMinutes) }
    var isTestingDrive by remember { mutableStateOf(false) }
    var driveStatus by remember { mutableStateOf<com.example.data.remote.drive.DriveConnectionStatus?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val lastSyncFormatted = remember(syncState.lastSyncTime) {
        if (syncState.lastSyncTime > 0) dateFormat.format(Date(syncState.lastSyncTime)) else "Chưa đồng bộ"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (vmaTheme.isGlass) {
            if (vmaTheme.isDark) Color(0xF5111827) else Color(0xF5F8FAFC)
        } else {
            vmaTheme.surface
        },
        titleContentColor = vmaTheme.textPrimary,
        textContentColor = vmaTheme.textSecondary,
        shape = vmaTheme.cardShape,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = vmaTheme.primary
                )
                Text(
                    text = "Cài đặt & Giao diện",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Giao diện & Trải nghiệm
                Text(
                    text = "Giao diện & Hiển thị",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dark/Light Mode Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(vmaTheme.cardBackground)
                        .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isDarkMode) "Chế độ Tối (Dark)" else "Chế độ Sáng (Light)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = vmaTheme.textPrimary
                            )
                            Text(
                                text = if (isDarkMode) "Bảo vệ mắt khi xem đêm" else "Rõ ràng, sáng sủa ban ngày",
                                style = MaterialTheme.typography.labelSmall,
                                color = vmaTheme.textSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App Style Selection: Liquid Glass vs Material 3 Expressive
                // (Only located inside Settings, never exposed on the main screen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(vmaTheme.cardBackground)
                        .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = vmaTheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Phong cách thiết kế (Chỉ trong Cài đặt)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = vmaTheme.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phong cách thiết kế chuẩn Material 3 Expressive mượt mà & tiết kiệm pin",
                        style = MaterialTheme.typography.labelSmall,
                        color = vmaTheme.textMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        onClick = {
                            onSetAppStyle(AppStyle.MATERIAL_EXPRESSIVE)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = vmaTheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, vmaTheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_m3_expressive_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = vmaTheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material 3 Expressive",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = vmaTheme.primary
                                )
                                Text(
                                    text = "Mượt mà, siêu nhẹ, tiết kiệm pin tối đa",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = vmaTheme.textMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = vmaTheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Google Gemini AI Configuration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Trí tuệ nhân tạo Google Gemini",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = vmaTheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(vmaTheme.cardBackground)
                        .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Google Gemini API Key",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = vmaTheme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            diagnosticResult = null
                        },
                        placeholder = { Text("Nhập AI Studio API Key (AIzaSy...)", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Ẩn/hiện API Key",
                                    tint = vmaTheme.textMuted
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = vmaTheme.cardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mô hình Gemini (Model)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = vmaTheme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        placeholder = { Text("gemini-3.5-flash-lite", color = vmaTheme.textMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_model_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = vmaTheme.cardBorder,
                            focusedTextColor = vmaTheme.textPrimary,
                            unfocusedTextColor = vmaTheme.textPrimary,
                            focusedContainerColor = vmaTheme.cardBackground,
                            unfocusedContainerColor = vmaTheme.cardBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Gợi ý nhanh mô hình:",
                        style = MaterialTheme.typography.labelSmall,
                        color = vmaTheme.textMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "gemini-3.5-flash-lite" to "3.5 Flash Lite (Mặc định)",
                            "gemini-3.5-flash" to "3.5 Flash",
                            "gemini-2.5-flash" to "2.5 Flash",
                            "gemini-3.1-pro-preview" to "3.1 Pro"
                        ).forEach { (mId, mLabel) ->
                            val isSelected = selectedModel.trim().equals(mId, ignoreCase = true) ||
                                    (mId == "gemini-3.5-flash-lite" && (selectedModel.isBlank() || selectedModel.trim().equals("3.5 flash lite", ignoreCase = true)))
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedModel = mId },
                                label = { Text(mLabel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFF38BDF8)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Test Connection Button
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            diagnosticResult = null
                            onTestGeminiConnection(apiKeyInput, selectedModel) { res ->
                                isTestingConnection = false
                                diagnosticResult = res
                                if (res.isSuccess) {
                                    onSaveGeminiConfig(apiKeyInput, res.normalizedModel)
                                }
                            }
                        },
                        enabled = !isTestingConnection && apiKeyInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_gemini_api_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang chạy kiểm tra chẩn đoán...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chẩn đoán & Kiểm tra Gemini API", fontSize = 12.sp)
                        }
                    }

                    // Diagnostic Feedback Card
                    if (diagnosticResult != null) {
                        val diag = diagnosticResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (diag.isSuccess) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF450A0A).copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (diag.isSuccess) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFEF4444).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_diagnostic_result_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Status header row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (diag.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (diag.isSuccess) Color(0xFF34D399) else Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = diag.statusSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diag.isSuccess) Color(0xFF34D399) else Color(0xFFF87171)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Telemetry Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    // Latency Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${diag.latencyMs} ms",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Model Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = vmaTheme.cardBackground
                                    ) {
                                        Text(
                                            text = diag.normalizedModel,
                                            color = vmaTheme.textPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // HTTP Code
                                    if (diag.httpStatusCode != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (diag.isSuccess) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "HTTP ${diag.httpStatusCode}",
                                                color = if (diag.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Error details
                                if (!diag.isSuccess && !diag.errorDetails.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = diag.errorDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp
                                    )
                                }

                                // Suggested Actions (Troubleshooting tips)
                                if (diag.suggestedActions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Gợi ý khắc phục:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = vmaTheme.textPrimary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    diag.suggestedActions.forEach { action ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 1.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Text(
                                                text = action,
                                                color = vmaTheme.textSecondary,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                // Quick fix button if user entered problematic model
                                if (!diag.isSuccess && diag.normalizedModel != "gemini-3.5-flash-lite") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            selectedModel = "gemini-3.5-flash-lite"
                                        },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Đặt lại về gemini-3.5-flash-lite",
                                            fontSize = 11.sp,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                // Toggle raw snippet
                                if (!diag.rawResponseSnippet.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = { showRawDetails = !showRawDetails },
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text(
                                                text = if (showRawDetails) "Ẩn chi tiết phản hồi" else "Xem chi tiết phản hồi",
                                                fontSize = 10.sp,
                                                color = vmaTheme.textMuted
                                            )
                                        }
                                    }
                                    if (showRawDetails) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = diag.rawResponseSnippet,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: PVR & Stream Recording (Local vs Google Drive)
                Text(
                    text = "Ghi hình Stream & PVR",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ghi nguyên trạng luồng stream video (.ts) và lưu vào bộ nhớ máy hoặc Google Drive",
                    style = MaterialTheme.typography.bodySmall,
                    color = vmaTheme.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(vmaTheme.cardBackground)
                        .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Storage Destination selection
                        Text(
                            text = "Nơi lưu trữ bản ghi:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = vmaTheme.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = destinationInput == "LOCAL",
                                onClick = { destinationInput = "LOCAL" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Bộ nhớ máy") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vmaTheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = vmaTheme.primary
                                )
                            )

                            FilterChip(
                                selected = destinationInput == "GOOGLE_DRIVE",
                                onClick = { destinationInput = "GOOGLE_DRIVE" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Google Drive ☁️") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFF38BDF8)
                                )
                            )
                        }

                        // Local Storage Path Input
                        if (destinationInput == "LOCAL" || keepLocalCopyInput) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Đường dẫn lưu trữ trên thiết bị:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = vmaTheme.textSecondary
                                )
                                OutlinedTextField(
                                    value = localPathInput,
                                    onValueChange = { localPathInput = it },
                                    modifier = Modifier.fillMaxWidth().testTag("pvr_local_path_input"),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = vmaTheme.primary,
                                        unfocusedBorderColor = vmaTheme.cardBorder
                                    )
                                )
                                if (defaultRecordingsPath.isNotBlank() && localPathInput != defaultRecordingsPath) {
                                    TextButton(
                                        onClick = { localPathInput = defaultRecordingsPath },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Đặt lại đường dẫn mặc định", fontSize = 11.sp, color = vmaTheme.primary)
                                    }
                                }
                            }
                        }

                        // Google Drive Configuration
                        if (destinationInput == "GOOGLE_DRIVE") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (isGoogleDriveSignedIn) {
                                    // Signed In Account Card
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF0F2942).copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().testTag("google_drive_signed_in_card")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // User Avatar Initial Circle
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEA4335))
                                                ) {
                                                    Text(
                                                        text = (googleDriveUserName.ifBlank { "L" }).take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = googleDriveUserName.ifBlank { "Nguyễn Thế Lực" },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = vmaTheme.textPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = googleDriveUserEmail.ifBlank { "thelucnguyen.tlthvn@gmail.com" },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Storage Quota & Destination Info
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "☁️ Thư mục: VMA_TV_Recordings",
                                                    fontSize = 11.sp,
                                                    color = vmaTheme.textSecondary
                                                )
                                                Text(
                                                    text = googleDriveQuota.ifBlank { "12.4 GB / 15.0 GB" },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Account actions
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showGoogleSignInDialog = true },
                                                    modifier = Modifier.weight(1f).height(34.dp).testTag("switch_google_account_btn")
                                                ) {
                                                    Text("Đổi tài khoản", fontSize = 11.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { onSignOutGoogleDrive() },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = Color(0xFFEF4444)
                                                    ),
                                                    modifier = Modifier.weight(1f).height(34.dp).testTag("signout_google_drive_btn")
                                                ) {
                                                    Text("Đăng xuất", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Not Signed In Card
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = vmaTheme.surface,
                                        border = BorderStroke(1.dp, vmaTheme.cardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Lưu trữ bản ghi lên Google Drive",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = vmaTheme.textPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Đăng nhập bằng tài khoản Google để tự động đồng bộ video bản ghi vào Google Drive của bạn mà không tốn dung lượng thiết bị.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = vmaTheme.textSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Prominent Google Sign-In Button
                                            Button(
                                                onClick = { showGoogleSignInDialog = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .testTag("google_drive_signin_btn")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    GoogleLogoIcon(modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Đăng nhập bằng Google",
                                                        color = Color(0xFF1F2937),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Collapsible Advanced Options (Optional manual token test)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showAdvancedDriveSettings = !showAdvancedDriveSettings }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showAdvancedDriveSettings) "▼ Thu gọn tùy chọn nâng cao" else "▶ Tùy chọn nâng cao (Kiểm tra token thủ công)",
                                        fontSize = 11.sp,
                                        color = vmaTheme.textMuted
                                    )
                                }

                                if (showAdvancedDriveSettings) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = driveTokenInput,
                                            onValueChange = { driveTokenInput = it },
                                            modifier = Modifier.fillMaxWidth().testTag("pvr_drive_token_input"),
                                            placeholder = { Text("Tùy chọn: Nhập mã Access Token riêng...", fontSize = 11.sp) },
                                            singleLine = true,
                                            visualTransformation = if (isDriveTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { isDriveTokenVisible = !isDriveTokenVisible }) {
                                                    Icon(
                                                        imageVector = if (isDriveTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = vmaTheme.textMuted
                                                    )
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = vmaTheme.cardBorder
                                            )
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                isTestingDrive = true
                                                driveStatus = null
                                                onTestGoogleDriveConnection(driveTokenInput) { st ->
                                                    isTestingDrive = false
                                                    driveStatus = st
                                                }
                                            },
                                            enabled = !isTestingDrive && driveTokenInput.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("test_drive_connection_button")
                                        ) {
                                            if (isTestingDrive) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFF38BDF8)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Đang kiểm tra kết nối...", fontSize = 11.sp)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFF38BDF8)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Kiểm tra kết nối Access Token", fontSize = 11.sp)
                                            }
                                        }

                                        driveStatus?.let { st ->
                                            Text(
                                                text = if (st.isConnected) "✓ Kết nối thành công: ${st.userEmail}" else "✗ Lỗi: ${st.errorMessage}",
                                                fontSize = 11.sp,
                                                color = if (st.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                                // Keep local copy toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Giữ bản sao cục bộ trên máy",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = vmaTheme.textPrimary
                                        )
                                        Text(
                                            text = "Vẫn giữ file trên máy sau khi tải lên Google Drive",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = vmaTheme.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = keepLocalCopyInput,
                                        onCheckedChange = { keepLocalCopyInput = it }
                                    )
                                }

                        // Auto-stop limit selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Tự động dừng ghi:",
                                style = MaterialTheme.typography.labelSmall,
                                color = vmaTheme.textSecondary
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "Không giới hạn",
                                    30 to "30 phút",
                                    60 to "60 phút",
                                    120 to "120 phút"
                                ).forEach { (mins, label) ->
                                    FilterChip(
                                        selected = autoStopMinutesInput == mins,
                                        onClick = { autoStopMinutesInput = mins },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = vmaTheme.primary.copy(alpha = 0.25f),
                                            selectedLabelColor = vmaTheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Info Summary Box
                Text(
                    text = "Dữ liệu & Đồng bộ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(vmaTheme.cardBackground)
                        .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tổng số kênh:", style = MaterialTheme.typography.bodySmall, color = vmaTheme.textMuted)
                            Text(text = "${syncState.channelCount} kênh", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = FlowLiveGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tổng số lịch EPG:", style = MaterialTheme.typography.bodySmall, color = vmaTheme.textMuted)
                            Text(text = "${syncState.programCount} chương trình", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = vmaTheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Lần cập nhật cuối:", style = MaterialTheme.typography.bodySmall, color = vmaTheme.textMuted)
                            Text(text = lastSyncFormatted, style = MaterialTheme.typography.bodySmall, color = vmaTheme.textSecondary)
                        }
                        if (!syncState.message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = syncState.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = vmaTheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // M3U URL Input
                Text(
                    text = "Nguồn danh sách kênh M3U:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = m3uInput,
                    onValueChange = { m3uInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_m3u_url"),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = vmaTheme.primary,
                        unfocusedBorderColor = vmaTheme.cardBorder,
                        focusedTextColor = vmaTheme.textPrimary,
                        unfocusedTextColor = vmaTheme.textPrimary,
                        focusedContainerColor = vmaTheme.cardBackground,
                        unfocusedContainerColor = vmaTheme.cardBackground
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { m3uInput = TvRepository.DEFAULT_M3U_URL }
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mặc định", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // EPG URL Input
                Text(
                    text = "Nguồn lịch phát sóng EPG:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = epgInput,
                    onValueChange = { epgInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_epg_url"),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = vmaTheme.primary,
                        unfocusedBorderColor = vmaTheme.cardBorder,
                        focusedTextColor = vmaTheme.textPrimary,
                        unfocusedTextColor = vmaTheme.textPrimary,
                        focusedContainerColor = vmaTheme.cardBackground,
                        unfocusedContainerColor = vmaTheme.cardBackground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick selector for EPG presets
                Text(
                    text = "Tùy chọn nhanh nguồn EPG:",
                    style = MaterialTheme.typography.bodySmall,
                    color = vmaTheme.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = epgInput == TvRepository.DEFAULT_EPG_URL,
                        onClick = { epgInput = TvRepository.DEFAULT_EPG_URL },
                        label = { Text("EPG Nén (.xml.gz)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vmaTheme.primary.copy(alpha = 0.3f),
                            selectedLabelColor = vmaTheme.primary
                        )
                    )
                    FilterChip(
                        selected = epgInput == TvRepository.DETAILED_EPG_URL,
                        onClick = { epgInput = TvRepository.DETAILED_EPG_URL },
                        label = { Text("Chi tiết (epgc.xml)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vmaTheme.primary.copy(alpha = 0.3f),
                            selectedLabelColor = vmaTheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Separate sync actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncM3uOnly,
                        enabled = !syncState.isSyncingM3u,
                        modifier = Modifier.weight(1f).testTag("button_sync_m3u_only")
                    ) {
                        if (syncState.isSyncingM3u) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Đồng bộ M3U", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onSyncEpgOnly,
                        enabled = !syncState.isSyncingEpg,
                        modifier = Modifier.weight(1f).testTag("button_sync_epg_only")
                    ) {
                        if (syncState.isSyncingEpg) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Đồng bộ EPG", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                VmaFooterBrand(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveGeminiConfig(apiKeyInput, selectedModel)
                    onSavePvrConfig(
                        destinationInput,
                        localPathInput,
                        driveTokenInput,
                        keepLocalCopyInput,
                        autoStopMinutesInput
                    )
                    onSaveAndSync(m3uInput, epgInput)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                modifier = Modifier.testTag("button_save_settings")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lưu & Đồng bộ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = vmaTheme.textSecondary)
            }
        }
    )

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
}
