package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ChannelItem
import com.example.model.CountryFilter
import com.example.model.countryFilter
import com.example.ui.components.EqualizerBars
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.VmaFooterBrand
import com.example.ui.components.VmaLogoImage
import com.example.ui.theme.AppStyle
import com.example.ui.theme.FlowFavoriteAmber
import com.example.ui.theme.FlowLiveRed
import com.example.ui.theme.LocalVmaTheme
import kotlinx.coroutines.delay

/**
 * Checks if a channel belongs to VTVcab (Pay TV system) and MUST NOT be classified as National VTV.
 */
fun isVtvCabChannel(ch: ChannelItem): Boolean {
    val group = ch.groupTitle.trim().lowercase()
    val name = ch.channelName.trim().lowercase()
    val tvg = ch.tvgName.trim().lowercase()
    val id = ch.tvgId.trim().lowercase()

    if (group.contains("vtvcab") || group.contains("vtv cab") || group.contains("vtv-cab") || group == "cab") return true
    if (name.contains("vtvcab") || name.contains("vtv cab") || name.contains("vtv-cab")) return true
    if (tvg.contains("vtvcab") || tvg.contains("vtv cab") || id.contains("vtvcab")) return true
    if (name.startsWith("on ") || name.startsWith("on-") || id.startsWith("on") || tvg.startsWith("on")) {
        if (!name.contains("online") && !id.contains("online")) return true
    }
    if (name.contains("bóng đá tv") || name.contains("thể thao tv") || name.contains("thể thao tin tức")) return true
    return false
}

private val NATIONAL_VTV_REGEX = Regex("^(kênh\\s*)?vtv\\s*([1-9]|10|cần thơ|can tho|tây nam bộ|tay nam bo|tây nguyên|tay nguyen|bạch long vĩ).*", RegexOption.IGNORE_CASE)

/**
 * Checks if a channel belongs to the National VTV system (VTV1-VTV9, VTV Cần Thơ, etc.).
 * Strictly excludes any channel that belongs to VTVcab.
 */
fun isNationalVtvChannel(ch: ChannelItem): Boolean {
    if (isVtvCabChannel(ch)) return false

    val group = ch.groupTitle.trim().lowercase()
    val name = ch.channelName.trim().lowercase()
    val id = ch.tvgId.trim().lowercase()

    val matchesCanonicalVtvName = name.matches(NATIONAL_VTV_REGEX) ||
        name.contains("vtv1") || name.contains("vtv2") || name.contains("vtv3") || name.contains("vtv4") ||
        name.contains("vtv5") || name.contains("vtv6") || name.contains("vtv7") || name.contains("vtv8") ||
        name.contains("vtv9") || name.contains("vietnam today") ||
        id.startsWith("vtv1") || id.startsWith("vtv2") || id.startsWith("vtv3") || id.startsWith("vtv4") ||
        id.startsWith("vtv5") || id.startsWith("vtv6") || id.startsWith("vtv7") || id.startsWith("vtv8") ||
        id.startsWith("vtv9")

    val isVtvGroup = group == "vtv" || group == "kênh vtv" || group == "truyền hình vtv"
    return matchesCanonicalVtvName || (isVtvGroup && !group.contains("cab"))
}

/**
 * Kiểm tra kênh thuộc Đài Truyền hình TP. Hồ Chí Minh (HTV / HTVC)
 */
