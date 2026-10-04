package com.example.util

import android.accounts.AccountManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object FileManagerHelper {

    /**
     * Mở thư mục bản ghi trực tiếp trong ứng dụng Quản lý tệp (File Manager) của thiết bị
     * (Hỗ trợ Files by Google, Samsung My Files, Xiaomi File Manager, Android DocumentsUI,...)
     */
    fun openFolderInFileManager(context: Context, folder: File): Boolean {
        if (!folder.exists()) {
            folder.mkdirs()
        }

        // Cách 1: Sử dụng FileProvider và ACTION_VIEW
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                folder
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (_: Exception) {}

        // Cách 2: Thử mở các ứng dụng Quản lý File phổ biến đã cài trên máy
        val fileManagerPackages = listOf(
            "com.google.android.apps.nbu.files",  // Files by Google
            "com.google.android.documentsui",    // Android Files (Google)
            "com.android.documentsui",           // Android System Files
            "com.sec.android.app.myfiles",       // Samsung My Files
            "com.mi.android.globalFileexplorer", // Xiaomi File Manager
            "com.coloros.filemanager",           // Oppo File Manager
            "com.huawei.hidisk"                  // Huawei Files
        )

        for (pkg in fileManagerPackages) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Toast.makeText(
                        context,
                        "Đã mở Trình quản lý file. Thư mục: ${folder.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
            } catch (_: Exception) {}
        }

        // Cách 3: Thử ACTION_VIEW_DOWNLOADS nếu nằm trong Downloads
        try {
            val downloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (downloadIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(downloadIntent)
                return true
            }
        } catch (_: Exception) {}

        // Cách 4: Generic ACTION_GET_CONTENT hoặc mở thư mục ngoài
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(folder), "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (genericIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(genericIntent)
                return true
            }
        } catch (_: Exception) {}

        Toast.makeText(
            context,
            "Thư mục lưu trữ: ${folder.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    /**
     * Mở tệp video bản ghi bằng ứng dụng ngoài (Trình phát video hệ thống, VLC, MX Player, v.v.)
     */
    fun openFileWithExternalApp(context: Context, file: File): Boolean {
        if (!file.exists()) {
            Toast.makeText(context, "Tệp không tồn tại trên bộ nhớ máy", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Mở bản ghi bằng").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở tệp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * Chia sẻ tệp video qua các ứng dụng khác (Zalo, Telegram, Google Drive, Email,...)
     */
    fun shareFile(context: Context, file: File): Boolean {
        if (!file.exists()) {
            Toast.makeText(context, "Tệp không tồn tại", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Chia sẻ bản ghi PVR").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể chia sẻ tệp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * Tạo Intent mở hộp thoại chọn tài khoản Google đã đồng bộ trên máy (Google Services)
     */
    fun createGoogleAccountPickerIntent(): Intent? {
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Hiện file trực tiếp trong trình quản lý file hoặc mở thư mục chứa bản ghi
     */
    fun revealFileInFileManager(context: Context, file: File): Boolean {
        if (!file.exists()) {
            Toast.makeText(context, "Tệp không tồn tại: ${file.name}", Toast.LENGTH_SHORT).show()
            return false
        }
        val parentFolder = file.parentFile ?: file
        val opened = openFolderInFileManager(context, parentFolder)
        Toast.makeText(
            context,
            "Tệp: ${file.name}\nThư mục: ${parentFolder.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
        return opened
    }

    /**
     * Lấy danh sách các thư mục lưu trữ chuẩn trên thiết bị (Phim, Tải về, Tài liệu, Riêng tư)
     */
    fun getStandardStorageDirectories(context: Context): List<Pair<String, File>> {
        val list = mutableListOf<Pair<String, File>>()

        // 1. Thư mục Movies công khai
        val moviesPublic = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "VMA_Recordings")
        list.add(Pair("Thư mục Phim (Movies/VMA_Recordings)", moviesPublic))

        // 2. Thư mục Download công khai
        val downloadPublic = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VMA_Recordings")
        list.add(Pair("Thư mục Tải về (Download/VMA_Recordings)", downloadPublic))

        // 3. Thư mục Documents công khai
        val docsPublic = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "VMA_Recordings")
        list.add(Pair("Thư mục Tài liệu (Documents/VMA_Recordings)", docsPublic))

        // 4. Thư mục riêng của ứng dụng
        val appSpecificMovies = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "VMA_Recordings")
        list.add(Pair("Bộ nhớ riêng ứng dụng (An toàn, không cần quyền)", appSpecificMovies))

        return list
    }

    /**
     * Chuyển đổi SAF DocumentTree Uri thành đường dẫn tệp thực tế trên bộ nhớ
     */
    fun resolveStoragePath(context: Context, uriOrPath: String): String {
        val rawTrimmed = uriOrPath.trim()
        if (rawTrimmed.isBlank()) {
            val defaultDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "VMA_Recordings")
            if (!defaultDir.exists()) defaultDir.mkdirs()
            return defaultDir.absolutePath
        }

        val decoded = try {
            java.net.URLDecoder.decode(rawTrimmed, "UTF-8")
        } catch (_: Exception) {
            rawTrimmed
        }

        if (decoded.startsWith("/") && !decoded.startsWith("/tree/")) {
            val file = File(decoded)
            try {
                if (!file.exists()) file.mkdirs()
                if (file.canWrite()) return file.absolutePath
            } catch (_: Exception) {}
        }

        // Trường hợp là SAF Uri (content://... hoặc /tree/primary:...)
        try {
            val uri = if (rawTrimmed.startsWith("content://")) Uri.parse(rawTrimmed) else null
            val docId = if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    android.provider.DocumentsContract.getTreeDocumentId(uri)
                } catch (_: Exception) {
                    uri.lastPathSegment ?: ""
                }
            } else {
                decoded.removePrefix("/tree/").removePrefix("primary:")
            }

            val decodedDocId = try {
                java.net.URLDecoder.decode(docId, "UTF-8")
            } catch (_: Exception) {
                docId
            }

            if (decodedDocId.isNotBlank()) {
                val parts = decodedDocId.split(":")
                val relPath = if (parts.size > 1) parts[1].trim('/') else parts[0].trim('/')
                val externalRoot = Environment.getExternalStorageDirectory()
                val candidate = if (relPath.isNotBlank()) File(externalRoot, relPath) else File(externalRoot, "Movies/VMA_Recordings")
                if (!candidate.exists()) candidate.mkdirs()
                if (candidate.canWrite()) {
                    return candidate.absolutePath
                }
            }
        } catch (_: Exception) {}

        // Fallback an toàn vào ExternalFilesDir
        val safeDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "VMA_Recordings")
        if (!safeDir.exists()) safeDir.mkdirs()
        return safeDir.absolutePath
    }

    /**
     * Format dung lượng byte thành chuỗi dễ đọc (KB, MB, GB)
     */
    fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 1024) {
            String.format(Locale.getDefault(), "%.2f GB", mb / 1024)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }
}
