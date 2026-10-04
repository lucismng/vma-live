# VMA Live TV - Trình Phát Truyền Hình Trực Tuyến & EPG Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Version](https://img.shields.io/badge/Version-v1.0.1-blue.svg)](https://github.com/lucismng/vma-live-tv/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/ExoPlayer-Media3-red.svg)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**VMA Live TV (v1.0.1)** là ứng dụng truyền hình trực tuyến chuyên nghiệp và phát lại chương trình hiện đại dành cho hệ điều hành Android, được phát triển bằng **Kotlin** và bộ công cụ **Jetpack Compose (Material Design 3)**. Ứng dụng mang đến trải nghiệm truyền hình mượt mà, độ trễ thấp, hỗ trợ danh sách kênh M3U/M3U8 phong phú, lịch phát sóng điện tử XMLTV (EPG) thời gian thực, ghi hình PVR đa luồng ngầm và cơ chế cập nhật phiên bản mới OTA tiện lợi qua GitHub Releases.

---

## 🌟 Tính Năng Nổi Bật (Phiên Bản 1.0.1)

### 1. Trình Phát Media3 (ExoPlayer) Siêu Ổn Định & Chống Văng (Crash-Proof)
- **Hỗ trợ toàn diện định dạng stream:** HLS (`.m3u8`), MPEG-DASH (`.mpd`), SmoothStreaming và Transport Stream (`.ts`).
- **Cơ chế phục hồi sự cố tự động:** Xử lý ngoại lệ kết nối an toàn, fallback MediaCodec giải mã mượt mà, hỗ trợ redirect chéo giao thức (HTTP ➔ HTTPS) và User-Agent tương thích VLC/Chrome.
- **Tùy chỉnh tỉ lệ khung hình:** Chuyển đổi linh hoạt giữa 16:9 (Chuẩn), 4:3 (SD), 21:9 (Ultrawide), Stretch (Lấp đầy) và Zoom (Cắt viền).
- **Chế độ cửa sổ thu nhỏ (Picture-in-Picture - PiP):** Tự động kích hoạt PiP khi thoát ra màn hình chính (Android 12+) và phát âm thanh trong nền khi tắt màn hình.

### 2. Trình Hướng Dẫn Thiết Lập Lần Đầu (Setup Wizard)
- Quy trình 4 bước tinh gọn được đánh số (1 - 2 - 3 - 4) với hiệu ứng chuyển trang (transition animations) mượt mà:
  1. **Chào mừng:** Giới thiệu tính năng cốt lõi và giao diện thương hiệu VMA.
  2. **Thêm nguồn kênh M3U:** Hỗ trợ nhập liên kết URL M3U ngoài hoặc tải tệp M3U từ bộ nhớ máy.
  3. **Thêm luồng EPG:** Lựa chọn các luồng lịch phát sóng chuẩn hoặc nhập URL EPG riêng.
  4. **Dò kênh tự động:** Trình diễn tiến trình quét kênh trực quan, hiển thị thống kê tổng số kênh, số chương trình EPG và danh sách tên kênh dò được theo thời gian thực.
- Hỗ trợ mở lại màn hình dò kênh bất kỳ lúc nào trong tab Cài đặt hoặc khi thêm nguồn kênh mới.

### 3. Tự Động Cập Nhật Lịch Phát Sóng (EPG) Khi Mở App
- Mỗi khi khởi động ứng dụng, hệ thống tự động đồng bộ ngay lập tức dữ liệu XMLTV mới nhất trong nền.
- Hỗ trợ đa dạng biến thể từ `epg.io.vn`:
  - `epg.xml` (Tiêu đề chuẩn chữ thường - Khuyên dùng)
  - `epgu.xml` (Tiêu đề in hoa nổi bật)
  - `epgc.xml` (Tiêu đề kèm mô tả chi tiết chương trình)
  - Các bản nén gzip (`.xml.gz`) siêu nhẹ, tiết kiệm tối đa băng thông mạng.
- Thanh tiến trình thời gian thực (Live Progress Bar) cho biết % thời lượng chương trình đang phát và lịch chiếu sắp tới.

### 4. Quản Lý Nguồn Kênh & Kênh Quản Trị (ADMIN.m3u)
- **Quản lý danh sách phát đa luồng:** Thêm nhiều playlist M3U cùng lúc, bật/tắt từng nguồn, gộp danh sách kênh và đồng bộ nhanh chóng.

### 5. Ghi Hình Truyền Hình Đa Luồng (Multi-Channel PVR)
- Ghi hình nhiều kênh TV đồng thời trong nền mà không làm gián đoạn việc xem kênh khác.
- Hiển thị thời gian thực (⏱) và dung lượng file đã ghi (💾) cho từng kênh đang chạy.
- Hỗ trợ dừng riêng lẻ từng kênh hoặc dừng tất cả chỉ với một chạm.

### 6. Cập Nhật Tự Động Từ Xa (OTA qua GitHub Releases)
- Kiểm tra bản phát hành mới nhất mỗi khi mở ứng dụng qua GitHub Releases API (`repos/{owner}/{repo}/releases/latest`).
- Khi có bản cập nhật mới, hiển thị hộp thoại pop-up thông báo rõ ràng số hiệu phiên bản mới (`v1.0.1`,...), nội dung nhật ký thay đổi ngắn gọn (Changelog) và nút tải trực tiếp file APK.

---

## 🛠 Kiến Trúc & Công Nghệ

- **Ngôn ngữ:** Kotlin 2.0+
- **Giao diện người dùng:** Jetpack Compose, Material Design 3 (M3)
- **Kiến trúc ứng dụng:** Model-View-ViewModel (MVVM) + Repository Pattern
- **Xử lý bất đồng bộ:** Kotlin Coroutines & StateFlow
- **Trình phát đa phương tiện:** AndroidX Media3 (ExoPlayer 1.4+)
- **Cơ sở dữ liệu cục bộ:** Room Database (Kênh, EPG và lịch sử ghi hình PVR)
- **Mạng & HTTP:** OkHttp 4.x
- **Tải ảnh & Logo:** Coil Compose (AsyncImage)

---

## 🚀 Hướng Dẫn Biên Dịch & Cài Đặt

### Yêu Cầu Môi Trường
- **Android Studio:** Hedgehog (2023.1.1) trở lên
- **JDK:** Java 17 hoặc Java 21
- **Android SDK:** Compile SDK 36, Min SDK 24 (Android 7.0+)

### Các Bước Biên Dịch

1. **Clone repository về máy tính:**
   ```bash
   git clone https://github.com/lucismng/vma-live-tv.git
   cd vma-live-tv
   ```

2. **Biên dịch bản Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   Tệp APK sau khi xuất sẽ nằm tại:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Chạy kiểm thử Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Cài đặt vào thiết bị qua ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Cấu Hình Mặc Định

| Thành phần | Cấu hình | Mô tả |
| :--- | :--- | :--- |
| **EPG XMLTV** | `https://epg.io.vn/epg.xml` | Lịch phát sóng truyền hình Việt Nam |
| **OTA Releases** | `lucismng/vma-live-tv` | Kho lưu trữ GitHub kiểm tra bản cập nhật APK |

---

## 📄 Bản Quyền & Tuyên Bố Miễn Trừ Trách Nhiệm

- Ứng dụng này là một trình phát đa phương tiện (Media Player) và không tự lưu trữ hoặc phát sóng bất kỳ nội dung âm thanh/video có bản quyền nào.
- Toàn bộ danh sách kênh và lịch phát sóng phụ thuộc vào các nguồn liên kết do người dùng tự nhập hoặc cấu hình.
