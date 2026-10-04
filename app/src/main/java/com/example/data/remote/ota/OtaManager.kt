package com.example.data.remote.ota

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OtaUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

class OtaManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "OtaManager"
        val DEFAULT_GITHUB_REPO: String get() = AppConfig.DEFAULT_GITHUB_REPO
        private const val PREFS_NAME = "vma_ota_prefs"
        private const val KEY_CUSTOM_REPO = "key_custom_github_repo"
        private const val KEY_LAST_CHECKED_VERSION = "key_last_checked_version"

        fun getRepository(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CUSTOM_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        }

        fun setRepository(context: Context, repo: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CUSTOM_REPO, repo.trim().removePrefix("https://github.com/")).apply()
        }
    }

    suspend fun checkForUpdates(context: Context, customRepo: String? = null): Result<OtaUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val repo = (customRepo ?: getRepository(context)).trim().removePrefix("https://github.com/").trim('/')
            if (repo.isBlank() || !repo.contains('/')) {
                return@withContext Result.failure(IllegalArgumentException("Định dạng repository GitHub không hợp lệ (cần dạng: user/repo)"))
            }

            // 1. Thử lấy bản phát hành chính thức mới nhất (/releases/latest)
            var response = client.newCall(
                Request.Builder()
                    .url("https://api.github.com/repos/$repo/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "VMALiveTV/${BuildConfig.VERSION_NAME}")
                    .build()
            ).execute()

            var responseBody: String? = null
            var isFromListFallback = false

            if (response.isSuccessful) {
                responseBody = response.body?.string()
            } else if (response.code == 404) {
                response.close()
                // Nếu /releases/latest trả về 404, có thể repo chỉ có Pre-release hoặc Draft.
                // Thử gọi /releases để lấy danh sách toàn bộ releases.
                val fallbackResponse = client.newCall(
                    Request.Builder()
                        .url("https://api.github.com/repos/$repo/releases")
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "VMALiveTV/${BuildConfig.VERSION_NAME}")
                        .build()
                ).execute()

                if (fallbackResponse.isSuccessful) {
                    val listBody = fallbackResponse.body?.string() ?: ""
                    fallbackResponse.close()
                    val releasesArray = org.json.JSONArray(listBody)
                    if (releasesArray.length() > 0) {
                        // Lấy release đầu tiên trong danh sách (kể cả pre-release)
                        responseBody = releasesArray.getJSONObject(0).toString()
                        isFromListFallback = true
                    } else {
                        return@withContext Result.failure(
                            Exception("Repo '$repo' chưa có bất kỳ bản Release nào trên GitHub. Vui lòng vào GitHub > Releases > 'Draft a new release' và ấn 'Publish release'.")
                        )
                    }
                } else {
                    val fbCode = fallbackResponse.code
                    fallbackResponse.close()
                    // Kiểm tra xem repo có tồn tại công khai không
                    val repoCheck = client.newCall(
                        Request.Builder()
                            .url("https://api.github.com/repos/$repo")
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("User-Agent", "VMALiveTV/${BuildConfig.VERSION_NAME}")
                            .build()
                    ).execute()
                    val repoExists = repoCheck.isSuccessful
                    repoCheck.close()

                    return@withContext if (!repoExists) {
                        Result.failure(
                            Exception("Không tìm thấy kho '$repo' (HTTP 404). Nguyên nhân: Kho chưa được tạo, sai tên tài khoản/tên kho, hoặc đang ở chế độ Private (cần đổi sang Public).")
                        )
                    } else {
                        Result.failure(
                            Exception("Kho '$repo' chưa có bản Release nào được xuất bản (Publish). Hãy tạo Release mới trên GitHub và đính kèm file APK.")
                        )
                    }
                }
            } else {
                val code = response.code
                response.close()
                return@withContext Result.failure(
                    Exception(
                        when (code) {
                            403 -> "Bị giới hạn lượt gọi GitHub API (HTTP 403 Rate Limit). Vui lòng thử lại sau vài phút."
                            else -> "GitHub API HTTP $code (chưa có release hoặc repo chưa public)"
                        }
                    )
                )
            }

            if (responseBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Phản hồi rỗng từ máy chủ GitHub"))
            }

            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName).ifBlank { tagName }
            val rawBody = json.optString("body", "").trim()
            val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

            // Find APK asset download URL if available
            var apkUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val finalDownloadUrl = apkUrl?.ifBlank { null } ?: htmlUrl
            val cleanTagVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

            val isNewer = isVersionNewer(cleanTagVersion, currentVersion)

            // Format concise changelog
            val formattedNotes = if (rawBody.isNotBlank()) {
                rawBody.lines()
                    .take(8)
                    .joinToString("\n")
            } else {
                "Cải tiến hiệu năng và sửa lỗi."
            }

            val updateInfo = OtaUpdateInfo(
                hasUpdate = isNewer,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = tagName.ifBlank { releaseName },
                releaseTitle = releaseName,
                releaseNotes = formattedNotes,
                downloadUrl = finalDownloadUrl,
                releasePageUrl = htmlUrl
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi kiểm tra cập nhật OTA: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * So sánh 2 chuỗi phiên bản dạng semver (ví dụ 1.1.0 vs 1.0.0 hoặc 1.2 vs 1.1.5)
     */
    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split('.').map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split('.').map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remoteVersion.isNotBlank() && remoteVersion != currentVersion
        }
    }
}
