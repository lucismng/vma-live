package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.RecordingEntity
import com.example.model.ChannelItem
import com.example.service.pvr.ActiveRecordingInfo
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PvrScreen(
    recordings: List<RecordingEntity>,
    isRecording: Boolean,
    activeRecordings: List<ActiveRecordingInfo> = emptyList(),
    recordingChannel: ChannelItem? = null,
    recordingDurationSec: Long = 0L,
    recordingBytes: Long = 0L,
    uploadProgress: Int? = null,
    isGoogleDriveSignedIn: Boolean = false,
    googleDriveUserEmail: String = "",
    onSignInGoogleDrive: (String, String) -> Unit = { _, _ -> },
    onStopRecording: () -> Unit = {},
    onStopRecordingChannel: (ChannelItem) -> Unit = {},
    onStopAllRecordings: () -> Unit = {},
    onPlayRecording: (RecordingEntity) -> Unit = {},
    onDeleteRecording: (RecordingEntity) -> Unit = {},
    onUploadToDrive: (RecordingEntity) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onGoToLiveTv: () -> Unit = {}
) {
    val vmaTheme = LocalVmaTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(vmaTheme.background)
            .padding(16.dp)
            .testTag("pvr_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "Quản lý ghi hình (PVR)",
                    color = vmaTheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lưu lại các chương trình thời sự, thể thao và phim truyện",
                    color = vmaTheme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isRecording) {
                Button(
                    onClick = onGoToLiveTv,
                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xem TV", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeRecordings.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 Đang ghi hình (${activeRecordings.size} kênh đồng thời)",
                    color = FlowLiveRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (activeRecordings.size > 1) {
                    TextButton(
                        onClick = onStopAllRecordings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = FlowLiveRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dừng tất cả (${activeRecordings.size})", color = FlowLiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeRecordings.forEach { active ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FlowLiveRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (active.channel.tvgLogo.isNotBlank()) {
                                    AsyncImage(
                                        model = active.channel.tvgLogo,
                                        contentDescription = active.channel.channelName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(FlowLiveRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, tint = FlowLiveRed, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = active.channel.channelName,
                                        color = FlowLiveRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = active.programTitle,
                                        color = vmaTheme.textSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val mins = active.durationSeconds / 60
                                    val secs = active.durationSeconds % 60
                                    val mb = active.recordedBytes / (1024f * 1024f)
                                    Text(
                                        text = String.format("⏱ %02d:%02d • 💾 %.1f MB", mins, secs, mb),
                                        color = vmaTheme.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onStopRecordingChannel(active.channel) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowLiveRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = "Dừng", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dừng", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else if (isRecording) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FlowLiveRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔴 Đang ghi hình: ${recordingChannel?.channelName ?: "Kênh trực tiếp"}",
                            color = FlowLiveRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val mins = recordingDurationSec / 60
                        val secs = recordingDurationSec % 60
                        val mb = recordingBytes / (1024 * 1024)
                        Text(
                            text = String.format("Thời lượng: %02d:%02d • Dung lượng: %d MB", mins, secs, mb),
                            color = vmaTheme.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowLiveRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Dừng", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dừng", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Danh sách bản ghi (${recordings.size})",
            color = vmaTheme.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = vmaTheme.textSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Chưa có bản ghi nào",
                        color = vmaTheme.textPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Nhấn nút REC khi đang xem TV để ghi lại chương trình",
                        color = vmaTheme.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = vmaTheme.card),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.channelName,
                                    color = vmaTheme.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec.programTitle ?: "Chương trình truyền hình",
                                    color = vmaTheme.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec.fileName.ifBlank { java.io.File(rec.filePath).name },
                                    color = vmaTheme.primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormat.format(Date(rec.startTimeMs)),
                                    color = vmaTheme.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val file = java.io.File(rec.filePath)
                                        com.example.util.FileManagerHelper.revealFileInFileManager(context, file)
                                    },
                                    modifier = Modifier.testTag("show_in_file_manager_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Hiện file trong trình quản lý file",
                                        tint = Color(0xFFF59E0B)
                                    )
                                }
                                IconButton(onClick = { onPlayRecording(rec) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Phát",
                                        tint = vmaTheme.accent
                                    )
                                }
                                IconButton(onClick = { onDeleteRecording(rec) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa",
                                        tint = vmaTheme.textSecondary
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
