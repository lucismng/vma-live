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

            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "VMALiveTV/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return@withContext Result.failure(Exception("GitHub API HTTP $code (chưa có release hoặc repo chưa public)"))
            }

            val responseBody = response.body?.string() ?: ""
            if (responseBody.isBlank()) {
                return@withContext Result.failure(Exception("Phản hồi rỗng từ máy chủ"))
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
