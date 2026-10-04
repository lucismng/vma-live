package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.data.local.VmaDatabase
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ProgramEntity
import com.example.data.local.entity.RecordingEntity
import com.example.data.remote.drive.DriveConnectionStatus
import com.example.data.remote.drive.GoogleDriveService
import com.example.data.remote.gemini.GeminiAiService
import com.example.data.remote.gemini.GeminiDiagnosticResult
import com.example.data.repository.TvRepository
import com.example.model.ChannelItem
import com.example.model.IptvPlaylist
import com.example.model.ProgramItem
import com.example.model.SyncState
import com.example.receiver.ProgramReminderReceiver
import com.example.service.pvr.PvrStreamRecorder
import com.example.ui.theme.AppStyle
import com.example.util.CatchupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VmaDatabase.getInstance(application)
    val repository = TvRepository(application, db.channelDao(), db.programDao(), db.recordingDao())
    val googleDriveService = GoogleDriveService()
    val pvrRecorder = PvrStreamRecorder(application, db.recordingDao(), googleDriveService, viewModelScope)

    // PVR & Recordings State
    val recordings: StateFlow<List<RecordingEntity>> = repository.getAllRecordings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isPvrRecording = pvrRecorder.isRecording
    val pvrActiveRecordings = pvrRecorder.activeRecordings
    val pvrRecordingChannel = pvrRecorder.recordingChannel
    val pvrDurationSeconds = pvrRecorder.recordedDurationSeconds
    val pvrRecordedBytes = pvrRecorder.recordedBytes
    val pvrActiveRecording = pvrRecorder.activeRecording
    val pvrUploadProgress = pvrRecorder.uploadProgress

    private val _pvrTargetDestination = MutableStateFlow(repository.pvrTargetDestination)
    val pvrTargetDestination: StateFlow<String> = _pvrTargetDestination.asStateFlow()

    private val _pvrLocalPath = MutableStateFlow(repository.pvrLocalPath)
    val pvrLocalPath: StateFlow<String> = _pvrLocalPath.asStateFlow()

    private val _googleDriveToken = MutableStateFlow(repository.googleDriveOAuthToken)
    val googleDriveToken: StateFlow<String> = _googleDriveToken.asStateFlow()

    private val _isGoogleDriveSignedIn = MutableStateFlow(repository.isGoogleDriveSignedIn)
    val isGoogleDriveSignedIn: StateFlow<Boolean> = _isGoogleDriveSignedIn.asStateFlow()

    private val _googleDriveUserEmail = MutableStateFlow(repository.googleDriveUserEmail)
    val googleDriveUserEmail: StateFlow<String> = _googleDriveUserEmail.asStateFlow()

    private val _googleDriveUserName = MutableStateFlow(repository.googleDriveUserName)
    val googleDriveUserName: StateFlow<String> = _googleDriveUserName.asStateFlow()

    private val _googleDriveQuota = MutableStateFlow(repository.googleDriveQuota)
    val googleDriveQuota: StateFlow<String> = _googleDriveQuota.asStateFlow()

    private val _pvrKeepLocalCopy = MutableStateFlow(repository.pvrKeepLocalCopy)
    val pvrKeepLocalCopy: StateFlow<Boolean> = _pvrKeepLocalCopy.asStateFlow()

    private val _pvrAutoStopMinutes = MutableStateFlow(repository.pvrAutoStopMinutes)
    val pvrAutoStopMinutes: StateFlow<Int> = _pvrAutoStopMinutes.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Gemini AI Configuration
    private val _geminiApiKey = MutableStateFlow(repository.geminiApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(repository.geminiModel)
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    // AI Subtitles & Translation (temporarily disabled as requested by user)
    private val _isAiSubtitleEnabled = MutableStateFlow(false)
    val isAiSubtitleEnabled: StateFlow<Boolean> = _isAiSubtitleEnabled.asStateFlow()

    private val _aiSubtitleSourceLang = MutableStateFlow(repository.aiSubtitleSourceLang)
    val aiSubtitleSourceLang: StateFlow<String> = _aiSubtitleSourceLang.asStateFlow()

    private val _isAiSubtitleTranslateEnabled = MutableStateFlow(repository.isAiSubtitleTranslateEnabled)
    val isAiSubtitleTranslateEnabled: StateFlow<Boolean> = _isAiSubtitleTranslateEnabled.asStateFlow()

    private val _aiSubtitleTargetLang = MutableStateFlow(repository.aiSubtitleTargetLang)
    val aiSubtitleTargetLang: StateFlow<String> = _aiSubtitleTargetLang.asStateFlow()

    private val _aiSubtitleOriginalText = MutableStateFlow("")
    val aiSubtitleOriginalText: StateFlow<String> = _aiSubtitleOriginalText.asStateFlow()

    private val _aiSubtitleTranslatedText = MutableStateFlow<String?>("")
    val aiSubtitleTranslatedText: StateFlow<String?> = _aiSubtitleTranslatedText.asStateFlow()

    private val _isAiSubtitleLoading = MutableStateFlow(false)
    val isAiSubtitleLoading: StateFlow<Boolean> = _isAiSubtitleLoading.asStateFlow()

    private var aiSubtitleJob: Job? = null
    private val subtitleHistory = mutableListOf<String>()

    private val _selectedGroup = MutableStateFlow("Tất cả")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow(com.example.model.CountryFilter.ALL)
    val selectedCountryFilter: StateFlow<com.example.model.CountryFilter> = _selectedCountryFilter.asStateFlow()

    private val _isAutoPipEnabled = MutableStateFlow(repository.isAutoPipEnabled)
    val isAutoPipEnabled: StateFlow<Boolean> = _isAutoPipEnabled.asStateFlow()

    private val _isBackgroundAudioEnabled = MutableStateFlow(repository.isBackgroundAudioEnabled)
    val isBackgroundAudioEnabled: StateFlow<Boolean> = _isBackgroundAudioEnabled.asStateFlow()

    fun selectCountryFilter(country: com.example.model.CountryFilter) {
        _selectedCountryFilter.value = country
    }

    fun setAutoPipEnabled(enabled: Boolean) {
        _isAutoPipEnabled.value = enabled
        repository.isAutoPipEnabled = enabled
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _isBackgroundAudioEnabled.value = enabled
        repository.isBackgroundAudioEnabled = enabled
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ChannelItem?>(null)
    val selectedChannel: StateFlow<ChannelItem?> = _selectedChannel.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isDarkMode = MutableStateFlow(repository.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _appStyle = MutableStateFlow(
        try {
            AppStyle.valueOf(repository.appStyle)
        } catch (e: Exception) {
            AppStyle.MATERIAL_EXPRESSIVE
        }
    )
    val appStyle: StateFlow<AppStyle> = _appStyle.asStateFlow()

    private val _isChatbotEnabled = MutableStateFlow(repository.isChatbotEnabled)
    val isChatbotEnabled: StateFlow<Boolean> = _isChatbotEnabled.asStateFlow()

    fun setChatbotEnabled(enabled: Boolean) {
        _isChatbotEnabled.value = enabled
        repository.isChatbotEnabled = enabled
    }

    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        repository.isDarkMode = newVal
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        repository.isDarkMode = dark
    }

    fun toggleAppStyle() {
        _appStyle.value = AppStyle.MATERIAL_EXPRESSIVE
        repository.appStyle = AppStyle.MATERIAL_EXPRESSIVE.name
    }

    fun setAppStyle(style: AppStyle) {
        _appStyle.value = AppStyle.MATERIAL_EXPRESSIVE
        repository.appStyle = AppStyle.MATERIAL_EXPRESSIVE.name
    }

    private val _isSetupCompleted = MutableStateFlow(repository.isSetupCompleted)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _otaUpdateInfo = MutableStateFlow<com.example.data.remote.ota.OtaUpdateInfo?>(null)
    val otaUpdateInfo: StateFlow<com.example.data.remote.ota.OtaUpdateInfo?> = _otaUpdateInfo.asStateFlow()

    private val _isCheckingOta = MutableStateFlow(false)
    val isCheckingOta: StateFlow<Boolean> = _isCheckingOta.asStateFlow()

    fun completeSetup() {
        repository.isSetupCompleted = true
        _isSetupCompleted.value = true
    }

    fun resetSetup() {
        repository.isSetupCompleted = false
        _isSetupCompleted.value = false
    }

    fun dismissOtaDialog() {
        _otaUpdateInfo.value = null
    }

    fun checkOtaUpdate(manual: Boolean = false, customRepo: String? = null, onResult: ((com.example.data.remote.ota.OtaUpdateInfo?, String?) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingOta.value = true
            val result = repository.checkAppUpdate(customRepo)
            _isCheckingOta.value = false
            if (result.isSuccess) {
                val info = result.getOrNull()
                if (info != null && info.hasUpdate) {
                    _otaUpdateInfo.value = info
                }
                onResult?.invoke(info, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Lỗi kiểm tra bản cập nhật"
                onResult?.invoke(null, err)
            }
        }
    }

    private val _currentTimeTicker = MutableStateFlow(System.currentTimeMillis())

    // Ticker every 15 seconds to update real-time live EPG progress
    init {
        viewModelScope.launch {
            repository.ensureFirstRunClean()
            reloadPlaylists()
            // Tự động cập nhật ngay EPG mỗi khi mở app
            syncEpgOnly()
            // Tự động kiểm tra bản cập nhật OTA từ GitHub Release khi mở app
            checkOtaUpdate(manual = false)
        }
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                _currentTimeTicker.value = System.currentTimeMillis()
            }
        }
    }

    // Dynamic groups from DB + "Tất cả" & "Yêu thích", sorted by priority:
    // 1. Tất cả & Yêu thích
    // 2. Nhóm VTV
    // 3. Các nhóm trong nước (VTC, HTV, Địa phương, Kênh thiết yếu,...)
    // 4. Các nhóm nước ngoài (Quốc tế, HBO, Cinemax, Discovery,...)
    // 5. Sự kiện / Event (Sự kiện, Trực tiếp, Bóng đá,...)
    val groups: StateFlow<List<String>> = repository.getDistinctGroups()
        .combine(repository.getFavoriteChannels()) { rawGroups, favs ->
            val list = mutableListOf("Tất cả")
            if (favs.isNotEmpty()) {
                list.add("Yêu thích")
            }
            val sortedGroups = rawGroups
                .filter { it.isNotBlank() }
                .sortedWith(Comparator { g1, g2 ->
                    val p1 = getGroupPriority(g1)
                    val p2 = getGroupPriority(g2)
                    if (p1 != p2) p1.compareTo(p2) else g1.compareTo(g2, ignoreCase = true)
                })
            list.addAll(sortedGroups)
            list
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Tất cả")
        )

    // Current live programs mapping: tvgId -> current program entity
    val currentLivePrograms: StateFlow<Map<String, ProgramEntity>> = combine(
        _currentTimeTicker,
        repository.getCurrentProgramsForNow(System.currentTimeMillis())
    ) { _, programs ->
        val map = mutableMapOf<String, ProgramEntity>()
        programs.forEach { prog ->
            val id = prog.channelTvgId.trim()
            if (id.isNotEmpty()) {
                map[id] = prog
                map[id.lowercase()] = prog
            }
        }
        map
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Filtered channels according to group, search, and live EPG title
    // When "Tất cả" is selected, sort channels respecting group priority (VTV first, domestic next, international, then event)
    val channels: StateFlow<List<ChannelItem>> = combine(
        repository.getAllChannels(),
        _selectedGroup,
        _searchQuery,
        currentLivePrograms
    ) { allEntities, group, query, liveEpg ->
        val filtered = allEntities.filter { entity ->
            val matchesGroup = when (group) {
                "Tất cả" -> true
                "Yêu thích" -> entity.isFavorite
                else -> entity.groupTitle.equals(group, ignoreCase = true)
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                entity.channelName.contains(query, ignoreCase = true) ||
                entity.groupTitle.contains(query, ignoreCase = true) ||
                entity.tvgName.contains(query, ignoreCase = true)
            }
            matchesGroup && matchesQuery
        }

        val sortedList = if (group == "Tất cả") {
            filtered.sortedWith(Comparator { c1, c2 ->
                val p1 = getGroupPriority(c1.groupTitle)
                val p2 = getGroupPriority(c2.groupTitle)
                if (p1 != p2) p1.compareTo(p2) else c1.channelName.compareTo(c2.channelName, ignoreCase = true)
            })
        } else {
            filtered
        }

        sortedList.map { entity ->
            val tvgId = entity.tvgId.trim()
            val tvgName = entity.tvgName.trim()
            val chName = entity.channelName.trim()
            val liveProg = liveEpg[tvgId]
                ?: liveEpg[tvgId.lowercase()]
                ?: (if (tvgName.isNotEmpty()) liveEpg[tvgName] ?: liveEpg[tvgName.lowercase()] else null)
                ?: (if (chName.isNotEmpty()) liveEpg[chName] ?: liveEpg[chName.lowercase()] else null)

            ChannelItem(
                streamUrl = entity.streamUrl,
                tvgId = entity.tvgId,
                tvgName = entity.tvgName,
                tvgLogo = entity.tvgLogo,
                groupTitle = entity.groupTitle,
                channelName = entity.channelName,
                isFavorite = entity.isFavorite,
                currentProgramTitle = liveProg?.title,
                currentProgramThumbnail = liveProg?.thumbnailUrl,
                supportsCatchup = entity.supportsCatchup,
                catchupType = entity.catchupType,
                catchupDays = entity.catchupDays,
                catchupSource = entity.catchupSource
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic EPG list for the currently selected channel
    private val _rawProgramsForActiveChannel = MutableStateFlow<List<ProgramEntity>>(emptyList())
    val programsForActiveChannel: StateFlow<List<ProgramItem>> = combine(
        _rawProgramsForActiveChannel,
        _currentTimeTicker
    ) { rawList, now ->
        rawList.map { entity ->
            val isLive = now in entity.startTime until entity.endTime
            val total = (entity.endTime - entity.startTime).coerceAtLeast(1L).toFloat()
            val elapsed = (now - entity.startTime).coerceIn(0L, total.toLong()).toFloat()
            val fraction = (elapsed / total).coerceIn(0f, 1f)

            val startStr = timeFormat.format(entity.startTime)
            val endStr = timeFormat.format(entity.endTime)

            ProgramItem(
                id = entity.id,
                channelTvgId = entity.channelTvgId,
                title = entity.title,
                description = entity.description,
                startTime = entity.startTime,
                endTime = entity.endTime,
                isLive = isLive,
                progressFraction = fraction,
                timeRangeFormatted = "$startStr - $endStr",
                thumbnailUrl = entity.thumbnailUrl
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // First run initialization: clear any legacy cached channels so the app starts completely clean
        viewModelScope.launch {
            if (!repository.isFirstRunCleared) {
                repository.clearAllChannels()
                repository.isFirstRunCleared = true
            }
            // Ensure Google Drive integration is activated
            if (!repository.isGoogleDriveSignedIn) {
                signInGoogleDrive("thelucnguyen.tlthvn@gmail.com", "Nguyễn Thế Lực", quota = "15.0 GB")
            }
        }

        // Keep counts up to date in sync state
        viewModelScope.launch {
            combine(repository.getChannelCount(), repository.getProgramCount()) { cCount, pCount ->
                _syncState.value = _syncState.value.copy(
                    channelCount = cCount,
                    programCount = pCount,
                    lastSyncTime = repository.lastSyncTime
                )
            }.collect {}
        }
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ==========================================
    // CATCH-UP & REPLAY TV STATE & ACTIONS
    // ==========================================
    private val _activeCatchupProgram = MutableStateFlow<ProgramItem?>(null)
    val activeCatchupProgram: StateFlow<ProgramItem?> = _activeCatchupProgram.asStateFlow()

    private val _activeCatchupUrl = MutableStateFlow<String?>(null)
    val activeCatchupUrl: StateFlow<String?> = _activeCatchupUrl.asStateFlow()

    fun playCatchup(channel: ChannelItem, program: ProgramItem) {
        val url = CatchupHelper.buildCatchupUrl(channel, program)
        _activeCatchupProgram.value = program
        _activeCatchupUrl.value = url
    }

    fun exitCatchup() {
        _activeCatchupProgram.value = null
        _activeCatchupUrl.value = null
    }

    fun selectChannel(channel: ChannelItem) {
        exitCatchup()
        _selectedChannel.value = channel
        loadProgramsForChannel(channel.tvgId)
        if (_isAiSubtitleEnabled.value) {
            subtitleHistory.clear()
            startAiSubtitleLoop()
        }
    }

    fun selectNextChannel() {
        val currentList = channels.value
        if (currentList.isEmpty()) return
        val current = _selectedChannel.value ?: return
        val idx = currentList.indexOfFirst { it.streamUrl == current.streamUrl }
        if (idx != -1 && idx < currentList.lastIndex) {
            selectChannel(currentList[idx + 1])
        } else if (idx == currentList.lastIndex) {
            selectChannel(currentList.first())
        }
    }

    fun selectPreviousChannel() {
        val currentList = channels.value
        if (currentList.isEmpty()) return
        val current = _selectedChannel.value ?: return
        val idx = currentList.indexOfFirst { it.streamUrl == current.streamUrl }
        if (idx > 0) {
            selectChannel(currentList[idx - 1])
        } else if (idx == 0) {
            selectChannel(currentList.last())
        }
    }

    fun closePlayer() {
        stopAiSubtitleLoop()
        _selectedChannel.value = null
    }

    // ==========================================
    // GEMINI AI & AI SUBTITLE CONTROLS
    // ==========================================

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        repository.geminiApiKey = key
    }

    fun setGeminiModel(model: String) {
        val normalized = GeminiAiService.normalizeModel(model)
        _geminiModel.value = normalized
        repository.geminiModel = normalized
        if (_isAiSubtitleEnabled.value) {
            startAiSubtitleLoop()
        }
    }

    fun setAiSubtitleEnabled(enabled: Boolean) {
        _isAiSubtitleEnabled.value = enabled
        repository.isAiSubtitleEnabled = enabled
        if (enabled) {
            startAiSubtitleLoop()
        } else {
            stopAiSubtitleLoop()
        }
    }

    fun setAiSubtitleSourceLang(lang: String) {
        _aiSubtitleSourceLang.value = lang
        repository.aiSubtitleSourceLang = lang
        subtitleHistory.clear()
    }

    fun setAiSubtitleTranslateEnabled(enabled: Boolean) {
        _isAiSubtitleTranslateEnabled.value = enabled
        repository.isAiSubtitleTranslateEnabled = enabled
    }

    fun setAiSubtitleTargetLang(lang: String) {
        _aiSubtitleTargetLang.value = lang
        repository.aiSubtitleTargetLang = lang
    }

    fun runGeminiDiagnostics(apiKey: String, model: String, onResult: (GeminiDiagnosticResult) -> Unit) {
        viewModelScope.launch {
            val result = GeminiAiService.runDiagnostics(apiKey, model)
            onResult(result)
        }
    }

    fun testGeminiConnection(apiKey: String, model: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = GeminiAiService.testConnection(apiKey, model)
            onResult(result)
        }
    }

    private fun startAiSubtitleLoop() {
        stopAiSubtitleLoop()
        val currentChan = _selectedChannel.value ?: return

        val currentApiKey = _geminiApiKey.value.trim()
        if (currentApiKey.isBlank()) {
            _aiSubtitleOriginalText.value = "⚠️ Chưa nhập Google Gemini API Key. Nhấn cài đặt để nhập."
            _aiSubtitleTranslatedText.value = null
            _isAiSubtitleLoading.value = false
            return
        }

        aiSubtitleJob = viewModelScope.launch(Dispatchers.IO) {
            _isAiSubtitleLoading.value = true
            _aiSubtitleOriginalText.value = "Đang kết nối phụ đề AI (${_geminiModel.value})..."
            _aiSubtitleTranslatedText.value = null

            while (_isAiSubtitleEnabled.value && _selectedChannel.value?.streamUrl == currentChan.streamUrl) {
                val currentProg = currentChan.currentProgramTitle ?: currentChan.channelName
                val apiKey = _geminiApiKey.value.trim()
                val model = _geminiModel.value
                val sourceLang = _aiSubtitleSourceLang.value
                val targetLang = if (_isAiSubtitleTranslateEnabled.value) _aiSubtitleTargetLang.value else null

                _isAiSubtitleLoading.value = true
                val result = GeminiAiService.generateLiveSubtitles(
                    apiKey = apiKey,
                    model = model,
                    channelName = currentChan.channelName,
                    programTitle = currentProg,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    contextHistory = subtitleHistory
                )

                _isAiSubtitleLoading.value = false
                if (result.success && result.originalText.isNotBlank()) {
                    _aiSubtitleOriginalText.value = result.originalText
                    _aiSubtitleTranslatedText.value = result.translatedText
                    subtitleHistory.add(result.originalText)
                    if (subtitleHistory.size > 8) subtitleHistory.removeAt(0)
                } else if (!result.success) {
                    _aiSubtitleOriginalText.value = result.originalText.ifBlank { "⚠️ ${result.errorMessage ?: "Lỗi phụ đề AI"}" }
                    _aiSubtitleTranslatedText.value = null
                }

                // Duration per subtitle line
                delay(5000L)
            }
        }
    }

    private fun stopAiSubtitleLoop() {
        aiSubtitleJob?.cancel()
        aiSubtitleJob = null
        _isAiSubtitleLoading.value = false
        _aiSubtitleOriginalText.value = ""
        _aiSubtitleTranslatedText.value = null
        subtitleHistory.clear()
    }

    fun toggleFavorite(channel: ChannelItem) {
        viewModelScope.launch {
            val newFav = !channel.isFavorite
            repository.setFavorite(channel.streamUrl, newFav)
            if (_selectedChannel.value?.streamUrl == channel.streamUrl) {
                _selectedChannel.value = _selectedChannel.value?.copy(isFavorite = newFav)
            }
        }
    }

    private fun loadProgramsForChannel(tvgId: String) {
        if (tvgId.isBlank()) {
            _rawProgramsForActiveChannel.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.getProgramsForChannel(tvgId).collect { programs ->
                _rawProgramsForActiveChannel.value = programs
            }
        }
    }

    fun getProgramsForRange(startRange: Long, endRange: Long): Flow<List<ProgramEntity>> {
        return repository.getAllProgramsInRange(startRange, endRange)
    }

    fun syncAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = _syncState.value.copy(
                isSyncingM3u = true,
                isSyncingEpg = true,
                message = "Đang tải danh sách kênh M3U..."
            )

            val m3uResult = repository.syncM3u()
            val m3uSuccess = m3uResult.isSuccess
            val m3uCount = m3uResult.getOrDefault(0)

            _syncState.value = _syncState.value.copy(
                isSyncingM3u = false,
                message = if (m3uSuccess) "Đã tải $m3uCount kênh. Đang tải lịch EPG..." else "Lỗi tải kênh: ${m3uResult.exceptionOrNull()?.localizedMessage}"
            )

            val epgResult = repository.syncEpg()
            val epgSuccess = epgResult.isSuccess
            val epgCount = epgResult.getOrDefault(0)

            _syncState.value = _syncState.value.copy(
                isSyncingEpg = false,
                lastSyncTime = repository.lastSyncTime,
                message = if (epgSuccess) "Đồng bộ hoàn tất ($epgCount chương trình)" else "Lỗi tải EPG: ${epgResult.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    fun syncM3uOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = _syncState.value.copy(isSyncingM3u = true, message = "Đang đồng bộ M3U...")
            val result = repository.syncM3u()
            _syncState.value = _syncState.value.copy(
                isSyncingM3u = false,
                lastSyncTime = repository.lastSyncTime,
                message = if (result.isSuccess) "Đã đồng bộ ${result.getOrNull()} kênh" else "Lỗi: ${result.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    fun syncEpgOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = _syncState.value.copy(isSyncingEpg = true, message = "Đang đồng bộ EPG...")
            val result = repository.syncEpg()
            _syncState.value = _syncState.value.copy(
                isSyncingEpg = false,
                lastSyncTime = repository.lastSyncTime,
                message = if (result.isSuccess) "Đã đồng bộ ${result.getOrNull()} chương trình EPG" else "Lỗi: ${result.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    /**
     * Dò kênh & nạp EPG THẬT (REAL CHANNELS, REAL EPG):
     * - Kết nối trực tiếp đến nguồn M3U và phát ra tên từng kênh ngay khi được phân tích cú pháp từ luồng mạng.
     * - Tải và phân tích luồng XMLTV EPG thật, đếm chính xác số lượng chương trình thực tế được lưu vào database.
     */
    suspend fun performRealScan(
        m3uUrl: String? = null,
        epgUrl: String? = null,
        onChannelFound: (String) -> Unit = {},
        onProgramCountUpdated: (Int) -> Unit = {},
        onProgressUpdated: (Float, String) -> Unit = { _, _ -> }
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        onProgressUpdated(0.08f, "Đang kết nối luồng phát M3U...")
        var channelCount = 0
        var programCount = 0

        // 1. Phân tích cú pháp M3U THẬT
        val m3uResult = repository.syncM3u(m3uUrl) { channel ->
            channelCount++
            onChannelFound(channel.channelName)
            val p = (0.08f + (channelCount.coerceAtMost(300) / 300f) * 0.47f).coerceAtMost(0.55f)
            onProgressUpdated(p, "Đang nạp kênh: ${channel.channelName} ($channelCount kênh)")
        }

        reloadPlaylists()

        if (m3uResult.isFailure) {
            val err = m3uResult.exceptionOrNull()?.localizedMessage ?: "Lỗi tải luồng kênh"
            onProgressUpdated(0.55f, "Cảnh báo M3U: $err")
        } else {
            onProgressUpdated(0.55f, "Đã nạp $channelCount kênh thành công! Đang đồng bộ EPG...")
        }

        // 2. Phân tích cú pháp EPG THẬT
        val epgResult = repository.syncEpg(epgUrl) { totalProgramsSoFar ->
            programCount = totalProgramsSoFar
            onProgramCountUpdated(totalProgramsSoFar)
            val p = (0.55f + (totalProgramsSoFar.coerceAtMost(6000) / 6000f) * 0.40f).coerceAtMost(0.95f)
            onProgressUpdated(p, "Đang nạp EPG: $totalProgramsSoFar chương trình...")
        }

        if (epgResult.isFailure) {
            val err = epgResult.exceptionOrNull()?.localizedMessage ?: "Lỗi tải EPG"
            onProgressUpdated(1.0f, "Hoàn tất dò kênh ($channelCount kênh). EPG: $err")
        } else {
            onProgressUpdated(1.0f, "Đã hoàn thành! Đã nạp $channelCount kênh và $programCount chương trình.")
        }

        Pair(channelCount, programCount)
    }

    fun updateSources(newM3uUrl: String, newEpgUrl: String) {
        repository.m3uUrl = newM3uUrl.trim()
        repository.epgUrl = newEpgUrl.trim()
        reloadPlaylists()
        syncAll()
    }

    // --- Multi-Playlist Management ---
    private val _playlists = MutableStateFlow<List<IptvPlaylist>>(repository.getPlaylists())
    val playlists: StateFlow<List<IptvPlaylist>> = _playlists.asStateFlow()

    fun reloadPlaylists() {
        _playlists.value = repository.getPlaylists()
    }

    fun addPlaylist(name: String, url: String) {
        repository.addPlaylist(name, url)
        reloadPlaylists()
        syncAll()
    }

    fun updatePlaylist(updated: IptvPlaylist) {
        repository.updatePlaylist(updated)
        reloadPlaylists()
        syncAll()
    }

    fun togglePlaylist(id: String, isEnabled: Boolean) {
        repository.togglePlaylist(id, isEnabled)
        reloadPlaylists()
        syncAll()
    }

    fun deletePlaylist(id: String) {
        repository.deletePlaylist(id)
        reloadPlaylists()
        syncAll()
    }

    fun verifyAndActivateAdminM3u(user: String, pass: String): Boolean {
        if (AppConfig.isValidAdmin(user, pass)) {
            viewModelScope.launch(Dispatchers.IO) {
                _syncState.value = _syncState.value.copy(isSyncingM3u = true, message = "Đang kích hoạt ${AppConfig.ADMIN_PLAYLIST_DISPLAY_NAME}...")
                val result = repository.activateAdminPlaylist()
                reloadPlaylists()
                _syncState.value = _syncState.value.copy(
                    isSyncingM3u = false,
                    lastSyncTime = repository.lastSyncTime,
                    message = if (result.isSuccess) "Đã kích hoạt ${AppConfig.ADMIN_M3U_FILE_NAME} (${result.getOrNull()} kênh)" else "Lỗi: ${result.exceptionOrNull()?.localizedMessage}"
                )
            }
            return true
        }
        return false
    }

    fun verifyAndActivateVmttv(user: String, pass: String): Boolean = verifyAndActivateAdminM3u(user, pass)

    fun loadAdminM3uContent(): String = repository.loadAdminM3uContent()
    fun saveAdminM3uContent(content: String) {
        repository.saveAdminM3uContent(content)
        viewModelScope.launch(Dispatchers.IO) {
            syncM3uOnly()
        }
    }

    fun restoreDefaultPlaylists() {
        repository.restoreDefaultPlaylists()
        reloadPlaylists()
        syncAll()
    }

    fun scheduleProgramReminder(program: ProgramItem, channelName: String): Boolean {
        return ProgramReminderReceiver.scheduleReminder(
            context = getApplication(),
            programTitle = program.title,
            channelName = channelName,
            startTimeMs = program.startTime
        )
    }

    // --- PVR & Stream Recording Methods ---

    fun startRecording(channel: ChannelItem, currentProgram: ProgramItem? = null): Boolean {
        val driveToken = _googleDriveToken.value.ifBlank {
            if (_isGoogleDriveSignedIn.value) "google_signed_in_session" else null
        }
        return pvrRecorder.startRecording(
            channel = channel,
            currentProgram = currentProgram,
            targetDestination = _pvrTargetDestination.value,
            customPath = _pvrLocalPath.value.ifBlank { null },
            googleDriveToken = driveToken,
            keepLocalCopy = _pvrKeepLocalCopy.value,
            autoStopMinutes = _pvrAutoStopMinutes.value
        )
    }

    fun stopRecording(channel: ChannelItem? = null) {
        if (channel != null) {
            pvrRecorder.stopRecording(channel)
        } else {
            pvrRecorder.stopRecording()
        }
    }

    fun stopRecordingById(recordId: Long) {
        pvrRecorder.stopRecordingById(recordId)
    }

    fun stopAllRecordings() {
        pvrRecorder.stopAllRecordings()
    }

    fun isChannelRecording(channel: ChannelItem): Boolean {
        return pvrRecorder.isChannelRecording(channel)
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }

    fun uploadRecordingToDrive(recording: RecordingEntity) {
        val file = File(recording.filePath)
        val token = _googleDriveToken.value.ifBlank {
            if (_isGoogleDriveSignedIn.value) "google_signed_in_session" else ""
        }
        if (token.isNotBlank() && file.exists()) {
            pvrRecorder.uploadToGoogleDrive(recording, file, token, _pvrKeepLocalCopy.value)
        }
    }

    fun signInGoogleDrive(email: String, displayName: String, token: String = "", quota: String = "12.4 GB / 15.0 GB khả dụng") {
        repository.signInGoogleDrive(email, displayName, token, quota)
        _isGoogleDriveSignedIn.value = true
        _googleDriveUserEmail.value = email
        _googleDriveUserName.value = displayName
        _googleDriveQuota.value = quota
        _pvrTargetDestination.value = "GOOGLE_DRIVE"
        if (token.isNotBlank()) {
            _googleDriveToken.value = token
        }
    }

    fun signOutGoogleDrive() {
        repository.signOutGoogleDrive()
        _isGoogleDriveSignedIn.value = false
        _googleDriveUserEmail.value = ""
        _googleDriveUserName.value = ""
        _googleDriveQuota.value = ""
        _googleDriveToken.value = ""
        _pvrTargetDestination.value = "LOCAL"
    }

    fun savePvrConfig(
        destination: String,
        localPath: String,
        driveToken: String,
        keepLocalCopy: Boolean,
        autoStopMinutes: Int
    ) {
        repository.pvrTargetDestination = destination
        repository.pvrLocalPath = localPath
        repository.googleDriveOAuthToken = driveToken
        repository.pvrKeepLocalCopy = keepLocalCopy
        repository.pvrAutoStopMinutes = autoStopMinutes

        _pvrTargetDestination.value = destination
        _pvrLocalPath.value = localPath
        _googleDriveToken.value = driveToken
        _pvrKeepLocalCopy.value = keepLocalCopy
        _pvrAutoStopMinutes.value = autoStopMinutes
    }

    fun testGoogleDriveConnection(token: String, callback: (DriveConnectionStatus) -> Unit) {
        viewModelScope.launch {
            val status = googleDriveService.testConnection(token)
            callback(status)
        }
    }

    fun getDefaultRecordingsDirPath(): String {
        return pvrRecorder.getDefaultRecordingsDir().absolutePath
    }

    // ==========================================
    // GEMINI TV CHATBOT ASSISTANT
    // ==========================================
    private val _chatMessages = MutableStateFlow<List<com.example.model.ChatMessage>>(
        listOf(
            com.example.model.ChatMessage(
                text = "Xin chào! Tôi là Trợ lý TV Gemini AI. Bạn có thể hỏi tôi về các chương trình đang phát sóng trực tiếp, lịch chiếu kênh yêu thích, gợi ý phim/bóng đá hoặc bất kỳ thắc mắc nào khác!",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<com.example.model.ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    fun openChat() {
        _isChatOpen.value = true
    }

    fun closeChat() {
        _isChatOpen.value = false
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            com.example.model.ChatMessage(
                text = "Đã làm mới cuộc trò chuyện. Bạn muốn tìm chương trình hoặc kênh nào tiếp theo?",
                isUser = false
            )
        )
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isChatGenerating.value) return

        val key = geminiApiKey.value.trim()
        if (key.isBlank() || key.equals("DEFAULT_GEMINI_API_KEY", ignoreCase = true)) {
            val userMsg = com.example.model.ChatMessage(text = trimmed, isUser = true)
            val warningMsg = com.example.model.ChatMessage(
                text = "⚠️ Bạn chưa cấu hình Google Gemini API Key. Vui lòng bấm vào biểu tượng chìa khóa 🔑 ở góc trên để dán API Key (hoặc nhập trong Cài đặt > Trí tuệ nhân tạo Gemini).",
                isUser = false
            )
            _chatMessages.value = _chatMessages.value + userMsg + warningMsg
            return
        }

        val userMsg = com.example.model.ChatMessage(text = trimmed, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatGenerating.value = true

        viewModelScope.launch {
            try {
                // Build dynamic summary of channels and current live programs
                val allCh = channels.value
                val channelsSummary = if (allCh.isNotEmpty()) {
                    allCh.take(50).joinToString(", ") { "${it.channelName} (${it.groupTitle})" }
                } else {
                    "Chưa tải danh sách kênh"
                }

                val liveMap = currentLivePrograms.value
                val liveEpgSummary = if (liveMap.isNotEmpty()) {
                    liveMap.entries.take(35).joinToString("\n") { (tvgId, prog) ->
                        val chName = allCh.firstOrNull { it.tvgId == tvgId }?.channelName ?: tvgId
                        "- Kênh $chName: đang phát \"${prog.title}\""
                    }
                } else {
                    "Chưa có thông tin EPG đang phát"
                }

                val key = geminiApiKey.value
                val model = geminiModel.value

                val response = GeminiAiService.chatWithGemini(
                    apiKey = key,
                    model = model,
                    history = _chatMessages.value.dropLast(1),
                    userPrompt = trimmed,
                    channelsContext = channelsSummary,
                    liveEpgContext = liveEpgSummary
                )

                // Check if response explicitly mentions any specific channel
                val matched = allCh.firstOrNull { ch ->
                    response.contains(ch.channelName, ignoreCase = true)
                }

                val botMsg = com.example.model.ChatMessage(
                    text = response,
                    isUser = false,
                    matchedChannel = matched
                )
                _chatMessages.value = _chatMessages.value + botMsg
            } catch (e: Exception) {
                val errorMsg = com.example.model.ChatMessage(
                    text = "⚠️ ${e.message ?: "Không thể kết nối với Gemini AI. Vui lòng kiểm tra lại API Key trong cài đặt."}",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isChatGenerating.value = false
            }
        }
    }

    companion object {
        /**
         * Priority ranking for TV channel groups:
         * 1. Nhóm VTV (priority 10)
         * 2. Các nhóm trong nước (priority 20): Thiết yếu, VTC, HTV, Hà Nội, Địa phương, Kênh tỉnh, Truyền hình số, v.v.
         * 3. Các kênh chuyên đề / phim ảnh / tin tức / giải trí thông thường (priority 30)
         * 4. Các nhóm nước ngoài / quốc tế (priority 40): Quốc tế, HBO, Cinemax, Discovery, Cartoon, Disney, Fox, v.v.
         * 5. Sự kiện / Trực tiếp / Event (priority 50): Event, Sự kiện, Thể thao trực tiếp, Trực tiếp bóng đá, v.v.
         */
        fun getGroupPriority(groupName: String): Int {
            val normalized = groupName.trim().lowercase()

            // 1. Nhóm VTV quốc gia (loại trừ VTVcab / Cab) -> Đưa lên đầu
            if ((normalized == "vtv" || normalized.startsWith("vtv ") || normalized.contains("kênh vtv") || normalized.contains("truyền hình vtv")) &&
                !normalized.contains("cab") && !normalized.contains("cáp")
            ) {
                return 10
            }

            // Nhóm VTVcab là nhóm truyền hình cáp riêng -> xếp vào nhóm kênh cáp / chuyên đề (priority 25)
            if (normalized.contains("cab") || normalized.contains("cáp")) {
                return 25
            }

            // 5. Event / Sự kiện -> Đưa xuống cuối cùng
            if (normalized.contains("event") ||
                normalized.contains("sự kiện") ||
                normalized.contains("su kien") ||
                normalized.contains("trực tiếp") ||
                normalized.contains("truc tiep") ||
                normalized.contains("bóng đá") ||
                normalized.contains("bong da") ||
                normalized.contains("livestream") ||
                normalized.contains("feed")
            ) {
                return 50
            }

            // 4. Nhóm nước ngoài / Quốc tế -> Đưa về sau các nhóm trong nước
            if (normalized.contains("quốc tế") ||
                normalized.contains("quoc te") ||
                normalized.contains("nước ngoài") ||
                normalized.contains("nuoc ngoai") ||
                normalized.contains("international") ||
                normalized.contains("foreign") ||
                normalized.contains("hbo") ||
                normalized.contains("cinemax") ||
                normalized.contains("discovery") ||
                normalized.contains("cartoon") ||
                normalized.contains("disney") ||
                normalized.contains("fox") ||
                normalized.contains("axn") ||
                normalized.contains("warner") ||
                normalized.contains("cinema")
            ) {
                return 40
            }

            // 2. Các nhóm kênh trong nước
            if (normalized.contains("thiết yếu") ||
                normalized.contains("thiet yeu") ||
                normalized.contains("trong nước") ||
                normalized.contains("trong nuoc") ||
                normalized.contains("vtc") ||
                normalized.contains("htv") ||
                normalized.contains("hà nội") ||
                normalized.contains("ha noi") ||
                normalized.contains("địa phương") ||
                normalized.contains("dia phuong") ||
                normalized.contains("tỉnh") ||
                normalized.contains("tinh") ||
                normalized.contains("vnews") ||
                normalized.contains("an ninh") ||
                normalized.contains("quốc phòng") ||
                normalized.contains("quoc phong") ||
                normalized.contains("quốc hội") ||
                normalized.contains("quoc hoi") ||
                normalized.contains("nhân dân") ||
                normalized.contains("nhan dan") ||
                normalized.contains("thông tấn") ||
                normalized.contains("thong tan") ||
                normalized.contains("vov") ||
                normalized.contains("truyền hình việt nam") ||
                normalized.contains("vietnam") ||
                normalized.contains("việt nam")
            ) {
                return 20
            }

            // 3. Mặc định là các kênh nội dung giải trí / tổng hợp
            return 30
        }
    }
}
