package com.example.config

import com.example.R

/**
 * =========================================================================================
 *                   TỆP CẤU HÌNH TRUNG TÂM TOÀN BỘ ỨNG DỤNG VMA LIVE TV
 *                                (CENTRAL APP CONFIG)
 * =========================================================================================
 * Tệp này gom tất cả các thông số có thể tuỳ chỉnh, cập nhật và chỉnh sửa khẩn cấp:
 *  1. Link danh sách kênh M3U Admin & Tên tệp
 *  2. Các đường dẫn nguồn lịch phát sóng EPG (XML / XML.GZ)
 *  3. Danh sách tài khoản đăng nhập quản trị viên (Admin accounts)
 *  4. Đường dẫn Logo online, ảnh logo dự phòng và thông tin thương hiệu
 *  5. Kho mã nguồn cập nhật OTA GitHub & Model AI mặc định
 *
 * Mọi thay đổi tại đây sẽ được toàn bộ hệ thống áp dụng đồng bộ ngay lập tức!
 * =========================================================================================
 */
object AppConfig {

    // =====================================================================================
    // 1. CẤU HÌNH DANH SÁCH KÊNH QUẢN TRỊ (ADMIN M3U PLAYLIST)
    // =====================================================================================
    /**
     * Link online tải tệp M3U Admin (khi cần cập nhật nhanh từ xa qua GitHub hoặc server riêng).
     */
    const val ADMIN_M3U_ONLINE_URL: String = "https://raw.githubusercontent.com/lucismng/vma-live/main/ADMIN.m3u"

    /**
     * Tên tệp M3U Admin lưu trong assets và bộ nhớ ứng dụng.
     */
    const val ADMIN_M3U_FILE_NAME: String = "ADMIN.m3u"

    /**
     * Tên hiển thị của playlist quản trị trong danh sách playlist của người dùng.
     */
    const val ADMIN_PLAYLIST_DISPLAY_NAME: String = "Kênh Quản Trị (ADMIN.m3u)"

    /**
     * Alias cho tương thích ngược
     */
    const val VMTTV_PLAYLIST_URL: String = ADMIN_M3U_ONLINE_URL


    // =====================================================================================
    // 2. CẤU HÌNH NGUỒN LỊCH PHÁT SÓNG EPG (ELECTRONIC PROGRAM GUIDE)
    // =====================================================================================
    /**
     * Nguồn lịch EPG mặc định (chữ thường - khuyên dùng cho tốc độ cao và đầy đủ kênh)
     */
    const val DEFAULT_EPG_URL: String = "https://epg.io.vn/epg.xml"

    /**
     * Nguồn lịch EPG nén GZIP siêu nhẹ (tiết kiệm băng thông, tải nhanh)
     */
    const val DEFAULT_EPG_GZ_URL: String = "https://epg.io.vn/epg.xml.gz"

    /**
     * Nguồn lịch EPG tiêu đề in hoa (chữ HOA nổi bật)
     */
    const val UPPERCASE_EPG_URL: String = "https://epg.io.vn/epgu.xml"

    /**
     * Nguồn lịch EPG tiêu đề in hoa dạng GZIP
     */
    const val UPPERCASE_EPG_GZ_URL: String = "https://epg.io.vn/epgu.xml.gz"

    /**
     * Nguồn lịch EPG chi tiết (kèm tóm tắt nội dung chương trình)
     */
    const val DETAILED_EPG_URL: String = "https://epg.io.vn/epgc.xml"

    /**
     * Nguồn lịch EPG chi tiết dạng GZIP
     */
    const val DETAILED_EPG_GZ_URL: String = "https://epg.io.vn/epgc.xml.gz"


    // =====================================================================================
    // 3. CẤU HÌNH TÀI KHOẢN ĐĂNG NHẬP QUẢN TRỊ (ADMIN ACCOUNTS)
    // =====================================================================================
    data class AdminAccount(
        val username: String,
        val password: String,
        val description: String = "Quản trị viên"
    )

    /**
     * Danh sách tất cả tài khoản được phép đăng nhập vào khu vực quản trị viên:
     * Dễ dàng thêm, bớt hoặc đổi mật khẩu tài khoản trực tiếp tại danh sách này khi cần khẩn cấp.
     */
    val ADMIN_ACCOUNTS: List<AdminAccount> = listOf(
        AdminAccount(
            username = "admin",
            password = "admin123",
            description = "Tài khoản quản trị chính"
        ),
        AdminAccount(
            username = "adminvma",
            password = "vmahomnay",
            description = "Tài khoản kỹ thuật VMA"
        )
    )

    /**
     * Hàm kiểm tra thông tin tài khoản đăng nhập quản trị
     */
    fun isValidAdmin(user: String, pass: String): Boolean {
        val cleanUser = user.trim()
        val cleanPass = pass.trim()
        return ADMIN_ACCOUNTS.any { it.username.equals(cleanUser, ignoreCase = true) && it.password == cleanPass }
    }


    // =====================================================================================
    // 4. CẤU HÌNH LOGO & THƯƠNG HIỆU (BRANDING & LOGO)
    // =====================================================================================
    /**
     * Đường link ảnh Logo chính của ứng dụng tải online (dán link mới vào đây để đổi logo ngay lập tức).
     */
    const val LOGO_URL: String = "https://i.ibb.co/7Jzpw3wz/vma.png"

    /**
     * Tài nguyên ảnh logo cục bộ dự phòng (offline hoặc khi mạng mất kết nối).
     */
    val LOCAL_FALLBACK_LOGO_RES: Int = R.drawable.logovma

    /**
     * Tên thương hiệu hiển thị
     */
    const val BRAND_NAME: String = "VMA Live TV"

    /**
     * Slogan thương hiệu hiển thị dưới logo
     */
    const val BRAND_SLOGAN: String = "TƯ LIỆU TRUYỀN THÔNG VIỆT NAM"

    /**
     * Thông tin phiên bản hiển thị tại footer
     */
    const val VERSION_NAME: String = "1.0.1"

    /**
     * Phụ đề chân trang (Footer subtitle)
     */
    const val FOOTER_SUBTITLE: String = "VMA Live TV • IPTV & EPG Streaming Engine"


    // =====================================================================================
    // 5. CẤU HÌNH CẬP NHẬT OTA GITHUB & AI KHẨN CẤP
    // =====================================================================================
    /**
     * Kho GitHub chứa bản phát hành OTA
     */
    const val DEFAULT_GITHUB_REPO: String = "lucismng/vma-live"

    /**
     * Mô hình Gemini AI mặc định sử dụng
     */
    const val DEFAULT_GEMINI_MODEL: String = "gemini-3.5-flash"
}