fun isHtvChannel(ch: ChannelItem): Boolean {
    val group = ch.groupTitle.trim().lowercase()
    val name = ch.channelName.trim().lowercase()
    val tvg = ch.tvgName.trim().lowercase()
    val id = ch.tvgId.trim().lowercase()

    if (group.contains("htv") || group.contains("htvc") || group.contains("tphcm") ||
        group.contains("tp.hcm") || group.contains("tp hcm") ||
        group.contains("hồ chí minh") || group.contains("ho chi minh")
    ) return true

    if (name.contains("htv") || tvg.contains("htv") || id.contains("htv") ||
        name.contains("htvc") || tvg.contains("htvc") || id.contains("htvc") ||
        name.contains("truyền hình tp") || name.contains("truyen hinh tp")
    ) return true

    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    channels: List<ChannelItem>,
    appStyle: AppStyle = AppStyle.MATERIAL_EXPRESSIVE,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onToggleAppStyle: () -> Unit = {},
    onSelectChannel: (ChannelItem) -> Unit,
    onOpenTvTab: () -> Unit,
    onOpenChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            channels.filter { ch ->
                ch.channelName.lowercase().contains(q) ||
                ch.groupTitle.lowercase().contains(q) ||
                ch.tvgName.lowercase().contains(q) ||
                ch.tvgId.lowercase().contains(q) ||
                (ch.currentProgramTitle?.lowercase()?.contains(q) == true) ||
                ch.countryFilter().displayName.lowercase().contains(q)
            }
        }
    }

    // Banner giới thiệu to ở Home: Lấy các kênh có lịch phát sóng (currentProgramTitle), tối đa 20 kênh
    val featuredSlideChannels: List<ChannelItem> = remember(channels) {
        if (channels.isEmpty()) emptyList()
        else {
            val withEpg = channels.filter { !it.currentProgramTitle.isNullOrBlank() }
            if (withEpg.isEmpty()) {
                val defaultFeatured = channels.filter { isNationalVtvChannel(it) || isHtvChannel(it) }
                if (defaultFeatured.isNotEmpty()) defaultFeatured.take(10) else channels.take(10)
            } else {
                val vtvWithEpg = withEpg.filter { isNationalVtvChannel(it) }
                val htvWithEpg = withEpg.filter { isHtvChannel(it) }
                val othersWithEpg = withEpg.filter { !isNationalVtvChannel(it) && !isHtvChannel(it) }
                val combined = mutableListOf<ChannelItem>()
                combined.addAll(vtvWithEpg)
                combined.addAll(htvWithEpg)
                combined.addAll(othersWithEpg)
                combined.distinctBy { it.streamUrl }.take(20)
            }
        }
    }

    val favorites = remember(channels) { channels.filter { it.isFavorite } }

    // Phân nhóm hiển thị thẳng toàn bộ kênh theo nhóm (không dùng bộ lọc ẩn kênh)
    val channelGroupSections = remember(channels, favorites) {
        val result = mutableListOf<Pair<String, List<ChannelItem>>>()

        // 1. Kênh yêu thích nếu có
        if (favorites.isNotEmpty()) {
            result.add(Pair("Kênh yêu thích của bạn ⭐", favorites))
        }

        // 2. Kênh VTV Quốc gia nếu có
        val vtv = channels.filter { isNationalVtvChannel(it) }
        if (vtv.isNotEmpty()) {
            result.add(Pair("Truyền hình Quốc gia VTV", vtv))
        }

        // 3. Kênh HTV TP.HCM nếu có
        val htv = channels.filter { isHtvChannel(it) }.sortedWith { a, b ->
            val numA = Regex("\\d+").find(a.channelName)?.value?.toIntOrNull() ?: 999
            val numB = Regex("\\d+").find(b.channelName)?.value?.toIntOrNull() ?: 999
            if (numA != numB) numA.compareTo(numB)
            else a.channelName.compareTo(b.channelName, ignoreCase = true)
        }
        if (htv.isNotEmpty()) {
            result.add(Pair("Đài Truyền hình TP.HCM (HTV)", htv))
        }

        // 4. Kênh VTVcab nếu có
        val cab = channels.filter { isVtvCabChannel(it) }
        if (cab.isNotEmpty()) {
            result.add(Pair("Truyền hình Cáp VTVcab", cab))
        }

        // 5. Gom các nhóm từ M3U (loại trừ các nhóm đã liệt kê ở trên nếu trùng tên)
        val addedNames = mutableSetOf(
            "Truyền hình Quốc gia VTV",
            "Đài Truyền hình TP.HCM (HTV)",
            "Truyền hình Cáp VTVcab",
            "Kênh yêu thích của bạn ⭐"
        )
        val m3uGroups = LinkedHashMap<String, MutableList<ChannelItem>>()
        for (ch in channels) {
            val rawGroup = ch.groupTitle.trim()
            val groupKey = if (rawGroup.isNotBlank()) rawGroup else "Kênh khác"
            m3uGroups.getOrPut(groupKey) { mutableListOf() }.add(ch)
        }

        for ((groupName, groupChannels) in m3uGroups) {
            if (!addedNames.contains(groupName)) {
                result.add(Pair(groupName, groupChannels))
            }
        }

        result
    }

    val quickKeywords = listOf("VTV1", "VTV3", "Bóng đá", "HTV7", "VTVcab", "Thời sự", "Sky News", "KBS World")

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(vmaTheme.background),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // =================================================================
            // 1. APP HEADER (VMA Logo)
            // =================================================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        VmaLogoImage(
                            modifier = Modifier.height(56.dp),
                            contentDescription = "VMA Logo"
                        )
                    }

                    // =========================================================
                    // 2. TRÌNH TÌM KIẾM KÊNH (SEARCH BAR - VẪN ĐƯỢC GIỮ LẠI NỔI BẬT)
                    // =========================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = vmaTheme.cardBackground,
                        border = BorderStroke(1.dp, if (isSearching) vmaTheme.primary else vmaTheme.cardBorder),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_bar_surface")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm kiếm kênh",
                                tint = if (isSearching) vmaTheme.primary else vmaTheme.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = "Tìm kiếm kênh (VTV1, HTV, Thể thao, Phim...)",
                                        color = vmaTheme.textMuted,
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = vmaTheme.textPrimary,
                                    unfocusedTextColor = vmaTheme.textPrimary,
                                    cursorColor = vmaTheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_channel_search_input")
                            )
                            if (isSearching) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = vmaTheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Suggested Search Keyword Chips (Horizontal scroll)
                    if (!isSearching) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickKeywords.forEach { kw ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(vmaTheme.cardBackground)
                                        .border(0.8.dp, vmaTheme.cardBorder, RoundedCornerShape(12.dp))
                                        .clickable { onSearchQueryChange(kw) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = kw,
                                        fontSize = 11.sp,
                                        color = vmaTheme.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. SEARCH RESULTS VIEW (khi người dùng gõ tìm kiếm)
            // =================================================================
            if (isSearching) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kết quả tìm kiếm (${searchResults.size} kênh)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = vmaTheme.textPrimary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Text(
                                text = "Xóa tìm kiếm",
                                color = vmaTheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onSearchQueryChange("") }
                            )
                        }

                        if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(vmaTheme.cardBackground)
                                    .border(1.dp, vmaTheme.cardBorder, RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Không tìm thấy kênh phù hợp với \"$searchQuery\"",
                                        color = vmaTheme.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Vui lòng thử tìm từ khóa khác như VTV1, HTV, Thể thao",
                                        color = vmaTheme.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            searchResults.forEach { ch ->
                                ChannelGridItem(
                                    channel = ch,
                                    onSelect = { onSelectChannel(ch) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // 4. HIỆN THẲNG TOÀN BỘ CÁC NHÓM KÊNH (BỎ HẾT BỘ LỌC)
                // =============================================================

                if (channels.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = vmaTheme.cardBackground),
                            border = BorderStroke(1.dp, vmaTheme.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(vmaTheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = vmaTheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = "Chưa có nguồn kênh nào",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = vmaTheme.textPrimary
                                )

                                Text(
                                    text = "Nhấn Cài đặt để thêm danh sách kênh M3U hoặc quét danh sách phát trực tuyến.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = vmaTheme.textMuted,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = onOpenSettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = vmaTheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mở Cài đặt nguồn kênh", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Banner trình chiếu kênh nổi bật
                if (featuredSlideChannels.isNotEmpty()) {
                    item {
                        FeaturedSlideBanner(
                            channels = featuredSlideChannels,
                            onSelectChannel = onSelectChannel
                        )
                    }
                }

                // Hiện thẳng từng nhóm kênh trực tiếp trên trang chủ
                channelGroupSections.forEach { (groupName, groupChannels) ->
                    if (groupChannels.isNotEmpty()) {
                        item(key = groupName) {
                            HomeSection(
                                title = groupName,
                                count = groupChannels.size,
                                channels = groupChannels,
                                onSelectChannel = onSelectChannel,
                                showEpg = groupChannels.any { !it.currentProgramTitle.isNullOrBlank() }
                            )
                        }
                    }
                }
            }

            // Footer branding
            item {
                VmaFooterBrand(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Slide Banner to ở Trang Chủ với HorizontalPager
 */
@Composable
private fun FeaturedSlideBanner(
    channels: List<ChannelItem>,
    onSelectChannel: (ChannelItem) -> Unit
) {
    val context = LocalContext.current
    val vmaTheme = LocalVmaTheme.current
    val pagerState = rememberPagerState(pageCount = { channels.size })

    // Auto scroll every 6 seconds
    LaunchedEffect(channels.size) {
        if (channels.size > 1) {
            while (true) {
                delay(6000)
                val nextPage = (pagerState.currentPage + 1) % channels.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) { page ->
            val ch = channels[page]
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onSelectChannel(ch) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Image or Gradient
                    if (!ch.currentProgramThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(ch.currentProgramThumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xD90F172A),
                                            Color(0xF20F172A)
                                        )
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            vmaTheme.primary.copy(alpha = 0.35f),
                                            Color(0xFF1E293B)
                                        )
                                    )
                                )
                        )
                    }

                    // Content overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Đang phát sóng" badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(FlowLiveRed)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FiberManualRecord,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = "TRỰC TIẾP",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Channel logo avatar
                            if (ch.tvgLogo.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(ch.tvgLogo)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = ch.channelName,
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Program & Channel Title
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = ch.channelName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val progTitle = ch.currentProgramTitle ?: ch.groupTitle
                            Text(
                                text = progTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Bottom Row with Equalizer and Play button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EqualizerBars(
                                color = FlowLiveRed,
                                barWidth = 2.5.dp,
                                maxHeight = 14.dp
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = vmaTheme.primary,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Xem ngay",
                                        fontSize = 11.sp,
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
    }
}

/**
 * Reusable Home Section for channel categories (VTV, HTV, VTVcab, etc.)
 */
@Composable
private fun HomeSection(
    title: String,
    count: Int,
    channels: List<ChannelItem>,
    onSelectChannel: (ChannelItem) -> Unit,
    showEpg: Boolean = false
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = vmaTheme.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count kênh",
                fontSize = 12.sp,
                color = vmaTheme.textMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels, key = { it.streamUrl }) { ch ->
                Surface(
                    onClick = { onSelectChannel(ch) },
                    shape = RoundedCornerShape(16.dp),
                    color = vmaTheme.cardBackground,
                    border = BorderStroke(1.dp, vmaTheme.cardBorder),
                    modifier = Modifier
                        .width(if (showEpg) 180.dp else 130.dp)
                        .height(125.dp)
                        .testTag("home_channel_card_${ch.channelName}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = vmaTheme.surface,
                                border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (ch.tvgLogo.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(ch.tvgLogo)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = ch.channelName,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = vmaTheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (ch.isFavorite) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = FlowFavoriteAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = ch.countryFilter().flag,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = ch.channelName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = vmaTheme.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = ch.currentProgramTitle ?: ch.groupTitle
                            Text(
                                text = sub,
                                fontSize = 11.sp,
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
    }
}

/**
 * Item card in Search / Filtered list
 */
@Composable
private fun ChannelGridItem(
    channel: ChannelItem,
    onSelect: () -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val context = LocalContext.current

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = vmaTheme.cardBackground,
        border = BorderStroke(1.dp, vmaTheme.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filtered_channel_${channel.channelName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = vmaTheme.surface,
                border = BorderStroke(0.8.dp, vmaTheme.cardBorder),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (channel.tvgLogo.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(channel.tvgLogo)
                                .crossfade(true)
                                .build(),
                            contentDescription = channel.channelName,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = vmaTheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.channelName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = vmaTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                val sub = channel.currentProgramTitle ?: channel.groupTitle
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = vmaTheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = channel.countryFilter().flag,
                fontSize = 16.sp
            )

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Phát",
                tint = vmaTheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
