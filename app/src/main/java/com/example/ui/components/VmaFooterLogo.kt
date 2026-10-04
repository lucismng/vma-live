package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.config.AppConfig
import com.example.ui.theme.LocalVmaTheme

/**
 * =========================================================================
 * THƯƠNG HIỆU & LOGO VMA:
 * Mọi cấu hình Logo, EPG, Admin M3U, Tài khoản quản trị đã được gom chung về:
 * File: app/src/main/java/com/example/config/AppConfig.kt
 * =========================================================================
 */
object AppBrandConfig {
    /**
     * Tham chiếu trực tiếp từ AppConfig.LOGO_URL để dễ dàng quản lý tập trung 1 file duy nhất.
     */
    val LOGO_URL: String get() = AppConfig.LOGO_URL
}

/**
 * Component hiển thị Logo VMA linh hoạt:
 * - Tự động tải từ AppConfig.LOGO_URL
 * - Tự động fallback về AppConfig.LOCAL_FALLBACK_LOGO_RES nếu offline
 */
@Composable
fun VmaLogoImage(
    modifier: Modifier = Modifier,
    contentDescription: String = AppConfig.BRAND_NAME,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val imageSource = if (AppConfig.LOGO_URL.isNotBlank()) {
        AppConfig.LOGO_URL
    } else {
        AppConfig.LOCAL_FALLBACK_LOGO_RES
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageSource)
            .placeholder(AppConfig.LOCAL_FALLBACK_LOGO_RES)
            .error(AppConfig.LOCAL_FALLBACK_LOGO_RES)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Khung Footer Logo VMA kèm thông tin phiên bản
 * Thêm cơ chế ẩn: khi nhấn liên tục 5 lần vào logo VMA ở chân trang, mở hộp thoại đăng nhập quản trị
 */
@Composable
fun VmaFooterBrand(
    modifier: Modifier = Modifier,
    versionText: String = "Phiên bản: ${AppConfig.VERSION_NAME}",
    subtitleText: String = AppConfig.FOOTER_SUBTITLE,
    onSecretAdminTap: () -> Unit = {}
) {
    val vmaTheme = LocalVmaTheme.current
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo VMA cùng dòng chữ TƯ LIỆU TRUYỀN THÔNG VIỆT NAM bên cạnh (Nhấn 5 lần để mở Admin)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 1800L) {
                        tapCount++
                        if (tapCount >= 5) {
                            tapCount = 0
                            onSecretAdminTap()
                        }
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = now
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            VmaLogoImage(
                modifier = Modifier.height(34.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(26.dp)
                    .background(vmaTheme.textMuted.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "TƯ LIỆU TRUYỀN THÔNG VIỆT NAM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = vmaTheme.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = versionText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = vmaTheme.textMuted
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitleText,
            fontSize = 11.sp,
            color = vmaTheme.textMuted.copy(alpha = 0.7f)
        )
    }
}
