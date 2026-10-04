package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVmaTheme

@Composable
fun VmaBottomNavBar(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    isPvrRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current

    Surface(
        color = vmaTheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Home Tab
            BottomNavItem(
                icon = if (selectedTab == NavTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                label = "Trang chủ",
                isSelected = selectedTab == NavTab.HOME,
                onClick = { onSelectTab(NavTab.HOME) },
                testTag = "nav_home",
                modifier = Modifier.weight(1f)
            )

            // 2. EPG Tab (Nằm cạnh tab trang chủ)
            BottomNavItem(
                icon = if (selectedTab == NavTab.EPG) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                label = "EPG",
                isSelected = selectedTab == NavTab.EPG,
                onClick = { onSelectTab(NavTab.EPG) },
                testTag = "nav_epg",
                modifier = Modifier.weight(0.9f)
            )

            // 3. TV Live Tab (Center pill with "TV")
            TvCenterPillItem(
                isSelected = selectedTab == NavTab.TV,
                onClick = { onSelectTab(NavTab.TV) },
                testTag = "nav_tv",
                modifier = Modifier.weight(1.1f)
            )

            // 4. PVR Recordings Tab
            BottomNavPvrItem(
                isSelected = selectedTab == NavTab.PVR,
                isRecording = isPvrRecording,
                onClick = { onSelectTab(NavTab.PVR) },
                testTag = "nav_pvr",
                modifier = Modifier.weight(1f)
            )

            // 5. Settings Tab
            BottomNavItem(
                icon = if (selectedTab == NavTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                label = "Cài đặt",
                isSelected = selectedTab == NavTab.SETTINGS,
                onClick = { onSelectTab(NavTab.SETTINGS) },
                testTag = "nav_settings",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) vmaTheme.primary else vmaTheme.textMuted,
        animationSpec = tween(200),
        label = "nav_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BottomNavPvrItem(
    isSelected: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) vmaTheme.primary else vmaTheme.textMuted,
        animationSpec = tween(200),
        label = "nav_pvr_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pvr_badge_pulse")
    val badgeAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pvr_badge_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = if (isSelected) Icons.Default.VideoLibrary else Icons.Outlined.VideoLibrary,
                contentDescription = "Bản ghi PVR",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = badgeAlpha))
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Bản ghi",
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvCenterPillItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current

    val pillBg = if (isSelected) {
        if (vmaTheme.isGlass) {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF3B82F6).copy(alpha = 0.85f),
                    Color(0xFF6366F1).copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.horizontalGradient(listOf(vmaTheme.primary, vmaTheme.primaryLight))
        }
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(pillBg)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (vmaTheme.isGlass) Color(0x80FFFFFF) else Color.Transparent
                            ),
                            RoundedCornerShape(20.dp)
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = "TV",
                tint = if (isSelected) Color.White else vmaTheme.textMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "TV",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else vmaTheme.textMuted,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}


