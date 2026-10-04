package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.config.AppConfig
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.ProgramDao
import com.example.data.local.dao.RecordingDao
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ProgramEntity
import com.example.data.local.entity.RecordingEntity
import com.example.data.parser.EpgGzipParser
import com.example.data.parser.M3uParser
import com.example.data.remote.ota.OtaManager
import com.example.data.remote.ota.OtaUpdateInfo
import com.example.model.IptvPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class TvRepository(
    private val context: Context,
    private val channelDao: ChannelDao,
    private val programDao: ProgramDao,
    private val recordingDao: RecordingDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vma_prefs", Context.MODE_PRIVATE)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val DEFAULT_M3U_URL = ""
        val ADMIN_M3U_ONLINE_URL: String get() = AppConfig.ADMIN_M3U_ONLINE_URL
        val VMTTV_PLAYLIST_URL: String get() = AppConfig.VMTTV_PLAYLIST_URL
        val DEFAULT_EPG_URL: String get() = AppConfig.DEFAULT_EPG_URL
        val UPPERCASE_EPG_URL: String get() = AppConfig.UPPERCASE_EPG_URL
        val DETAILED_EPG_URL: String get() = AppConfig.DETAILED_EPG_URL
        val DEFAULT_EPG_GZ_URL: String get() = AppConfig.DEFAULT_EPG_GZ_URL
        val UPPERCASE_EPG_GZ_URL: String get() = AppConfig.UPPERCASE_EPG_GZ_URL
        val DETAILED_EPG_GZ_URL: String get() = AppConfig.DETAILED_EPG_GZ_URL

        private const val KEY_M3U_URL = "key_m3u_url_v5"
        private const val KEY_IPTV_PLAYLISTS = "key_iptv_playlists_v3"
        private const val KEY_LAST_SYNCED_M3U_URL = "key_last_synced_m3u_url"
        private const val KEY_EPG_URL = "key_epg_url"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
        private const val KEY_APP_STYLE = "key_app_style_v2"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_GEMINI_MODEL = "key_gemini_model"
        private const val KEY_IS_CHATBOT_ENABLED = "key_is_chatbot_enabled"
        private const val KEY_AI_SUBTITLE_ENABLED = "key_ai_subtitle_enabled"
        private const val KEY_AI_SUBTITLE_SOURCE_LANG = "key_ai_subtitle_source_lang"
        private const val KEY_AI_SUBTITLE_TRANSLATE_ENABLED = "key_ai_subtitle_translate_enabled"
        private const val KEY_AI_SUBTITLE_TARGET_LANG = "key_ai_subtitle_target_lang"

        private const val KEY_IS_AUTO_PIP_ENABLED = "key_is_auto_pip_enabled"
        private const val KEY_IS_BACKGROUND_AUDIO_ENABLED = "key_is_background_audio_enabled"

        private const val KEY_FIRST_RUN_CLEARED = "key_first_run_cleared_v4"
        private const val KEY_SETUP_COMPLETED = "key_setup_completed_v1"

        // PVR & Recording Configuration Keys
        private const val KEY_PVR_TARGET_DESTINATION = "key_pvr_target_destination"
        private const val KEY_PVR_LOCAL_PATH = "key_pvr_local_path"
        private const val KEY_GOOGLE_DRIVE_OAUTH_TOKEN = "key_google_drive_oauth_token"
        private const val KEY_PVR_KEEP_LOCAL_COPY = "key_pvr_keep_local_copy"
        private const val KEY_PVR_AUTO_STOP_MINUTES = "key_pvr_auto_stop_minutes"
        private const val KEY_GOOGLE_DRIVE_IS_SIGNED_IN = "key_google_drive_is_signed_in"
        private const val KEY_GOOGLE_DRIVE_USER_EMAIL = "key_google_drive_user_email"
        private const val KEY_GOOGLE_DRIVE_USER_NAME = "key_google_drive_user_name"
        private const val KEY_GOOGLE_DRIVE_QUOTA = "key_google_drive_quota"

        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
    }

    var isGoogleDriveSignedIn: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_DRIVE_IS_SIGNED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_IS_SIGNED_IN, value).apply()

    var googleDriveUserEmail: String
        get() = prefs.getString(KEY_GOOGLE_DRIVE_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DRIVE_USER_EMAIL, value.trim()).apply()

    var googleDriveUserName: String
        get() = prefs.getString(KEY_GOOGLE_DRIVE_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DRIVE_USER_NAME, value.trim()).apply()

    var googleDriveQuota: String
        get() = prefs.getString(KEY_GOOGLE_DRIVE_QUOTA, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DRIVE_QUOTA, value.trim()).apply()

    fun signInGoogleDrive(email: String, displayName: String, token: String = "", quota: String = "12.4 GB / 15.0 GB khả dụng") {
        isGoogleDriveSignedIn = true
        googleDriveUserEmail = email
        googleDriveUserName = displayName
        googleDriveQuota = quota
        if (token.isNotBlank()) {
            googleDriveOAuthToken = token
        }
        pvrTargetDestination = "GOOGLE_DRIVE"
    }

    fun signOutGoogleDrive() {
        isGoogleDriveSignedIn = false
        googleDriveUserEmail = ""
        googleDriveUserName = ""
        googleDriveQuota = ""
        googleDriveOAuthToken = ""
        pvrTargetDestination = "LOCAL"
    }

    var pvrTargetDestination: String
        get() = prefs.getString(KEY_PVR_TARGET_DESTINATION, "LOCAL") ?: "LOCAL"
        set(value) = prefs.edit().putString(KEY_PVR_TARGET_DESTINATION, value).apply()

    var pvrLocalPath: String
        get() = prefs.getString(KEY_PVR_LOCAL_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PVR_LOCAL_PATH, value.trim()).apply()

    var googleDriveOAuthToken: String
        get() = prefs.getString(KEY_GOOGLE_DRIVE_OAUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DRIVE_OAUTH_TOKEN, value.trim()).apply()

    var pvrKeepLocalCopy: Boolean
        get() = prefs.getBoolean(KEY_PVR_KEEP_LOCAL_COPY, true)
        set(value) = prefs.edit().putBoolean(KEY_PVR_KEEP_LOCAL_COPY, value).apply()

    var pvrAutoStopMinutes: Int
        get() = prefs.getInt(KEY_PVR_AUTO_STOP_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_PVR_AUTO_STOP_MINUTES, value).apply()

    fun getAllRecordings(): Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        recordingDao.deleteRecording(recording)
        try {
            val file = java.io.File(recording.filePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    var geminiApiKey: String
        get() {
            val savedKey = prefs.getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
            if (savedKey.isNotBlank()) return savedKey
            return try {
                val configKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                if (configKey.isNotBlank() &&
                    !configKey.equals("DEFAULT_GEMINI_API_KEY", ignoreCase = true) &&
                    !configKey.equals("null", ignoreCase = true)) {
                    configKey
                } else {
                    ""
                }
            } catch (e: Throwable) {
                ""
            }
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var isAiSubtitleEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_SUBTITLE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AI_SUBTITLE_ENABLED, value).apply()

    var aiSubtitleSourceLang: String
        get() = prefs.getString(KEY_AI_SUBTITLE_SOURCE_LANG, "Tiếng Việt") ?: "Tiếng Việt"
        set(value) = prefs.edit().putString(KEY_AI_SUBTITLE_SOURCE_LANG, value).apply()

    var isAiSubtitleTranslateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_SUBTITLE_TRANSLATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_SUBTITLE_TRANSLATE_ENABLED, value).apply()

    var aiSubtitleTargetLang: String
        get() = prefs.getString(KEY_AI_SUBTITLE_TARGET_LANG, "Tiếng Anh") ?: "Tiếng Anh"
        set(value) = prefs.edit().putString(KEY_AI_SUBTITLE_TARGET_LANG, value).apply()

    var isAutoPipEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_AUTO_PIP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_AUTO_PIP_ENABLED, value).apply()

    var isBackgroundAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_BACKGROUND_AUDIO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_BACKGROUND_AUDIO_ENABLED, value).apply()

    var isChatbotEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_CHATBOT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_CHATBOT_ENABLED, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    var appStyle: String
        get() = prefs.getString(KEY_APP_STYLE, "MATERIAL_EXPRESSIVE") ?: "MATERIAL_EXPRESSIVE"
        set(value) = prefs.edit().putString(KEY_APP_STYLE, value).apply()

    var m3uUrl: String
        get() {
            val saved = prefs.getString(KEY_M3U_URL, DEFAULT_M3U_URL) ?: DEFAULT_M3U_URL
            if (saved.contains("vietanhtv.top")) {
                prefs.edit().putString(KEY_M3U_URL, DEFAULT_M3U_URL).apply()
                return DEFAULT_M3U_URL
            }
            return saved
        }
        set(value) = prefs.edit().putString(KEY_M3U_URL, value).apply()

    var lastSyncedM3uUrl: String
        get() = prefs.getString(KEY_LAST_SYNCED_M3U_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNCED_M3U_URL, value).apply()

    var epgUrl: String
        get() = prefs.getString(KEY_EPG_URL, DEFAULT_EPG_URL) ?: DEFAULT_EPG_URL
        set(value) = prefs.edit().putString(KEY_EPG_URL, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        private set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    fun getAllChannels(): Flow<List<ChannelEntity>> = channelDao.getAllChannels()

    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>> =
        channelDao.getChannelsByGroup(group)

    fun getFavoriteChannels(): Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels()

    fun getDistinctGroups(): Flow<List<String>> = channelDao.getDistinctGroups()

    fun searchChannels(query: String): Flow<List<ChannelEntity>> = channelDao.searchChannels(query)

    suspend fun getChannelByUrl(streamUrl: String): ChannelEntity? =
        channelDao.getChannelByUrl(streamUrl)

    fun getProgramsForChannel(tvgId: String): Flow<List<ProgramEntity>> =
        programDao.getProgramsForChannel(tvgId)

    fun getAllProgramsInRange(startRange: Long, endRange: Long): Flow<List<ProgramEntity>> =
        programDao.getAllProgramsInRange(startRange, endRange)

    fun getCurrentProgramsForNow(now: Long): Flow<List<ProgramEntity>> =
        programDao.getCurrentProgramsForNow(now)

    fun getChannelCount(): Flow<Int> = channelDao.getChannelCount()

    fun getProgramCount(): Flow<Int> = programDao.getProgramCount()

    suspend fun setFavorite(streamUrl: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            channelDao.updateFavorite(streamUrl, isFavorite)
        }
    }

    var isFirstRunCleared: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN_CLEARED, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN_CLEARED, value).apply()

    suspend fun ensureFirstRunClean() = withContext(Dispatchers.IO) {
        if (!isFirstRunCleared) {
            channelDao.deleteAllChannels()
            savePlaylists(emptyList())
            isFirstRunCleared = true
        }
    }

    suspend fun clearAllChannels() {
        withContext(Dispatchers.IO) {
            channelDao.deleteAllChannels()
        }
    }

    fun getPlaylists(): List<IptvPlaylist> {
        val raw = prefs.getString(KEY_IPTV_PLAYLISTS, "") ?: ""
        return IptvPlaylist.listFromJson(raw)
    }

    fun savePlaylists(list: List<IptvPlaylist>) {
        prefs.edit().putString(KEY_IPTV_PLAYLISTS, IptvPlaylist.listToJson(list)).apply()
    }

    fun addPlaylist(name: String, url: String): IptvPlaylist {
        val trimmedUrl = url.trim()
        val trimmedName = name.trim().ifBlank { "Playlist ${System.currentTimeMillis() % 1000}" }
        val newPlaylist = IptvPlaylist(
            id = java.util.UUID.randomUUID().toString(),
            name = trimmedName,
            url = trimmedUrl,
            isEnabled = true
        )
        val current = getPlaylists().toMutableList()
        current.add(newPlaylist)
        savePlaylists(current)
        return newPlaylist
    }

    fun updatePlaylist(updated: IptvPlaylist) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            savePlaylists(current)
        }
    }

    fun togglePlaylist(id: String, isEnabled: Boolean) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = isEnabled)
            savePlaylists(current)
        }
    }

    fun deletePlaylist(id: String) {
        val current = getPlaylists().filter { it.id != id }
        savePlaylists(current)
    }

    fun restoreDefaultPlaylists(): List<IptvPlaylist> {
        val empty = emptyList<IptvPlaylist>()
        savePlaylists(empty)
        return empty
    }

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    private val otaManager = OtaManager(httpClient)

    suspend fun checkAppUpdate(customRepo: String? = null): Result<OtaUpdateInfo?> {
        return otaManager.checkForUpdates(context, customRepo)
    }

    fun getAdminM3uFile(): File {
        val file = File(context.filesDir, AppConfig.ADMIN_M3U_FILE_NAME)
        if (!file.exists()) {
            try {
                context.assets.open(AppConfig.ADMIN_M3U_FILE_NAME).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                file.writeText("#EXTM3U\n")
            }
        }
        return file
    }

    fun loadAdminM3uContent(): String {
        val file = getAdminM3uFile()
        return if (file.exists()) file.readText() else "#EXTM3U\n"
    }

    fun saveAdminM3uContent(content: String) {
        val file = getAdminM3uFile()
        file.writeText(content)
    }

    suspend fun activateAdminPlaylist(): Result<Int> = withContext(Dispatchers.IO) {
        val adminFile = getAdminM3uFile()
        if (!adminFile.exists() || adminFile.length() <= 10L) {
            try {
                val req = Request.Builder()
                    .url(ADMIN_M3U_ONLINE_URL)
                    .header("User-Agent", "VMA-Live/1.0 (Android; ADMIN)")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank() && body.contains("#EXTINF", ignoreCase = true)) {
                        adminFile.writeText(body)
                    }
                }
            } catch (_: Exception) {}
        }
        val adminPlaylist = IptvPlaylist(
            id = "admin_m3u",
            name = AppConfig.ADMIN_PLAYLIST_DISPLAY_NAME,
            url = "file://${adminFile.absolutePath}",
            isEnabled = true
        )
        val current = getPlaylists().toMutableList()
        current.removeAll { it.id == "admin_m3u" || it.id == "vmttv" || it.url == "file://${adminFile.absolutePath}" }
        current.add(0, adminPlaylist)
        savePlaylists(current)
        syncM3u()
    }

    suspend fun activateVmttvAdminList(): Result<Int> = activateAdminPlaylist()

    /**
     * Downloads and parses M3U playlists from ALL enabled playlists or default streams.
     * Combines channels across all sources, preserves user favorites.
     * Supports streaming real channels callback as they are parsed from the network/file.
     */
    suspend fun syncM3u(
        customUrl: String? = null,
        onChannelParsed: ((ChannelEntity) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val sourcesToFetch = mutableListOf<Pair<String, String>>() // Pair(name, url)

        if (!customUrl.isNullOrBlank()) {
            sourcesToFetch.add(Pair("Tùy chỉnh", customUrl))
        } else {
            val enabledPlaylists = getPlaylists().filter { it.isEnabled && it.url.isNotBlank() }
            for (pl in enabledPlaylists) {
                sourcesToFetch.add(Pair(pl.name, pl.url))
            }
        }

        if (sourcesToFetch.isEmpty()) {
            channelDao.deleteAllChannels()
            lastSyncTime = System.currentTimeMillis()
            return@withContext Result.success(0)
        }

        val allChannels = mutableListOf<ChannelEntity>()
        val updatedPlaylists = getPlaylists().toMutableList()
        var hasAtLeastOneSuccess = false
        var lastError: Exception? = null

        for ((name, url) in sourcesToFetch) {
            try {
                if (url.startsWith("file://")) {
                    val filePath = url.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists()) {
                        val parsed = file.inputStream().use { inputStream ->
                            M3uParser.parse(inputStream, onChannelParsed)
                        }
                        allChannels.addAll(parsed)
                        hasAtLeastOneSuccess = true

                        val idx = updatedPlaylists.indexOfFirst { it.url == url }
                        if (idx != -1) {
                            updatedPlaylists[idx] = updatedPlaylists[idx].copy(
                                channelCount = parsed.size,
                                lastSyncTime = System.currentTimeMillis()
                            )
                        }
                    } else {
                        lastError = Exception("Không tìm thấy tệp cục bộ: $filePath")
                    }
                } else {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "VMA-Live/1.1 (Android; IPTV)")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val parsed = body.byteStream().use { inputStream ->
                                M3uParser.parse(inputStream, onChannelParsed)
                            }
                            allChannels.addAll(parsed)
                            hasAtLeastOneSuccess = true

                            // Cập nhật số kênh vào playlist nếu là playlist tùy chỉnh
                            val idx = updatedPlaylists.indexOfFirst { it.url == url }
                            if (idx != -1) {
                                updatedPlaylists[idx] = updatedPlaylists[idx].copy(
                                    channelCount = parsed.size,
                                    lastSyncTime = System.currentTimeMillis()
                                )
                            }
                        }
                    } else {
                        lastError = Exception("HTTP ${response.code} khi tải $name")
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (!hasAtLeastOneSuccess && allChannels.isEmpty()) {
            return@withContext Result.failure(lastError ?: Exception("Không thể nạp danh sách kênh"))
        }

        // Deduplicate channels by streamUrl while preserving order
        val distinctChannels = mutableListOf<ChannelEntity>()
        val seenUrls = mutableSetOf<String>()
        for (ch in allChannels) {
            if (seenUrls.add(ch.streamUrl)) {
                distinctChannels.add(ch.copy(orderIndex = distinctChannels.size))
            }
        }

        // Preserve favorite status for existing channels
        val existingFavorites = channelDao.getFavoriteChannels().firstOrNull()
            ?.associateBy { it.streamUrl } ?: emptyMap()

        val mergedChannels = distinctChannels.map { channel ->
            val existing = existingFavorites[channel.streamUrl]
            if (existing != null) {
                channel.copy(isFavorite = existing.isFavorite)
            } else {
                channel
            }
        }.toMutableList()

        channelDao.deleteAllChannels()
        channelDao.insertChannels(mergedChannels)
        savePlaylists(updatedPlaylists)
        lastSyncTime = System.currentTimeMillis()
        Result.success(mergedChannels.size)
    }

    /**
     * Downloads and parses EPG (GZIP or raw XML) stream in batches on Dispatchers.IO.
     * Deletes programs older than 24 hours.
     * Supports real-time program count reporting via onProgramsBatch callback.
     */
    suspend fun syncEpg(
        customUrl: String? = null,
        onProgramsBatch: ((Int) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val targetUrl = customUrl ?: epgUrl
        try {
            // Cleanup programs older than 24 hours
            val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            programDao.deleteOldPrograms(threshold)

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "VMA-Live/1.0 (Android; EPG)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body response"))
            var totalPrograms = 0

            body.byteStream().use { inputStream ->
                EpgGzipParser.parseStreaming(inputStream) { batch ->
                    programDao.insertPrograms(batch)
                    totalPrograms += batch.size
                    onProgramsBatch?.invoke(totalPrograms)
                }
            }

            lastSyncTime = System.currentTimeMillis()
            Result.success(totalPrograms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
