package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import com.example.util.CatchupHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ChannelItem
import com.example.model.ProgramItem
import com.example.ui.components.EqualizerBars
import com.example.ui.theme.FlowDarkSurface
import com.example.ui.theme.FlowLiveGreen
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowTextMuted
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicEpgView(
    programs: List<ProgramItem>,
    channel: ChannelItem,
    onScheduleReminder: (ProgramItem) -> Boolean,
    onTriggerEpgSync: () -> Unit,
    onPlayCatchup: ((ProgramItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var selectedProgramForDetail by remember { mutableStateOf<ProgramItem?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val displayHeaderFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")) }

    // Tính toán ngày hiện tại
    val todayKey = remember { dateKeyFormat.format(Date()) }

    // Nhóm các chương trình theo từng ngày (DateKey: yyyy-MM-dd)
    val groupedByDay = remember(programs) {
        val map = linkedMapOf<String, MutableList<ProgramItem>>()
        val cal = Calendar.getInstance()

        programs.forEach { prog ->
            cal.timeInMillis = prog.startTime
            val key = dateKeyFormat.format(cal.time)
            map.getOrPut(key) { mutableListOf() }.add(prog)
        }
        map
    }

    // Ngày được chọn hiện tại (mặc định là hôm nay nếu có, hoặc ngày đầu tiên)
    var selectedDateKey by remember(groupedByDay) {
        val defaultKey = if (groupedByDay.containsKey(todayKey)) todayKey
        else groupedByDay.keys.firstOrNull() ?: todayKey
        mutableStateOf(defaultKey)
    }

    // Hàm chuyển ngày theo độ lệch (+1 hoặc -1)
    fun shiftDay(offset: Int) {
        try {
            val cal = Calendar.getInstance()
            dateKeyFormat.parse(selectedDateKey)?.let { cal.time = it }
            cal.add(Calendar.DAY_OF_YEAR, offset)
            selectedDateKey = dateKeyFormat.format(cal.time)
        } catch (_: Exception) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            selectedDateKey = dateKeyFormat.format(cal.time)
        }
    }

    // Tiêu đề ngày hiển thị ở thanh trên
    val dateDisplayLabel = remember(selectedDateKey, todayKey) {
        try {
            val date = dateKeyFormat.parse(selectedDateKey) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Thứ 2"
                Calendar.TUESDAY -> "Thứ 3"
                Calendar.WEDNESDAY -> "Thứ 4"
                Calendar.THURSDAY -> "Thứ 5"
                Calendar.FRIDAY -> "Thứ 6"
                Calendar.SATURDAY -> "Thứ 7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }
            val formattedDate = fullDateFormat.format(date)
            if (selectedDateKey == todayKey) {
                "Hôm nay • $dayName, $formattedDate"
            } else {
                "$dayName, $formattedDate"
            }
        } catch (_: Exception) {
            selectedDateKey
        }
    }

    // Danh sách chương trình sau khi lọc theo ngày được chọn
    val displayedPrograms = remember(programs, selectedDateKey, groupedByDay) {
        groupedByDay[selectedDateKey] ?: emptyList()
    }

    val liveIndex = remember(displayedPrograms) {
        displayedPrograms.indexOfFirst { it.isLive }
    }

    // Cuộn tự động tới chương trình đang phát nếu đang chọn ngày hôm nay
    LaunchedEffect(selectedDateKey, liveIndex) {
        if (liveIndex >= 0) {
            val target = (liveIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        } else {
            listState.scrollToItem(0)
        }
    }

    if (programs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = FlowTextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Chưa có thông tin lịch phát sóng cho kênh này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onTriggerEpgSync,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    modifier = Modifier.testTag("sync_epg_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đồng bộ EPG ngay")
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // ========================================================
            // 1. THANH CHỌN NGÀY DUY NHẤT (TÍCH HỢP LỊCH THÁNG)
            // ========================================================
            Surface(
                color = FlowDarkSurface,
                border = BorderStroke(0.8.dp, Color(0x2EFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút ngày trước (<)
                    FilledTonalIconButton(
                        onClick = { shiftDay(-1) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x2A334155)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("epg_prev_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Ngày trước",
                            tint = Color.White
                        )
                    }

                    // Khối bấm vào để mở LỊCH THÁNG (Calendar Picker)
                    Surface(
                        onClick = { showDatePickerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22EF4444),
                        border = BorderStroke(1.dp, FlowPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("epg_open_month_calendar_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Mở lịch tháng",
                                tint = FlowPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateDisplayLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = FlowPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Nút ngày sau (>)
                    FilledTonalIconButton(
                        onClick = { shiftDay(1) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x2A334155)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("epg_next_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ngày sau",
                            tint = Color.White
                        )
                    }
                }
            }

            // Thanh tóm tắt số lượng chương trình & nút nhảy đến LIVE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${displayedPrograms.size} chương trình",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FlowTextSecondary
                    )
                    if (channel.supportsCatchup) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0x2510B981),
                            border = BorderStroke(0.8.dp, Color(0x6610B981))
                        ) {
                            Box(
                                modifier = Modifier.padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Hỗ trợ xem lại",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (liveIndex >= 0 && selectedDateKey == todayKey) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = FlowLiveRed.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, FlowLiveRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    val target = (liveIndex - 1).coerceAtLeast(0)
                                    listState.animateScrollToItem(target)
                                }
                            }
                            .testTag("jump_to_live_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            LiveBadge()
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tới chương trình đang phát",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlowLiveRed
                            )
                        }
                    }
                }
            }

            // ========================================================
            // 2. DANH SÁCH CHƯƠNG TRÌNH GỌN GÀNG (KHÔNG CÓ ẢNH TRONG LIST)
            // ========================================================
            if (displayedPrograms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FlowTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không có chương trình nào trong ngày này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlowTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { selectedDateKey = todayKey },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Xem lịch phát sóng Hôm nay")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayedPrograms,
                        key = { _, item -> item.id }
                    ) { _, program ->
                        val canReplay = CatchupHelper.isProgramReplayable(channel, program)
                        CompactProgramRowItem(
                            program = program,
                            canReplay = canReplay,
                            onClick = { selectedProgramForDetail = program },
                            onPlayCatchup = { onPlayCatchup?.invoke(program) },
                            onScheduleReminder = {
                                val success = onScheduleReminder(program)
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Đã đặt lịch nhắc: ${program.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Không thể đặt lịch nhắc nhở",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ========================================================
    // 3. DIALOG LỊCH THÁNG (MATERIAL 3 MONTH DATE PICKER)
    // ========================================================
    if (showDatePickerDialog) {
        val initialMillis = remember(selectedDateKey) {
            try {
                dateKeyFormat.parse(selectedDateKey)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            selectedDateKey = dateKeyFormat.format(cal.time)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Chọn", color = FlowPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            selectedDateKey = todayKey
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Hôm nay", color = FlowLiveRed, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Đóng", color = FlowTextSecondary)
                    }
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1E2433))
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Chọn ngày phát sóng",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                headline = {
                    Text(
                        text = channel.channelName,
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = FlowPrimary
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF1E2433),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = FlowTextMuted,
                    subheadContentColor = FlowTextSecondary,
                    yearContentColor = Color.White,
                    currentYearContentColor = FlowPrimary,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = FlowPrimary,
                    dayContentColor = Color.White,
                    disabledDayContentColor = Color(0x33FFFFFF),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = FlowPrimary,
                    todayContentColor = FlowPrimary,
                    todayDateBorderColor = FlowPrimary
                )
            )
        }
    }

    // ========================================================
    // 4. HỘP THOẠI CHI TIẾT CHƯƠNG TRÌNH (BẤM VÀO MỚI HIỆN HÌNH ẢNH)
    // ========================================================
    selectedProgramForDetail?.let { prog ->
        AlertDialog(
            onDismissRequest = { selectedProgramForDetail = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF181C26),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Column {
                    // Hình ảnh thumbnail hiển thị khi bấm vào chi tiết
                    if (!prog.thumbnailUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF10131A)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(prog.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = prog.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else if (channel.tvgLogo.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222838)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(channel.tvgLogo)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = channel.channelName,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (prog.isLive) {
                            LiveBadge()
                        }
                        Text(
                            text = prog.timeRangeFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (prog.isLive) FlowLiveGreen else FlowPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (prog.title.isNotBlank()) prog.title else "Chương trình truyền hình",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kênh: ${channel.channelName} • ${channel.groupTitle}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowPrimary
                    )

                    if (!prog.description.isNullOrBlank()) {
                        Text(
                            text = prog.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    } else {
                        Text(
                            text = "Không có thông tin mô tả chi tiết cho chương trình này.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlowTextMuted
                        )
                    }
                }
            },
            confirmButton = {
                if (prog.startTime > System.currentTimeMillis()) {
                    Button(
                        onClick = {
                            val success = onScheduleReminder(prog)
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "Đã đặt lịch nhắc: ${prog.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedProgramForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhắc tôi", fontWeight = FontWeight.Bold)
                    }
                } else if (CatchupHelper.isProgramReplayable(channel, prog)) {
                    Button(
                        onClick = {
                            onPlayCatchup?.invoke(prog)
                            selectedProgramForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_play_catchup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xem lại ngay", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProgramForDetail = null }) {
                    Text("Đóng", color = FlowTextSecondary)
                }
            }
        )
    }
}

/**
 * Mục chương trình EPG gọn gàng, độ tương phản cao, KHÔNG chứa ảnh thumbnail trong danh sách.
 */
@Composable
private fun CompactProgramRowItem(
    program: ProgramItem,
    canReplay: Boolean = false,
    onClick: () -> Unit,
    onScheduleReminder: () -> Unit,
    onPlayCatchup: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLive = program.isLive
    val isPast = !isLive && program.endTime < System.currentTimeMillis()
    val isFuture = !isLive && program.startTime > System.currentTimeMillis()

    // Màu nền opaque, độ tương phản cao để không bao giờ bị lỗi xám mờ / khó nhìn
    val cardBackground = when {
        isLive -> Color(0xFF1F2636)
        isPast -> Color(0xFF12141C)
        else -> Color(0xFF161922)
    }

    val cardBorder = when {
        isLive -> FlowLiveRed
        canReplay -> Color(0x6610B981)
        else -> Color(0x28FFFFFF)
    }

    val titleText = if (program.title.isNotBlank()) program.title else "Chương trình truyền hình"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        border = BorderStroke(if (isLive) 1.2.dp else 0.8.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isPast && !canReplay) 0.65f else 1f)
            .testTag("epg_program_${program.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nhóm Status + Thời gian
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLive) {
                        LiveBadge()
                    } else if (canReplay) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x2210B981)
                        ) {
                            Text(
                                text = "XEM LẠI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isPast) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x2694A3B8)
                        ) {
                            Text(
                                text = "ĐÃ CHIẾU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlowTextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FlowPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "SẮP CHIẾU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlowPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = program.timeRangeFormatted,
                        fontSize = 12.sp,
                        fontWeight = if (isLive || canReplay) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLive) FlowLiveGreen else if (canReplay) Color(0xFF34D399) else if (isPast) FlowTextMuted else Color(0xFFCBD5E1),
                        maxLines = 1,
                        softWrap = false
                    )

                    if (isLive) {
                        EqualizerBars(
                            color = FlowLiveGreen,
                            barWidth = 2.dp,
                            maxHeight = 12.dp
                        )
                    }
                }

                // Nút chuông nhắc nhở cho các chương trình sắp chiếu hoặc nút XEM LẠI cho chương trình đã chiếu
                if (isFuture) {
                    IconButton(
                        onClick = onScheduleReminder,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("program_reminder_button_${program.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Đặt nhắc nhở",
                            tint = FlowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (canReplay) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x3310B981),
                        border = BorderStroke(0.8.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .clickable { onPlayCatchup?.invoke() }
                            .testTag("program_replay_button_${program.id}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Xem lại",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tiêu đề chương trình (Luôn màu trắng tương phản, rõ nét)
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLive) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isPast) Color(0xFF94A3B8) else Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            // Thanh tiến trình phát sóng trực tiếp thời gian thực
            if (isLive) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = program.progressFraction)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(FlowPrimary, FlowLiveGreen)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(FlowLiveRed.copy(alpha = 0.2f))
            .border(0.5.dp, FlowLiveRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(FlowLiveRed)
                .alpha(alphaAnim)
        )
        Text(
            text = "LIVE",
            color = FlowLiveRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
