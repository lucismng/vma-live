package com.example.ui.components

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalVmaTheme

/**
 * Google multi-color "G" Icon drawn natively with Vector canvas
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (w / 2f) * 0.95f

        drawCircle(
            color = Color(0xFF4285F4),
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.55f,
            center = Offset(cx, cy)
        )
        val pathRed = Path().apply {
            moveTo(cx, cy)
            lineTo(cx - radius, cy - radius * 0.7f)
            lineTo(cx + radius, cy - radius * 0.7f)
            close()
        }
        drawPath(pathRed, Color(0xFFEA4335))

        val pathYellow = Path().apply {
            moveTo(cx, cy)
            lineTo(cx - radius, cy - radius * 0.7f)
            lineTo(cx - radius, cy + radius * 0.7f)
            close()
        }
        drawPath(pathYellow, Color(0xFFFBBC05))

        val pathGreen = Path().apply {
            moveTo(cx, cy)
            lineTo(cx - radius, cy + radius * 0.7f)
            lineTo(cx + radius, cy + radius * 0.7f)
            close()
        }
        drawPath(pathGreen, Color(0xFF34A853))

        drawCircle(
            color = Color.White,
            radius = radius * 0.52f,
            center = Offset(cx, cy)
        )

        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx - radius * 0.05f, cy - radius * 0.22f),
            size = Size(radius * 1.05f, radius * 0.44f)
        )
    }
}

@Composable
fun GoogleSignInDialog(
    initialEmail: String = "thelucnguyen.tlthvn@gmail.com",
    initialName: String = "Nguyễn Thế Lực",
    onSignInSuccess: (email: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    var isSigningIn by remember { mutableStateOf(false) }
    var showCustomAccountInput by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(0) }

    // Launcher for system Google Account Picker (Google Play Services / Android Account Manager)
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                customEmail = accountName
                customName = accountName.substringBefore("@")
                selectedAccount = 1
                showCustomAccountInput = true
                onSignInSuccess(accountName, customName)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSigningIn) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("google_signin_dialog"),
            colors = CardDefaults.cardColors(containerColor = vmaTheme.cardBackground),
            border = BorderStroke(1.dp, vmaTheme.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Google Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            GoogleLogoIcon(modifier = Modifier.size(26.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Đăng nhập với Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vmaTheme.textPrimary
                        )
                        Text(
                            text = "Chọn tài khoản để lưu trữ Google Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = vmaTheme.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Account Selection List
                Text(
                    text = "TÀI KHOẢN GOOGLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = vmaTheme.textMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Default Primary Device Account Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedAccount == 0) Color(0xFF4285F4).copy(alpha = 0.12f) else vmaTheme.surface,
                    border = BorderStroke(
                        if (selectedAccount == 0) 1.5.dp else 1.dp,
                        if (selectedAccount == 0) Color(0xFF4285F4) else vmaTheme.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedAccount = 0
                            showCustomAccountInput = false
                        }
                        .testTag("google_account_tile_primary")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEA4335))
                        ) {
                            Text(
                                text = initialName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = initialName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = vmaTheme.textPrimary
                            )
                            Text(
                                text = initialEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = vmaTheme.textSecondary
                            )
                        }

                        if (selectedAccount == 0) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Đã chọn",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Choose directly from device's Google Accounts (Google Play Services / Android Account Manager)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val pickerIntent = com.example.util.FileManagerHelper.createGoogleAccountPickerIntent()
                            if (pickerIntent != null) {
                                try {
                                    accountPickerLauncher.launch(pickerIntent)
                                } catch (_: Exception) {
                                    selectedAccount = 1
                                    showCustomAccountInput = true
                                }
                            } else {
                                selectedAccount = 1
                                showCustomAccountInput = true
                            }
                        }
                        .testTag("google_account_picker_device_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chọn tài khoản Google trên máy",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = vmaTheme.textPrimary
                            )
                            Text(
                                text = "Liên kết trực tiếp với Google Services của thiết bị",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Use another account option
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedAccount == 1) Color(0xFF4285F4).copy(alpha = 0.12f) else vmaTheme.surface,
                    border = BorderStroke(
                        if (selectedAccount == 1) 1.5.dp else 1.dp,
                        if (selectedAccount == 1) Color(0xFF4285F4) else vmaTheme.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedAccount = 1
                            showCustomAccountInput = true
                        }
                        .testTag("google_account_tile_custom")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (customEmail.isNotBlank()) (customName.ifBlank { "Tài khoản Google khác" }) else "Sử dụng tài khoản Google khác",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = vmaTheme.textPrimary
                            )
                            Text(
                                text = if (customEmail.isNotBlank()) customEmail else "Nhập email Google Drive của bạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = vmaTheme.textSecondary
                            )
                        }

                        if (selectedAccount == 1) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Đã chọn",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (showCustomAccountInput) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        placeholder = { Text("nhap_email@gmail.com", fontSize = 13.sp) },
                        label = { Text("Địa chỉ Gmail") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = vmaTheme.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gmail_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Tên hiển thị (ví dụ: Thế Lực)", fontSize = 13.sp) },
                        label = { Text("Tên người dùng") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = vmaTheme.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Drive Scope & Privacy explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Quyền truy cập Google Drive an toàn:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "• Tạo và lưu video PVR vào thư mục 'VMA_TV_Recordings'\n• Không truy cập bất kỳ tệp cá nhân hay tài liệu nào khác của bạn",
                                fontSize = 10.sp,
                                color = vmaTheme.textSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSigningIn
                    ) {
                        Text("Hủy", color = vmaTheme.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            isSigningIn = true
                            val targetEmail = if (selectedAccount == 1 && customEmail.isNotBlank()) {
                                customEmail.trim()
                            } else {
                                initialEmail
                            }
                            val targetName = if (selectedAccount == 1 && customName.isNotBlank()) {
                                customName.trim()
                            } else if (selectedAccount == 1) {
                                targetEmail.substringBefore("@")
                            } else {
                                initialName
                            }
                            onSignInSuccess(targetEmail, targetName)
                        },
                        enabled = !isSigningIn && (selectedAccount == 0 || (selectedAccount == 1 && customEmail.contains("@"))),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_google_signin_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang kết nối...", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đăng nhập và Liên kết",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
