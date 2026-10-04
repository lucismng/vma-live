package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.ChannelItem
import com.example.model.SyncState
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.AppStyle
import com.example.ui.theme.FlowFavoriteAmber
import com.example.ui.theme.FlowLiveGreen
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme

@Composable
fun ChannelGridScreen(
    channels: List<ChannelItem>,
    groups: List<String>,
    selectedGroup: String,
    searchQuery: String,
    syncState: SyncState,
    isDarkMode: Boolean = true,
    appStyle: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
    onSelectGroup: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onChannelClick: (ChannelItem) -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onToggleAppStyle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = vmaTheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(vmaTheme.topBarBackground)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo and Title: "VMA Live"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(if (vmaTheme.style == AppStyle.MATERIAL_EXPRESSIVE) RoundedCornerShape(14.dp) else RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(vmaTheme.primary, vmaTheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "VMA Live",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = vmaTheme.textPrimary,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LiveBadge()
                            }
                            Text(
                                text = "IPTV & Lịch phát sóng EPG",
                                style = MaterialTheme.typography.labelSmall,
                                color = vmaTheme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Icons & Toggles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Toggle 1: Dark Mode / Light Mode
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dark_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Chuyển sang giao diện Sáng" else "Chuyển sang giao diện Tối",
                                tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Search Toggle Button
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) {
                                    onSearchQueryChange("")
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = vmaTheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Sync Button
                        IconButton(
                            onClick = onSyncClick,
                            enabled = !syncState.isSyncingM3u && !syncState.isSyncingEpg,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("sync_action_button")
                        ) {
                            if (syncState.isSyncingM3u || syncState.isSyncingEpg) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = vmaTheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Đồng bộ",
                                    tint = vmaTheme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Settings Button
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("settings_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Cài đặt",
                                tint = vmaTheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Collapsible Search Field
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Tìm kiếm kênh, nhóm hoặc chương trình...", fontSize = 13.sp, color = vmaTheme.textMuted) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = vmaTheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Xóa tìm kiếm",
                                            tint = vmaTheme.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("search_text_field"),
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
                    }
                }

                // Dynamic Categories Horizontal Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it }) { group ->
                        val isSelected = group == selectedGroup
                        CategoryPill(
                            title = group,
                            isSelected = isSelected,
                            onClick = { onSelectGroup(group) }
                        )
                    }
                }

                // Sync status indicator bar
                if (syncState.isSyncingM3u || syncState.isSyncingEpg) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = vmaTheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = vmaTheme.textMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (syncState.isSyncingM3u) "Đang tải danh sách kênh M3U..." else "Không tìm thấy kênh nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vmaTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Thử tìm kiếm với từ khóa khác" else "Kiểm tra lại đường dẫn nguồn trong Cài đặt",
                            style = MaterialTheme.typography.bodySmall,
                            color = vmaTheme.textSecondary
                        )
                        if (!syncState.isSyncingM3u && !syncState.isSyncingEpg) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = onSyncClick,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                                modifier = Modifier.testTag("empty_state_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tải lại kênh")
                            }
                        }
                    }
                }
            } else {
                // 3-Column LazyVerticalGrid with explicit keys
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("channels_grid"),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = channels,
                        key = { it.streamUrl }
                    ) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    if (vmaTheme.isGlass) {
        LiquidGlassPill(
            onClick = onClick,
            accentTint = if (isSelected) vmaTheme.primary else null,
            borderWidth = if (isSelected) 1.5.dp else 1.dp,
            modifier = modifier.testTag("category_pill_$title")
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else vmaTheme.textSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    } else {
        val backgroundColor = if (isSelected) vmaTheme.primary else vmaTheme.cardBackground
        val borderColor = if (isSelected) vmaTheme.primaryLight else vmaTheme.cardBorder
        val textColor = if (isSelected) Color.White else vmaTheme.textSecondary

        Box(
            modifier = modifier
                .clip(vmaTheme.pillShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, vmaTheme.pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("category_pill_$title")
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("channel_card_${channel.channelName}"),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Favorite heart icon at top end
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .testTag("favorite_button_${channel.channelName}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (channel.isFavorite) FlowFavoriteAmber else vmaTheme.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Channel Logo Box
                val logoBg = if (vmaTheme.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(if (vmaTheme.style == AppStyle.MATERIAL_EXPRESSIVE) RoundedCornerShape(16.dp) else RoundedCornerShape(10.dp))
                        .background(logoBg)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.tvgLogo.isNotBlank()) {
                        val imageRequest = remember(channel.tvgLogo, context) {
                            ImageRequest.Builder(context)
                                .data(channel.tvgLogo)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = channel.channelName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = vmaTheme.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Channel Name
                Text(
                    text = channel.channelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = vmaTheme.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // EPG Line: "[Tên chương trình hiện tại]" with live indicator
                val nowPlayingText = channel.currentProgramTitle
                if (!nowPlayingText.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(FlowLiveGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = nowPlayingText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = FlowLiveGreen,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                } else {
                    Text(
                        text = channel.groupTitle.ifBlank { "Kênh truyền hình" },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = vmaTheme.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FlowLiveRed)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "LIVE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}
