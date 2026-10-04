package com.example.data.remote.drive

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val isSuccess: Boolean,
    val fileId: String? = null,
    val webViewLink: String? = null,
    val errorMessage: String? = null
)

data class DriveConnectionStatus(
    val isConnected: Boolean,
    val userEmail: String? = null,
    val displayName: String? = null,
    val totalQuotaBytes: Long = 0L,
    val usedQuotaBytes: Long = 0L,
    val errorMessage: String? = null
) {
    val storageQuotaFormatted: String
        get() {
            if (totalQuotaBytes <= 0) return ""
            val usedGb = usedQuotaBytes.toDouble() / (1024 * 1024 * 1024)
            val totalGb = totalQuotaBytes.toDouble() / (1024 * 1024 * 1024)
            return "%.1f GB / %.1f GB".format(usedGb, totalGb)
        }
}

class GoogleDriveService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val FOLDER_NAME = "VMA_TV_Recordings"
    }

    /**
     * Test connection to Google Drive using the provided OAuth Access Token
     */
    suspend fun testConnection(accessToken: String): DriveConnectionStatus = withContext(Dispatchers.IO) {
        val trimmedToken = accessToken.trim()
        if (trimmedToken.isBlank()) {
            return@withContext DriveConnectionStatus(
                isConnected = false,
                errorMessage = "Chưa cung cấp Google OAuth Access Token"
            )
        }

        try {
            val request = Request.Builder()
                .url("$DRIVE_API_BASE/about?fields=user,storageQuota")
                .header("Authorization", "Bearer $trimmedToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val userObj = json.optJSONObject("user")
                    val quotaObj = json.optJSONObject("storageQuota")

                    val email = userObj?.optString("emailAddress", "")
                    val name = userObj?.optString("displayName", "")
                    val limit = quotaObj?.optLong("limit", 0L) ?: 0L
                    val usage = quotaObj?.optLong("usage", 0L) ?: 0L

                    DriveConnectionStatus(
                        isConnected = true,
                        userEmail = email,
                        displayName = name,
                        totalQuotaBytes = limit,
                        usedQuotaBytes = usage
                    )
                } else {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyString).optJSONObject("error")
                        errObj?.optString("message") ?: "Mã lỗi HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Mã phản hồi HTTP: ${response.code}"
                    }
                    DriveConnectionStatus(
                        isConnected = false,
                        errorMessage = "Lỗi kết nối Google Drive: $errorMsg"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Drive connection", e)
            DriveConnectionStatus(
                isConnected = false,
                errorMessage = "Không thể kết nối Google Drive: ${e.localizedMessage ?: "Lỗi mạng"}"
            )
        }
    }

    /**
     * Finds or creates the target folder in user's Google Drive root
     */
    suspend fun getOrCreateRecordingsFolder(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken.trim()
        try {
            // Search for existing folder
            val query = "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val searchRequest = Request.Builder()
                .url("$DRIVE_API_BASE/files?q=${query.replace(" ", "%20").replace("'", "%27")}&fields=files(id,name)")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val filesArray = JSONObject(body).optJSONArray("files") ?: JSONArray()
                    if (filesArray.length() > 0) {
                        val folderId = filesArray.getJSONObject(0).getString("id")
                        return@withContext Result.success(folderId)
                    }
                }
            }

            // Create folder if not found
            val metadataJson = JSONObject().apply {
                put("name", FOLDER_NAME)
                put("mimeType", "application/vnd.google-apps.folder")
            }

            val createRequest = Request.Builder()
                .url("$DRIVE_API_BASE/files?fields=id")
                .header("Authorization", "Bearer $token")
                .post(metadataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(createRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val id = JSONObject(body).getString("id")
                    Result.success(id)
                } else {
                    Result.failure(Exception("Không thể tạo thư mục trên Drive (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads the recorded stream file to Google Drive
     */
    suspend fun uploadRecording(
        file: File,
        accessToken: String,
        onProgress: (percent: Int) -> Unit = {}
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val token = accessToken.trim()
        if (!file.exists() || file.length() == 0L) {
            return@withContext DriveUploadResult(
                isSuccess = false,
                errorMessage = "File bản ghi không tồn tại hoặc rỗng (0 bytes)"
            )
        }

        if (token.isBlank()) {
            return@withContext DriveUploadResult(
                isSuccess = false,
                errorMessage = "Chưa kết nối Google Drive hoặc thiếu Access Token"
            )
        }

        try {
            // Get or create parent folder
            val folderId = getOrCreateRecordingsFolder(token).getOrNull()

            // Prepare metadata JSON
            val metadata = JSONObject().apply {
                put("name", file.name)
                put("mimeType", "video/mp2t")
                if (!folderId.isNullOrBlank()) {
                    put("parents", JSONArray().put(folderId))
                }
            }

            // Multipart body with metadata and file content with progress tracking
            val fileRequestBody = file.asRequestBody("video/mp2t".toMediaType())
            val progressRequestBody = CountingRequestBody(fileRequestBody) { bytesWritten, totalBytes ->
                if (totalBytes > 0) {
                    val percent = ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
                    onProgress(percent)
                }
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart("file", file.name, progressRequestBody)
                .build()

            val uploadRequest = Request.Builder()
                .url("$UPLOAD_API_BASE/files?uploadType=multipart&fields=id,name,webViewLink")
                .header("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            client.newCall(uploadRequest).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val fileId = json.optString("id")
                    val webViewLink = json.optString("webViewLink", "https://drive.google.com/file/d/$fileId/view")

                    onProgress(100)
                    DriveUploadResult(
                        isSuccess = true,
                        fileId = fileId,
                        webViewLink = webViewLink
                    )
                } else {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyString).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    DriveUploadResult(
                        isSuccess = false,
                        errorMessage = "Tải lên Drive thất bại: $errorMsg"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Google Drive", e)
            DriveUploadResult(
                isSuccess = false,
                errorMessage = "Lỗi khi tải lên Google Drive: ${e.localizedMessage ?: "Lỗi I/O"}"
            )
        }
    }
}

/**
 * RequestBody wrapper to report byte upload progress
 */
private class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType() = delegate.contentType()

    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var bytesWritten = 0L

        val countingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, totalBytes)
            }
        }

        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
