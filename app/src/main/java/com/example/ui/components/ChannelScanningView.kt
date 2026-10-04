package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVmaTheme

@Composable
fun ChannelScanningContent(
    progress: Float,
    statusText: String,
    detectedChannels: List<String>,
    programCount: Int,
    isFinished: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    finishButtonText: String = "Hoàn tất & Xem TV ngay"
) {
    val vmaTheme = LocalVmaTheme.current
    val listState = rememberLazyListState()

    // Auto-scroll to the newest scanned channel
    LaunchedEffect(detectedChannels.size) {
        if (detectedChannels.isNotEmpty()) {
            listState.animateScrollToItem(detectedChannels.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isFinished) "Hoàn tất dò kênh!" else "Đang dò kênh truyền hình...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = vmaTheme.textPrimary
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = if (isFinished) Color(0xFF4CAF50) else vmaTheme.textMuted
                    )
                }

                if (isFinished) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(24.dp),
                        color = vmaTheme.primary,
                        strokeWidth = 2.5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Stats Cards: Số kênh & Số chương trình
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Số kênh
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(vmaTheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, tint = vmaTheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Kênh tìm thấy", fontSize = 11.sp, color = vmaTheme.textMuted)
                            Text("${detectedChannels.size} kênh", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = vmaTheme.textPrimary)
                        }
                    }
                }

                // Card 2: Lịch phát sóng
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x3338BDF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Lịch phát sóng", fontSize = 11.sp, color = vmaTheme.textMuted)
                            Text("${programCount} chương trình", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = vmaTheme.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isFinished) Color(0xFF4CAF50) else vmaTheme.primary,
                    trackColor = Color(0x22FFFFFF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFinished) "Đã hoàn thành dò kênh" else "Tiến trình quét...",
                        fontSize = 11.sp,
                        color = vmaTheme.textMuted
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = vmaTheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Danh sách kênh dò được (${detectedChannels.size}):",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = vmaTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Clean Channel List Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F141E))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                if (detectedChannels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Đang kết nối luồng kênh...", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(detectedChannels) { index, channelName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (index == detectedChannels.lastIndex && !isFinished) vmaTheme.primary.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "%02d.".format(index + 1),
                                        color = vmaTheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = channelName,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x334ADE80))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("HD", color = Color(0xFF4ADE80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Button
        Button(
            onClick = onComplete,
            enabled = isFinished,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFinished) Color(0xFF10B981) else vmaTheme.primary.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isFinished) {
                Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(finishButtonText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đang dò kênh (${(progress * 100).toInt()}%)...", fontSize = 14.sp)
            }
        }
    }
}

/**
 * Dialog hiển thị khi thêm / đồng bộ kênh trong màn hình Cài đặt
 */
@Composable
fun ChannelScanningDialog(
    progress: Float,
    statusText: String,
    detectedChannels: List<String>,
    programCount: Int,
    isFinished: Boolean,
    onDismiss: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current

    AlertDialog(
        onDismissRequest = {
            if (isFinished) onDismiss()
        },
        containerColor = vmaTheme.cardBackground,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Box(modifier = Modifier.height(460.dp)) {
                ChannelScanningContent(
                    progress = progress,
                    statusText = statusText,
                    detectedChannels = detectedChannels,
                    programCount = programCount,
                    isFinished = isFinished,
                    onComplete = onDismiss,
                    finishButtonText = "Đóng & Cập nhật danh sách"
                )
            }
        },
        confirmButton = {}
    )
}
