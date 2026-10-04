package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Trang chủ", Icons.Default.Home),
    EPG("EPG", Icons.Default.CalendarMonth),
    TV("Xem TV", Icons.Default.LiveTv),
    PVR("Ghi hình", Icons.Default.Videocam),
    SETTINGS("Cài đặt", Icons.Default.Settings)
}
