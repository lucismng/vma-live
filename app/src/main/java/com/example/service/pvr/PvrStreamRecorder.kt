package com.example.service.pvr

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.dao.RecordingDao
import com.example.data.local.entity.RecordingEntity
import com.example.data.remote.drive.GoogleDriveService
import com.example.model.ChannelItem
import com.example.model.ProgramItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Immutable representation of an active recording for UI consumption
 */
data class ActiveRecordingInfo(
    val recordId: Long,
    val channelId: String,
    val channel: ChannelItem,
    val programTitle: String,
    val durationSeconds: Long,
    val recordedBytes: Long,
    val filePath: String,
    val fileName: String,
    val targetDestination: String,
    val uploadProgress: Int? = null
)

/**
 * Multi-channel concurrent PVR stream recorder.
 * Supports recording multiple TV channels simultaneously in background.
 */
class PvrStreamRecorder(
    private val context: Context,
    private val recordingDao: RecordingDao,
    private val googleDriveService: GoogleDriveService,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PvrStreamRecorder"
        private const val BUFFER_SIZE = 64 * 1024 // 64KB buffer for smooth raw stream capture
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private class InternalSession(
        val recordId: Long,
        val channel: ChannelItem,
        val program: ProgramItem?,
        val outputFile: File,
        val fileName: String,
        val startTimeMs: Long,
        val targetDestination: String,
        val googleDriveToken: String?,
        val keepLocalCopy: Boolean,
        val autoStopMinutes: Int,
        @Volatile var durationSeconds: Long = 0L,
        @Volatile var recordedBytes: Long = 0L,
        @Volatile var uploadProgress: Int? = null,
        var recordingJob: Job? = null,
        var timerJob: Job? = null,
        @Volatile var activeCall: Call? = null,
        @Volatile var activeInputStream: InputStream? = null,
        @Volatile var activeOutputStream: OutputStream? = null,
        @Volatile var isStopping: Boolean = false
    )

    private val sessions = ConcurrentHashMap<String, InternalSession>()

    private val _activeRecordings = MutableStateFlow<List<ActiveRecordingInfo>>(emptyList())
    val activeRecordings: StateFlow<List<ActiveRecordingInfo>> = _activeRecordings.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingChannel = MutableStateFlow<ChannelItem?>(null)
    val recordingChannel: StateFlow<ChannelItem?> = _recordingChannel.asStateFlow()

    private val _recordedDurationSeconds = MutableStateFlow(0L)
    val recordedDurationSeconds: StateFlow<Long> = _recordedDurationSeconds.asStateFlow()

    private val _recordedBytes = MutableStateFlow(0L)
    val recordedBytes: StateFlow<Long> = _recordedBytes.asStateFlow()

    private val _activeRecording = MutableStateFlow<RecordingEntity?>(null)
    val activeRecording: StateFlow<RecordingEntity?> = _activeRecording.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private fun getSessionKey(channel: ChannelItem): String {
        return if (channel.streamUrl.isNotBlank()) channel.streamUrl else channel.tvgId
    }

    private fun syncUiState() {
        val list = sessions.values.map { s ->
            ActiveRecordingInfo(
                recordId = s.recordId,
                channelId = s.channel.tvgId.ifBlank { s.channel.streamUrl },
                channel = s.channel,
                programTitle = s.program?.title ?: "Luồng trực tiếp",
                durationSeconds = s.durationSeconds,
                recordedBytes = s.recordedBytes,
                filePath = s.outputFile.absolutePath,
                fileName = s.fileName,
                targetDestination = s.targetDestination,
                uploadProgress = s.uploadProgress
            )
        }
        _activeRecordings.value = list
        _isRecording.value = list.isNotEmpty()

        val primary = sessions.values.firstOrNull()
        _recordingChannel.value = primary?.channel
        _recordedDurationSeconds.value = primary?.durationSeconds ?: 0L
        _recordedBytes.value = primary?.recordedBytes ?: 0L
        _uploadProgress.value = primary?.uploadProgress
    }

    /**
     * Resolve default recordings directory
     */
    fun getDefaultRecordingsDir(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "recordings")
        val pvrDir = File(moviesDir, "VMA_Recordings")
        if (!pvrDir.exists()) {
            pvrDir.mkdirs()
        }
        return pvrDir
    }

    /**
     * Check if a specific channel is currently being recorded
     */
    fun isChannelRecording(channel: ChannelItem): Boolean {
        return sessions.containsKey(getSessionKey(channel))
    }

    fun isChannelRecording(streamUrl: String): Boolean {
        return sessions.containsKey(streamUrl) || sessions.values.any { it.channel.streamUrl == streamUrl }
    }

    /**
     * Start recording a channel concurrently.
     * Can be invoked multiple times for different channels without canceling others!
     */
    fun startRecording(
        channel: ChannelItem,
        currentProgram: ProgramItem? = null,
        targetDestination: String = "LOCAL",
        customPath: String? = null,
        googleDriveToken: String? = null,
        keepLocalCopy: Boolean = true,
        autoStopMinutes: Int = 0
    ): Boolean {
        val key = getSessionKey(channel)
        if (sessions.containsKey(key)) {
            Log.w(TAG, "Channel already being recorded: ${channel.channelName}")
            return false
        }

        val targetDir = if (!customPath.isNullOrBlank()) {
            val resolvedPath = com.example.util.FileManagerHelper.resolveStoragePath(context, customPath)
            try {
                val dir = File(resolvedPath)
                if (!dir.exists()) dir.mkdirs()
                if (dir.canWrite()) dir else getDefaultRecordingsDir()
            } catch (_: Exception) {
                getDefaultRecordingsDir()
            }
        } else {
            getDefaultRecordingsDir()
        }

        // Cú pháp chuẩn: <tên kênh>_<năm ngày tháng> (Ví dụ: VTV1_2026_04_10.mp4)
        val safeChannelName = channel.channelName.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val dateYearDayMonth = SimpleDateFormat("yyyy_dd_MM", Locale.getDefault()).format(Date())
        var baseFileName = "${safeChannelName}_$dateYearDayMonth"
        var candidateFile = File(targetDir, "$baseFileName.mp4")
        if (candidateFile.exists()) {
            val timeSuffix = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            baseFileName = "${safeChannelName}_${dateYearDayMonth}_$timeSuffix"
            candidateFile = File(targetDir, "$baseFileName.mp4")
        }
        val fileName = "$baseFileName.mp4"
        val outputFile = candidateFile
        val startTimeMs = System.currentTimeMillis()

        scope.launch(Dispatchers.IO) {
            var currentEntity = RecordingEntity(
                channelId = channel.tvgId.ifBlank { channel.streamUrl },
                channelName = channel.channelName,
                channelLogo = channel.tvgLogo,
                programTitle = currentProgram?.title ?: "Luồng trực tiếp",
                filePath = outputFile.absolutePath,
                fileName = fileName,
                fileSizeBytes = 0L,
                durationSeconds = 0L,
                startTimeMs = startTimeMs,
                targetDestination = targetDestination,
                status = "RECORDING"
            )

            val recordId = recordingDao.insertRecording(currentEntity)
            currentEntity = currentEntity.copy(id = recordId)

            val session = InternalSession(
                recordId = recordId,
                channel = channel,
                program = currentProgram,
                outputFile = outputFile,
                fileName = fileName,
                startTimeMs = startTimeMs,
                targetDestination = targetDestination,
                googleDriveToken = googleDriveToken,
                keepLocalCopy = keepLocalCopy,
                autoStopMinutes = autoStopMinutes
            )

            sessions[key] = session
            syncUiState()

            // Timer ticker per session
            session.timerJob = scope.launch {
                while (isActive && !session.isStopping && sessions.containsKey(key)) {
                    delay(1000)
                    session.durationSeconds += 1
                    syncUiState()
                    if (session.autoStopMinutes > 0 && session.durationSeconds >= session.autoStopMinutes * 60L) {
                        stopRecordingInternal(session, key)
                        break
                    }
                }
            }

            // Stream recording worker job
            session.recordingJob = scope.launch(Dispatchers.IO) {
                var totalBytes = 0L
                var recordSuccess = false

                try {
                    totalBytes = recordRawStream(session, channel.streamUrl, outputFile)
                    recordSuccess = outputFile.exists() && outputFile.length() > 0
                } catch (e: CancellationException) {
                    recordSuccess = outputFile.exists() && outputFile.length() > 0
                } catch (e: Exception) {
                    if (!session.isStopping) {
                        Log.e(TAG, "Error recording channel: ${channel.channelName}", e)
                    }
                    recordSuccess = outputFile.exists() && outputFile.length() > 0
                } finally {
                    withContext(NonCancellable) {
                        val endTimeMs = System.currentTimeMillis()
                        val durationSec = ((endTimeMs - session.startTimeMs) / 1000).coerceAtLeast(1)
                        val finalSizeBytes = if (outputFile.exists()) outputFile.length() else totalBytes
                        val finalSuccess = (outputFile.exists() && outputFile.length() > 0) || totalBytes > 0

                        val finalEntity = currentEntity.copy(
                            endTimeMs = endTimeMs,
                            durationSeconds = durationSec,
                            fileSizeBytes = finalSizeBytes,
                            status = if (finalSuccess) "COMPLETED" else "FAILED"
                        )

                        try {
                            recordingDao.updateRecording(finalEntity)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update recording DAO: ${e.message}")
                        }

                        sessions.remove(key)
                        session.timerJob?.cancel()
                        syncUiState()

                        if (finalSuccess && targetDestination == "GOOGLE_DRIVE" && !googleDriveToken.isNullOrBlank()) {
                            uploadToGoogleDrive(finalEntity, outputFile, googleDriveToken, keepLocalCopy)
                        }
                    }
                }
            }
        }

        return true
    }

    private fun stopRecordingInternal(session: InternalSession, key: String) {
        if (session.isStopping) return
        session.isStopping = true
        session.timerJob?.cancel()

        try {
            session.activeCall?.cancel()
        } catch (_: Exception) {}

        try {
            session.activeInputStream?.close()
        } catch (_: Exception) {}

        try {
            session.activeOutputStream?.flush()
            session.activeOutputStream?.close()
        } catch (_: Exception) {}

        session.recordingJob?.cancel()
    }

    /**
     * Stop recording for a specific channel
     */
    fun stopRecording(channel: ChannelItem) {
        val key = getSessionKey(channel)
        sessions[key]?.let { session ->
            stopRecordingInternal(session, key)
        }
    }

    /**
     * Stop legacy single recording or stop the first active channel
     */
    fun stopRecording() {
        val firstEntry = sessions.entries.firstOrNull() ?: return
        stopRecordingInternal(firstEntry.value, firstEntry.key)
    }

    /**
     * Stop recording by recording record ID
     */
    fun stopRecordingById(recordId: Long) {
        val entry = sessions.entries.firstOrNull { it.value.recordId == recordId } ?: return
        stopRecordingInternal(entry.value, entry.key)
    }

    /**
     * Stop all currently active recordings simultaneously
     */
    fun stopAllRecordings() {
        val currentSessions = sessions.toMap()
        for ((key, session) in currentSessions) {
            stopRecordingInternal(session, key)
        }
    }

    /**
     * Records raw stream into output file for a specific session
     */
    private suspend fun recordRawStream(
        session: InternalSession,
        streamUrl: String,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        val isM3u8 = streamUrl.contains(".m3u8", ignoreCase = true)
        if (isM3u8) {
            recordHlsStream(session, streamUrl, outputFile)
        } else {
            recordDirectStream(session, streamUrl, outputFile)
        }
    }

    private suspend fun recordDirectStream(
        session: InternalSession,
        streamUrl: String,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        var bytesWritten = 0L
        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", "VMA_PVR_Recorder/2.0")
            .build()

        val call = httpClient.newCall(request)
        session.activeCall = call

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP stream request failed: ${response.code}")
                }
                val body = response.body ?: throw Exception("Empty stream body")
                val inputStream: InputStream = body.byteStream()
                session.activeInputStream = inputStream

                val out = FileOutputStream(outputFile, true)
                session.activeOutputStream = out

                out.use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var readBytes: Int

                    while (!session.isStopping && coroutineContext.isActive) {
                        readBytes = inputStream.read(buffer)
                        if (readBytes == -1) break
                        outputStream.write(buffer, 0, readBytes)
                        bytesWritten += readBytes
                        session.recordedBytes = bytesWritten
                    }
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            if (!session.isStopping && e !is CancellationException) {
                Log.w(TAG, "Direct stream recording ended for ${session.channel.channelName}: ${e.message}")
            }
        } finally {
            session.activeCall = null
            session.activeInputStream = null
            session.activeOutputStream = null
        }
        bytesWritten
    }

    private suspend fun recordHlsStream(
        session: InternalSession,
        playlistUrl: String,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val downloadedSegments = mutableSetOf<String>()
        val baseUrl = playlistUrl.substringBeforeLast("/") + "/"

        val out = FileOutputStream(outputFile, true)
        session.activeOutputStream = out

        try {
            out.use { fileOut ->
                while (!session.isStopping && coroutineContext.isActive) {
                    try {
                        val playlistRequest = Request.Builder()
                            .url(playlistUrl)
                            .header("User-Agent", "VMA_PVR_Recorder/2.0")
                            .build()

                        val call = httpClient.newCall(playlistRequest)
                        session.activeCall = call
                        val playlistContent = call.execute().use { res ->
                            if (res.isSuccessful) res.body?.string().orEmpty() else ""
                        }
                        session.activeCall = null

                        val lines = playlistContent.lines()
                        val segmentsToDownload = mutableListOf<String>()

                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                                val segmentUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                    trimmed
                                } else {
                                    baseUrl + trimmed
                                }
                                if (!downloadedSegments.contains(segmentUrl)) {
                                    segmentsToDownload.add(segmentUrl)
                                }
                            }
                        }

                        for (segUrl in segmentsToDownload) {
                            if (session.isStopping || !coroutineContext.isActive) break

                            try {
                                val segRequest = Request.Builder().url(segUrl).build()
                                val segCall = httpClient.newCall(segRequest)
                                session.activeCall = segCall

                                segCall.execute().use { segRes ->
                                    if (segRes.isSuccessful) {
                                        val segInput = segRes.body?.byteStream()
                                        session.activeInputStream = segInput
                                        segInput?.use { input ->
                                            val buffer = ByteArray(BUFFER_SIZE)
                                            var bytesRead: Int
                                            while (!session.isStopping && coroutineContext.isActive) {
                                                bytesRead = input.read(buffer)
                                                if (bytesRead == -1) break
                                                fileOut.write(buffer, 0, bytesRead)
                                                totalBytes += bytesRead
                                                session.recordedBytes = totalBytes
                                            }
                                            fileOut.flush()
                                        }
                                        session.activeInputStream = null
                                        downloadedSegments.add(segUrl)
                                    }
                                }
                                session.activeCall = null
                            } catch (e: Exception) {
                                if (!session.isStopping && e !is CancellationException) {
                                    Log.w(TAG, "Failed downloading segment: $segUrl", e)
                                }
                            }
                        }

                        if (downloadedSegments.size > 200) {
                            val keep = downloadedSegments.toList().takeLast(100).toSet()
                            downloadedSegments.clear()
                            downloadedSegments.addAll(keep)
                        }

                        if (!session.isStopping && coroutineContext.isActive) {
                            delay(2000)
                        }
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        if (!session.isStopping) {
                            delay(3000)
                        } else {
                            break
                        }
                    }
                }
            }
        } finally {
            session.activeCall = null
            session.activeInputStream = null
            session.activeOutputStream = null
        }
        totalBytes
    }

    /**
     * Upload finished recording to Google Drive
     */
    fun uploadToGoogleDrive(
        recording: RecordingEntity,
        file: File,
        googleDriveToken: String,
        keepLocalCopy: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                recordingDao.updateRecording(recording.copy(status = "UPLOADING"))
                _uploadProgress.value = 0

                val uploadResult = googleDriveService.uploadRecording(file, googleDriveToken) { percent ->
                    _uploadProgress.value = percent
                }

                if (uploadResult.isSuccess) {
                    val updated = recording.copy(
                        status = "COMPLETED",
                        isUploadedToDrive = true,
                        driveFileId = uploadResult.fileId,
                        driveWebViewLink = uploadResult.webViewLink
                    )
                    recordingDao.updateRecording(updated)

                    if (!keepLocalCopy && file.exists()) {
                        file.delete()
                    }
                } else {
                    recordingDao.updateRecording(recording.copy(status = "FAILED"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed Drive upload", e)
                recordingDao.updateRecording(recording.copy(status = "FAILED"))
            } finally {
                _uploadProgress.value = null
            }
        }
    }
}
