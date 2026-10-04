package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelItem
import com.example.model.SyncState
import com.example.ui.components.VmaLogoImage
import com.example.ui.theme.LocalVmaTheme
import kotlinx.coroutines.delay

/**
 * Màn hình khởi động chuẩn thương hiệu VMA:
 * - Logo VMA không nền phóng to (250dp) với hiệu ứng thở (breathing pulse) và vầng sáng nhẹ
 * - Dòng chữ "TƯ LIỆU TRUYỀN THÔNG VIỆT NAM" được kéo sát lên chân logo
 * - Icon xoay tinh gọn
 * - Hiệu ứng chuyển nhá mượt mà sang giao diện chính
 */
@Composable
fun SplashScreen(
    channels: List<ChannelItem>,
    syncState: SyncState,
    onFinish: (Boolean) -> Unit
) {
    val vmaTheme = LocalVmaTheme.current
    val flashAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(channels, syncState) {
        if (!syncState.isSyncingM3u && channels.isNotEmpty()) {
            delay(1300)
            flashAlpha.animateTo(
                targetValue = 0.45f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            onFinish(true)
        } else if (!syncState.isSyncingM3u && channels.isEmpty()) {
            delay(2000)
            flashAlpha.animateTo(
                targetValue = 0.45f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            onFinish(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0C0D13),
                        Color(0xFF10121A),
                        Color(0xFF08090D)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // 1. Logo VMA kèm vầng sáng breathing halo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    vmaTheme.primary.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                VmaLogoImage(
                    modifier = Modifier
                        .size(250.dp)
                        .scale(pulseScale)
                        .testTag("splash_vma_logo"),
                    contentDescription = "Logo VMA",
                    contentScale = ContentScale.Fit
                )
            }

            // 2. Dòng chữ: TƯ LIỆU TRUYỀN THÔNG VIỆT NAM
            Text(
                text = "TƯ LIỆU TRUYỀN THÔNG VIỆT NAM",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = (-36).dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Icon xoay tinh gọn
            CircularProgressIndicator(
                color = vmaTheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(32.dp)
                    .offset(y = (-20).dp)
            )
        }

        // Lớp phủ hiệu ứng "nhá" sáng khi chuyển màn hình
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }
    }
}
