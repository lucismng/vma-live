package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ProgramEntity
import com.example.model.ChannelItem
import com.example.model.CountryFilter
import com.example.model.ProgramItem
import com.example.model.SyncState
import com.example.model.countryFilter
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import com.example.ui.viewmodel.TvViewModel
import com.example.util.CatchupHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Màn hình EPG đa kênh dạng Timeline ma trận (như ảnh 1):
 * - Thanh chọn ngày dạng viên thuốc (Yesterday, Today, Tomorrow...)
 * - Thông số tổng số kênh có EPG và thời gian cập nhật
 * - Bảng ma trận: Cột trái cố định danh sách kênh, phần phải cuộn ngang theo các mốc thời gian 24h
 * - Đường chỉ báo giờ hiện tại (Vertical indicator)
 * - Tương tác chạm: Bấm kênh để phát ngay, bấm chương trình để xem chi tiết / phát lại (catch-up)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiChannelEpgScreen(
    viewModel: TvViewModel,
    channels: List<ChannelItem>,
    syncState: SyncState,
    onSelectChannel: (ChannelItem) -> Unit,
    onPlayCatchup: (ChannelItem, ProgramItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Quản lý ngày xem EPG (-2 ngày trước đến +5 ngày tới)
    val calendarDays = remember {
        val list = mutableListOf<CalendarDayInfo>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayNames = arrayOf("Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy")
        val monthFormat = SimpleDateFormat("dd MMM", Locale("vi", "VN"))

        for (offset in -2..5) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            val startMillis = c.timeInMillis
            val endMillis = startMillis + 24L * 60 * 60 * 1000 - 1

            val label = when (offset) {
                -1 -> "Hôm qua"
                0 -> "Hôm nay"
                1 -> "Ngày mai"
                else -> dayNames[c.get(Calendar.DAY_OF_WEEK) - 1]
            }

            list.add(
                CalendarDayInfo(
                    offset = offset,
                    label = label,
                    dateFormatted = monthFormat.format(c.time),
                    startOfDayMillis = startMillis,
                    endOfDayMillis = endMillis,
                    isToday = offset == 0
                )
            )
        }
        list
    }

    var selectedDayIndex by remember { mutableIntStateOf(2) } // Mặc định là Hôm nay (index 2: -2, -1, 0)
    val currentDayInfo = calendarDays.getOrElse(selectedDayIndex) { calendarDays[2] }

    // Tải toàn bộ chương trình trong ngày đã chọn
    val programsInDay by viewModel.getProgramsForRange(
        currentDayInfo.startOfDayMillis,
        currentDayInfo.endOfDayMillis
    ).collectAsState(initial = emptyList())

    // Map chương trình theo channelTvgId
    val programsByChannel = remember(programsInDay) {
        programsInDay.groupBy { it.channelTvgId.lowercase() }
    }

    // Bộ lọc kênh và tìm kiếm
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("Tất cả") }

    val categoryTabs = listOf("Tất cả", "VTV", "HTV", "VTVcab", "Kênh Thiết Yếu", "Địa Phương", "Quốc Tế")

    val filteredChannels = remember(channels, searchQuery, selectedGroupFilter) {
        var list = channels
        if (selectedGroupFilter != "Tất cả") {
            list = when (selectedGroupFilter) {
                "VTV" -> list.filter { it.channelName.contains("VTV", ignoreCase = true) || it.groupTitle.contains("VTV", ignoreCase = true) }
                "HTV" -> list.filter { it.channelName.contains("HTV", ignoreCase = true) || it.groupTitle.contains("HTV", ignoreCase = true) }
                "VTVcab" -> list.filter { it.channelName.contains("VTVcab", ignoreCase = true) || it.groupTitle.contains("VTVcab", ignoreCase = true) }
                "Kênh Thiết Yếu" -> list.filter { it.groupTitle.contains("thiết yếu", ignoreCase = true) || it.groupTitle.contains("thiet yeu", ignoreCase = true) }
                "Địa Phương" -> list.filter { it.groupTitle.contains("địa phương", ignoreCase = true) || it.groupTitle.contains("dia phuong", ignoreCase = true) || it.groupTitle.contains("tỉnh", ignoreCase = true) }
                "Quốc Tế" -> list.filter { it.groupTitle.contains("quốc tế", ignoreCase = true) || it.groupTitle.contains("quoc te", ignoreCase = true) || it.countryFilter() != CountryFilter.VIETNAM }
                else -> list.filter { it.groupTitle.equals(selectedGroupFilter, ignoreCase = true) }
            }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { it.channelName.lowercase().contains(q) || it.groupTitle.lowercase().contains(q) }
        }
        list
    }

    // Đếm số kênh có EPG
    val channelsWithEpgCount = remember(channels, programsByChannel) {
        channels.count { ch ->
            val tvgKey = ch.tvgId.ifBlank { ch.tvgName }.lowercase()
            programsByChannel[tvgKey]?.isNotEmpty() == true
        }
    }

    // Thời gian hiện tại cập nhật mỗi 30s
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Modal hiển thị chi tiết chương trình
    var selectedProgramDetail by remember { mutableStateOf<ProgramDetailData?>(null) }

    // Quản lý thanh cuộn thời gian 24 giờ
    // 1 giờ = 160.dp -> 1 phút = 160 / 60 = 2.67.dp. Tổng bề ngang 24 giờ = 3840.dp
    val hourWidthDp = 160.dp
    val pxPerMinuteDp = hourWidthDp / 60f
    val timelineScrollState = rememberScrollState()

    // Cuộn tới giờ hiện tại khi mở màn hình nếu đang xem hôm nay
    LaunchedEffect(currentDayInfo.isToday) {
        if (currentDayInfo.isToday) {
            val nowCal = Calendar.getInstance()
            val minutesFromMidnight = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val targetScroll = ((minutesFromMidnight - 45).coerceAtLeast(0) * (hourWidthDp.value / 60f) * 2.5f).toInt()
            timelineScrollState.animateScrollTo(targetScroll)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(vmaTheme.background)
            .testTag("multi_channel_epg_screen")
    ) {
        // ========================================================
        // 1. THANH CHỌN NGÀY DẠNG VIÊN THUỐC (IMAGE 1 TOP)
        // ========================================================
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(calendarDays) { day ->
                val isSelected = day.offset == currentDayInfo.offset
                Surface(
                    onClick = {
                        val idx = calendarDays.indexOf(day)
                        if (idx >= 0) selectedDayIndex = idx
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) Color(0xFF6366F1) else vmaTheme.cardBackground,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF818CF8) else vmaTheme.cardBorder
                    ),
                    modifier = Modifier.testTag("date_pill_${day.offset}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = day.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else vmaTheme.textMuted
                        )
                        Text(
                            text = day.dateFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else vmaTheme.textPrimary
                        )
                    }
                }
            }
        }

        // ========================================================
        // 2. DÒNG THỐNG KÊ KÊNH VÀ TRẠNG THÁI CẬP NHẬT
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = vmaTheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$channelsWithEpgCount kênh có lịch phát sóng",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vmaTheme.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (syncState.isSyncingEpg) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = vmaTheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Đang đồng bộ...",
                        fontSize = 11.sp,
                        color = vmaTheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.syncAll() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới EPG",
                            tint = vmaTheme.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Đã cập nhật",
                        fontSize = 11.sp,
                        color = vmaTheme.textMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // ========================================================
        // 3. THANH TÌM KIẾM NHANH & CHỌN NHÓM
        // ========================================================
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categoryTabs) { tab ->
                val isSelected = tab == selectedGroupFilter
                Surface(
                    onClick = { selectedGroupFilter = tab },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) vmaTheme.primary.copy(alpha = 0.2f) else vmaTheme.cardBackground,
                    border = BorderStroke(
                        0.8.dp,
                        if (isSelected) vmaTheme.primary else vmaTheme.cardBorder
                    )
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) vmaTheme.primary else vmaTheme.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ========================================================
        // ========================================================
        // 4. BẢNG MA TRẬN TIMELINE EPG (IMAGE 1)
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val channelColumnWidth = 124.dp

            val minutesNow = remember(currentTimeMillis) {
                val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
                nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            }
            val indicatorOffsetDp = remember(minutesNow, pxPerMinuteDp) {
                (minutesNow * pxPerMinuteDp.value).dp
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header đồng bộ: Cột Kênh + Dòng 24 mốc giờ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF0F172A))
                ) {
                    // Header của cột kênh: "Kênh"
                    Box(
                        modifier = Modifier
                            .width(channelColumnWidth)
                            .fillMaxHeight()
                            .border(BorderStroke(0.8.dp, vmaTheme.cardBorder))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Kênh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Header timeline: 24 mốc giờ (00:00 -> 23:00) cuộn ngang đồng bộ
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(timelineScrollState)
                    ) {
                        for (hour in 0..23) {
                            Box(
                                modifier = Modifier
                                    .width(hourWidthDp)
                                    .fillMaxHeight()
                                    .border(BorderStroke(0.4.dp, Color(0x3394A3B8)))
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = String.format("%02d:00", hour),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Danh sách KÊNH + TIMELINE trong cùng 1 LazyColumn duy nhất (đồng bộ cuộn dọc 100%)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredChannels, key = { it.streamUrl }) { ch ->
                        val tvgKey = ch.tvgId.ifBlank { ch.tvgName }.lowercase()
                        val channelPrograms = programsByChannel[tvgKey] ?: emptyList()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            // Cột 1 cố định: Tên kênh + Logo
                            Surface(
                                onClick = { onSelectChannel(ch) },
                                color = vmaTheme.surface,
                                modifier = Modifier
                                    .width(channelColumnWidth)
                                    .fillMaxHeight()
                                    .border(BorderStroke(0.5.dp, Color(0x22FFFFFF)))
                                    .testTag("epg_channel_col_${ch.channelName}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0x330F172A),
                                        border = BorderStroke(0.6.dp, Color(0x44FFFFFF)),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (ch.tvgLogo.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(ch.tvgLogo)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = ch.channelName,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Tv,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.channelName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 14.sp
                                        )
                                        if (ch.supportsCatchup) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Xem lại",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Cột 2: Timeline cuộn ngang đồng bộ cùng timelineScrollState
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(timelineScrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(hourWidthDp * 24)
                                        .fillMaxHeight()
                                        .border(BorderStroke(0.4.dp, Color(0x1AFFFFFF)))
                                ) {
                                    if (channelPrograms.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0x1F334155),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Đang phát sóng: ${ch.currentProgramTitle ?: ch.channelName}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else {
                                        val dayStart = currentDayInfo.startOfDayMillis
                                        val dayEnd = currentDayInfo.endOfDayMillis

                                        channelPrograms.forEach { prog ->
                                            val progStart = prog.startTime.coerceAtLeast(dayStart)
                                            val progEnd = prog.endTime.coerceAtMost(dayEnd)

                                            if (progEnd > progStart) {
                                                val startMinutes = ((progStart - dayStart) / (60 * 1000f))
                                                val durationMinutes = ((progEnd - progStart) / (60 * 1000f)).coerceAtLeast(15f)

                                                val blockOffsetDp = (startMinutes * pxPerMinuteDp.value).dp
                                                val blockWidthDp = (durationMinutes * pxPerMinuteDp.value).dp

                                                val isLive = currentTimeMillis in prog.startTime until prog.endTime
                                                val isPast = prog.endTime < currentTimeMillis
                                                val canReplay = ch.supportsCatchup && isPast

                                                val blockBg = when {
                                                    isLive -> Color(0xFF312E81)
                                                    isPast -> Color(0x331E293B)
                                                    else -> Color(0x44334155)
                                                }

                                                val blockBorder = when {
                                                    isLive -> BorderStroke(1.2.dp, Color(0xFF818CF8))
                                                    canReplay -> BorderStroke(0.8.dp, Color(0x5510B981))
                                                    else -> BorderStroke(0.6.dp, Color(0x22FFFFFF))
                                                }

                                                val startCal = Calendar.getInstance().apply { timeInMillis = prog.startTime }
                                                val endCal = Calendar.getInstance().apply { timeInMillis = prog.endTime }
                                                val startStr = "%02d:%02d".format(startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE))
                                                val endStr = "%02d:%02d".format(endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))

                                                Surface(
                                                    onClick = {
                                                        val pItem = ProgramItem(
                                                            id = prog.id,
                                                            title = prog.title,
                                                            description = prog.description,
                                                            startTime = prog.startTime,
                                                            endTime = prog.endTime,
                                                            channelTvgId = prog.channelTvgId,
                                                            isLive = isLive
                                                        )
                                                        selectedProgramDetail = ProgramDetailData(
                                                            channel = ch,
                                                            program = pItem,
                                                            canReplay = canReplay
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = blockBg,
                                                    border = blockBorder,
                                                    modifier = Modifier
                                                        .offset(x = blockOffsetDp)
                                                        .width(blockWidthDp)
                                                        .fillMaxHeight()
                                                        .padding(horizontal = 2.dp, vertical = 5.dp)
                                                        .testTag("epg_block_${prog.id}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = prog.title,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isLive) Color.White else Color(0xFFE2E8F0),
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )

                                                            if (canReplay) {
                                                                Icon(
                                                                    imageVector = Icons.Default.History,
                                                                    contentDescription = "Xem lại",
                                                                    tint = Color(0xFF34D399),
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = "$startStr - $endStr",
                                                            fontSize = 10.sp,
                                                            color = if (isLive) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Chỉ báo giờ hiện tại trong từng hàng timeline
                                    if (currentDayInfo.isToday) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = indicatorOffsetDp)
                                                .width(2.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF818CF8))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // NÚT "HIỆN TẠI" NỔI ĐỂ CUỘN NGAY ĐẾN GIỜ ĐANG PHÁT
            if (currentDayInfo.isToday) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val nowCal = Calendar.getInstance()
                            val minutesNow = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                            val targetScroll = ((minutesNow - 45).coerceAtLeast(0) * (hourWidthDp.value / 60f) * 2.5f).toInt()
                            timelineScrollState.animateScrollTo(targetScroll)
                        }
                    },
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("jump_to_now_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hiện tại", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // ========================================================
    // 5. MODAL BOTTOM SHEET: CHI TIẾT CHƯƠNG TRÌNH & PHÁT LẠI
    // ========================================================
    selectedProgramDetail?.let { detail ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val timeFmt = SimpleDateFormat("HH:mm - EEEE, dd/MM/yyyy", Locale("vi", "VN"))
        val startFormatted = timeFmt.format(Date(detail.program.startTime))
        val endFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(detail.program.endTime))

        ModalBottomSheet(
            onDismissRequest = { selectedProgramDetail = null },
            sheetState = sheetState,
            containerColor = vmaTheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = vmaTheme.cardBackground,
                            border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (detail.channel.tvgLogo.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(detail.channel.tvgLogo)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = detail.channel.channelName,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = vmaTheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = detail.channel.channelName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = vmaTheme.primary
                            )
                            Text(
                                text = detail.channel.groupTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = vmaTheme.textMuted
                            )
                        }
                    }

                    IconButton(onClick = { selectedProgramDetail = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = vmaTheme.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = detail.program.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = vmaTheme.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$startFormatted đến $endFormatted",
                        fontSize = 12.sp,
                        color = vmaTheme.textMuted
                    )
                }

                if (!detail.program.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = detail.program.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = vmaTheme.textSecondary,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isUpcoming = detail.program.startTime > System.currentTimeMillis()
                    if (detail.program.isLive) {
                        Button(
                            onClick = {
                                onSelectChannel(detail.channel)
                                selectedProgramDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowLiveRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Xem trực tiếp ngay", fontWeight = FontWeight.Bold)
                        }
                    } else if (detail.canReplay) {
                        Button(
                            onClick = {
                                onPlayCatchup(detail.channel, detail.program)
                                selectedProgramDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Xem lại chương trình", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (isUpcoming) {
                        Button(
                            onClick = {
                                val success = viewModel.scheduleProgramReminder(detail.program, detail.channel.channelName)
                                Toast.makeText(
                                    context,
                                    if (success) "Đã đặt lịch nhắc: ${detail.program.title}" else "Lỗi đặt nhắc nhở",
                                    Toast.LENGTH_SHORT
                                ).show()
                                selectedProgramDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nhắc tôi phát sóng", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSelectChannel(detail.channel)
                                selectedProgramDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Kênh", fontWeight = FontWeight.SemiBold, color = vmaTheme.primary)
                        }
                    } else {
                        Button(
                            onClick = {
                                onSelectChannel(detail.channel)
                                selectedProgramDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chuyển đến kênh", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class CalendarDayInfo(
    val offset: Int,
    val label: String,
    val dateFormatted: String,
    val startOfDayMillis: Long,
    val endOfDayMillis: Long,
    val isToday: Boolean
)

private data class ProgramDetailData(
    val channel: ChannelItem,
    val program: ProgramItem,
    val canReplay: Boolean
)
