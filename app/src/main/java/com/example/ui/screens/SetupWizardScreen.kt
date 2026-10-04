package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TvRepository
import com.example.model.ChannelItem
import com.example.ui.components.ChannelScanningContent
import com.example.ui.components.VmaLogoImage
import com.example.ui.theme.LocalVmaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class SetupStep {
    WELCOME,
    M3U_SOURCE,
    EPG_SOURCE,
    SCANNING,
    COMPLETED
}

@Composable
fun SetupWizardScreen(
    onFinishSetup: (m3uUrl: String, epgUrl: String) -> Unit,
    onPerformRealScan: suspend (
        m3uUrl: String,
        epgUrl: String,
        onChannelFound: (String) -> Unit,
        onProgramCountUpdated: (Int) -> Unit,
        onProgressUpdated: (Float, String) -> Unit
    ) -> Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(SetupStep.WELCOME) }

    val context = LocalContext.current
    var m3uListName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var localFileName by remember { mutableStateOf<String?>(null) }

    var selectedEpgUrl by remember { mutableStateOf(TvRepository.DEFAULT_EPG_URL) }
    var isCustomEpgSelected by remember { mutableStateOf(false) }
    var customEpgUrl by remember { mutableStateOf("") }

    // Channel Scanning Simulation State
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatusText by remember { mutableStateOf("Đang khởi tạo kết nối...") }
    val detectedChannels = remember { mutableStateListOf<String>() }
    var programCount by remember { mutableIntStateOf(0) }
    var isScanFinished by remember { mutableStateOf(false) }

    // Smooth animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = scanProgress,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "scan_progress"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = vmaTheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Step Indicator (Numbered 1 - 2 - 3 - 4 with slick line connectors)
            StepProgressBar(currentStep = currentStep)

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(300)))
                    }
                },
                label = "step_content_anim",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    SetupStep.WELCOME -> {
                        WelcomeStepContent(
                            onStart = { currentStep = SetupStep.M3U_SOURCE }
                        )
                    }
                    SetupStep.M3U_SOURCE -> {
                        M3uStepContent(
                            listName = m3uListName,
                            onListNameChange = { m3uListName = it },
                            m3uUrl = m3uUrl,
                            onM3uUrlChange = { m3uUrl = it },
                            localFileName = localFileName,
                            onLocalFileNameChange = { localFileName = it },
                            onBack = { currentStep = SetupStep.WELCOME },
                            onNext = {
                                currentStep = SetupStep.EPG_SOURCE
                            }
                        )
                    }
                    SetupStep.EPG_SOURCE -> {
                        EpgStepContent(
                            selectedEpgUrl = selectedEpgUrl,
                            onEpgUrlChange = {
                                selectedEpgUrl = it
                                isCustomEpgSelected = false
                            },
                            isCustomEpgSelected = isCustomEpgSelected,
                            onToggleCustomEpg = { isCustomEpgSelected = true },
                            customEpgUrl = customEpgUrl,
                            onCustomEpgUrlChange = { customEpgUrl = it },
                            onBack = { currentStep = SetupStep.M3U_SOURCE },
                            onStartScan = {
                                currentStep = SetupStep.SCANNING
                            }
                        )
                    }
                    SetupStep.SCANNING, SetupStep.COMPLETED -> {
                        ChannelScanningContent(
                            progress = animatedProgress,
                            statusText = scanStatusText,
                            detectedChannels = detectedChannels,
                            programCount = programCount,
                            isFinished = isScanFinished,
                            onComplete = {
                                val effectiveM3u = m3uUrl.trim()

                                val effectiveEpg = if (isCustomEpgSelected && customEpgUrl.isNotBlank()) {
                                    customEpgUrl.trim()
                                } else {
                                    selectedEpgUrl
                                }

                                onFinishSetup(effectiveM3u, effectiveEpg)
                            }
                        )
                    }
                }
            }
        }
    }

    // Real Channel Scan & EPG Loading (100% Real, zero simulation)
    LaunchedEffect(currentStep) {
        if (currentStep == SetupStep.SCANNING && !isScanFinished) {
            detectedChannels.clear()
            scanProgress = 0.05f
            programCount = 0
            scanStatusText = "Đang kết nối luồng phát và nạp danh sách kênh..."

            val effectiveM3u = m3uUrl.trim()

            val effectiveEpg = if (isCustomEpgSelected && customEpgUrl.isNotBlank()) {
                customEpgUrl.trim()
            } else {
                selectedEpgUrl
            }

            try {
                val (chCount, progCount) = onPerformRealScan(
                    effectiveM3u,
                    effectiveEpg,
                    { channelName ->
                        if (!detectedChannels.contains(channelName)) {
                            detectedChannels.add(channelName)
                        }
                    },
                    { pCount ->
                        programCount = pCount
                    },
                    { progress, status ->
                        scanProgress = progress
                        scanStatusText = status
                    }
                )
                programCount = progCount
                scanProgress = 1.0f
                scanStatusText = "Đã hoàn thành! Nạp được $chCount kênh và $progCount chương trình EPG."
                delay(300)
                isScanFinished = true
                currentStep = SetupStep.COMPLETED
            } catch (e: Exception) {
                scanStatusText = "Đã hoàn tất dò kênh: ${e.localizedMessage}"
                scanProgress = 1.0f
                isScanFinished = true
                currentStep = SetupStep.COMPLETED
            }
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: SetupStep) {
    val vmaTheme = LocalVmaTheme.current
    val stepIndex = when (currentStep) {
        SetupStep.WELCOME -> 0
        SetupStep.M3U_SOURCE -> 1
        SetupStep.EPG_SOURCE -> 2
        SetupStep.SCANNING, SetupStep.COMPLETED -> 3
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 0..3) {
            val isActive = i <= stepIndex
            val isCurrent = i == stepIndex
            val isDone = i < stepIndex || currentStep == SetupStep.COMPLETED

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone || isCurrent) vmaTheme.primary
                        else Color(0x22FFFFFF)
                    )
                    .border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) Color.White else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${i + 1}",
                        color = if (isActive) Color.White else Color(0x88FFFFFF),
                        fontSize = 13.5.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            if (i < 3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .padding(horizontal = 8.dp)
                        .background(
                            if (i < stepIndex) vmaTheme.primary
                            else Color(0x22FFFFFF),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(onStart: () -> Unit) {
    val vmaTheme = LocalVmaTheme.current

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_logo_anim")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Official VMA Brand Logo with smooth breathing animation
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                VmaLogoImage(
                    modifier = Modifier.size(105.dp),
                    contentDescription = "Logo VMA"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Chào mừng bạn đến với VMA Live TV",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = vmaTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ứng dụng xem truyền hình trực tuyến tốc độ cao, hỗ trợ lịch phát sóng điện tử EPG và ghi hình PVR chuyên nghiệp.",
                fontSize = 12.5.sp,
                color = vmaTheme.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Feature cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItemRow(
                    icon = Icons.Default.LiveTv,
                    title = "Kênh truyền hình trực tuyến tốc độ cao",
                    desc = "Hình ảnh sắc nét chuẩn HD/FHD, chuyển kênh mượt mà tức thì"
                )
                FeatureItemRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "Lịch phát sóng EPG thời gian thực",
                    desc = "Đồng bộ chi tiết từng chương trình phát sóng hàng ngày"
                )
                FeatureItemRow(
                    icon = Icons.Default.Videocam,
                    title = "Ghi hình đa kênh đồng thời (PVR)",
                    desc = "Ghi nhiều chương trình yêu thích cùng lúc vào bộ nhớ hoặc Google Drive"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Bắt đầu thiết lập", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FeatureItemRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    val vmaTheme = LocalVmaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AFFFFFF))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(vmaTheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = vmaTheme.textPrimary)
            Text(text = desc, fontSize = 11.sp, color = vmaTheme.textMuted)
        }
    }
}

@Composable
private fun M3uStepContent(
    listName: String,
    onListNameChange: (String) -> Unit,
    m3uUrl: String,
    onM3uUrlChange: (String) -> Unit,
    localFileName: String?,
    onLocalFileNameChange: (String?) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val vmaTheme = LocalVmaTheme.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "playlist.m3u"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
                val localFile = File(context.filesDir, "imported_${System.currentTimeMillis()}.m3u")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (listName.isBlank()) {
                    onListNameChange(displayName.removeSuffix(".m3u").removeSuffix(".m3u8"))
                }
                onM3uUrlChange("file://${localFile.absolutePath}")
                onLocalFileNameChange(displayName)
                Toast.makeText(context, "Đã chọn tệp: $displayName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể đọc tệp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Nhập danh sách phát",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
                Text(
                    text = "Nhập đường dẫn link M3U hoặc chọn tệp từ máy của bạn:",
                    fontSize = 12.5.sp,
                    color = vmaTheme.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 1. Tên danh sách (Tên List)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tên danh sách:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary
                )
                OutlinedTextField(
                    value = listName,
                    onValueChange = onListNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập tên danh sách...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = vmaTheme.primary,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )
            }

            // 2. Link danh sách (Link List)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Đường dẫn link M3U:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary
                )
                OutlinedTextField(
                    value = if (m3uUrl.startsWith("file://")) "" else m3uUrl,
                    onValueChange = {
                        onM3uUrlChange(it)
                        if (localFileName != null) {
                            onLocalFileNameChange(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/playlist.m3u8", fontSize = 12.5.sp) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    onM3uUrlChange(clip.trim())
                                    onLocalFileNameChange(null)
                                    Toast.makeText(context, "Đã dán từ clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Dán link",
                                tint = vmaTheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = vmaTheme.primary,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )
            }

            // Dòng ngăn cách HOẶC
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x22FFFFFF))
                Text("HOẶC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = vmaTheme.textMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x22FFFFFF))
            }

            // 3. Chọn file cục bộ (File cục bộ)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (localFileName != null) vmaTheme.primary.copy(alpha = 0.15f) else Color(0x1AFFFFFF),
                border = BorderStroke(1.2.dp, if (localFileName != null) vmaTheme.primary else Color(0x2AFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filePickerLauncher.launch("*/*")
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (localFileName != null) vmaTheme.primary else Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (localFileName != null) Icons.Default.Check else Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (localFileName != null) "Tệp: $localFileName" else "Chọn tệp từ thiết bị",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (localFileName != null) vmaTheme.primary else vmaTheme.textPrimary
                            )
                            Text(
                                text = if (localFileName != null) "Đã chọn tệp M3U nội bộ thành công" else "Hỗ trợ các tệp .m3u, .m3u8 lưu trên máy",
                                fontSize = 11.5.sp,
                                color = vmaTheme.textMuted
                            )
                        }
                    }

                    if (localFileName != null) {
                        IconButton(
                            onClick = {
                                onLocalFileNameChange(null)
                                onM3uUrlChange("")
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa tệp", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary.copy(alpha = 0.85f))
                        ) {
                            Text("Duyệt...", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quay lại")
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Tiếp theo", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EpgStepContent(
    selectedEpgUrl: String,
    onEpgUrlChange: (String) -> Unit,
    isCustomEpgSelected: Boolean,
    onToggleCustomEpg: () -> Unit,
    customEpgUrl: String,
    onCustomEpgUrlChange: (String) -> Unit,
    onBack: () -> Unit,
    onStartScan: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = "Nguồn lịch phát sóng (EPG)",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )
                Text(
                    text = "EPG giúp hiển thị lịch chiếu chương trình, mô tả và khung giờ phát trực tiếp:",
                    fontSize = 12.5.sp,
                    color = vmaTheme.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            SelectionCard(
                title = "epg.xml (Chữ thường - Khuyên dùng) ★",
                subtitle = "Nguồn lịch chuẩn đầy đủ từ epg.io.vn định dạng XMLTV",
                selected = !isCustomEpgSelected && selectedEpgUrl == TvRepository.DEFAULT_EPG_URL,
                onClick = { onEpgUrlChange(TvRepository.DEFAULT_EPG_URL) }
            )

            SelectionCard(
                title = "epgu.xml (Tiêu đề chữ HOA)",
                subtitle = "Hiển thị tiêu đề chương trình in hoa nổi bật",
                selected = !isCustomEpgSelected && selectedEpgUrl == TvRepository.UPPERCASE_EPG_URL,
                onClick = { onEpgUrlChange(TvRepository.UPPERCASE_EPG_URL) }
            )

            SelectionCard(
                title = "epg.xml.gz (File nén siêu nhẹ)",
                subtitle = "Tải nén gzip giúp tải nhanh hơn và tiết kiệm dữ liệu",
                selected = !isCustomEpgSelected && selectedEpgUrl == TvRepository.DEFAULT_EPG_GZ_URL,
                onClick = { onEpgUrlChange(TvRepository.DEFAULT_EPG_GZ_URL) }
            )

            // External EPG Link Option
            SelectionCard(
                title = "🔗 Nhập link EPG ngoài (XML / XML.GZ)",
                subtitle = "Nhập đường dẫn lịch phát sóng từ máy chủ hoặc liên kết riêng",
                selected = isCustomEpgSelected,
                onClick = onToggleCustomEpg
            )

            if (isCustomEpgSelected) {
                OutlinedTextField(
                    value = customEpgUrl,
                    onValueChange = onCustomEpgUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Đường dẫn URL tệp EPG XML ngoài", fontSize = 12.sp) },
                    placeholder = { Text("https://example.com/epg.xml", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = vmaTheme.primary,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quay lại")
            }

            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bắt đầu dò kênh", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) vmaTheme.primary.copy(alpha = 0.15f) else Color(0x14FFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) vmaTheme.primary else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = vmaTheme.primary
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) vmaTheme.primary else vmaTheme.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = vmaTheme.textMuted
                )
            }
        }
    }
}
