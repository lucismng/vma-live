package com.example.data.remote.gemini

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Structured diagnostic result containing comprehensive connection telemetry
 * and user-friendly troubleshooting details.
 */
data class GeminiDiagnosticResult(
    val isSuccess: Boolean,
    val testedModel: String,
    val normalizedModel: String,
    val latencyMs: Long = 0L,
    val httpStatusCode: Int? = null,
    val statusSummary: String,
    val errorDetails: String? = null,
    val suggestedActions: List<String> = emptyList(),
    val rawResponseSnippet: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Diagnostic utility that verifies Gemini API connectivity, validates credentials,
 * evaluates latency, analyzes HTTP responses, and generates actionable troubleshooting guidance.
 */
object GeminiDiagnosticUtility {
    private const val TAG = "GeminiDiagnosticUtility"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Dedicated OkHttpClient with balanced timeout for rapid diagnostic feedback
    private val diagnosticClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Deprecated models prohibited by system guidelines
    private val DEPRECATED_MODELS = setOf(
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-pro",
        "gemini-2.0-flash",
        "gemini-2.0-pro",
        "gemini-2.0-flash-thinking"
    )

    /**
     * Executes comprehensive diagnostic tests for the provided API key and model name.
     *
     * @param apiKey User-provided Google AI Studio / Gemini API key
     * @param rawModel Model name or alias entered by user (e.g., "gemini-3.5-flash-lite", "3.5 flash")
     * @return [GeminiDiagnosticResult] with detailed outcome and troubleshooting recommendations
     */
    suspend fun runDiagnostics(apiKey: String, rawModel: String): GeminiDiagnosticResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val normalizedModel = GeminiAiService.normalizeModel(rawModel)

        // 1. Check: Empty API key
        if (cleanKey.isBlank() || cleanKey.equals("DEFAULT_GEMINI_API_KEY", ignoreCase = true)) {
            return@withContext GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel.ifBlank { "Mặc định" },
                normalizedModel = normalizedModel,
                statusSummary = "Thiếu API Key",
                errorDetails = "API Key đang để trống. Ứng dụng không thể gửi yêu cầu xác thực tới máy chủ Google.",
                suggestedActions = listOf(
                    "Truy cập https://aistudio.google.com/ để tạo API Key miễn phí.",
                    "Dán khóa API (thường bắt đầu bằng 'AIzaSy...') vào ô trên.",
                    "Nhấn 'Lưu cấu hình' sau khi dán khóa."
                )
            )
        }

        // 2. Check: API key format sanity
        if (cleanKey.contains(" ") || cleanKey.length < 20) {
            return@withContext GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                statusSummary = "Định dạng API Key không hợp lệ",
                errorDetails = "Khóa chứa khoảng trắng hoặc có độ dài quá ngắn (${cleanKey.length} ký tự). Khóa chuẩn của Google thường có độ dài khoảng 39 ký tự.",
                suggestedActions = listOf(
                    "Kiểm tra lại xem có khoảng trắng hoặc ký tự thừa khi sao chép không.",
                    "Đảm bảo sao chép toàn bộ chuỗi ký tự từ Google AI Studio."
                )
            )
        }

        // 3. Check: Deprecated / Prohibited model warning
        if (DEPRECATED_MODELS.contains(normalizedModel.lowercase()) ||
            DEPRECATED_MODELS.contains(rawModel.lowercase().trim())) {
            return@withContext GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                statusSummary = "Mô hình đã ngừng hoạt động (Deprecated)",
                errorDetails = "Mô hình '$rawModel' đã bị Google hoặc hệ thống ngừng hỗ trợ theo chính sách API mới nhất.",
                suggestedActions = listOf(
                    "Chuyển sang 'gemini-3.5-flash' (nhanh nhất & khuyên dùng).",
                    "Hoặc chọn 'gemini-3.1-flash-lite-preview' hoặc 'gemini-2.5-flash'."
                )
            )
        }

        // 4. Perform Live Probe Request
        val probeUrl = "$BASE_URL$normalizedModel:generateContent?key=$cleanKey"
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val userContent = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", "ping") })
                    }
                    put("parts", partsArray)
                }
                put(userContent)
            }
            put("contents", contentsArray)

            val config = JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 10)
            }
            put("generationConfig", config)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(probeUrl)
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .build()

        val startTime = System.currentTimeMillis()
        try {
            val response = diagnosticClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Parse candidate text snippet
                val snippet = extractCandidateSnippet(responseBody)
                GeminiDiagnosticResult(
                    isSuccess = true,
                    testedModel = rawModel,
                    normalizedModel = normalizedModel,
                    latencyMs = latency,
                    httpStatusCode = responseCode,
                    statusSummary = "Kết nối thành công (${latency}ms)",
                    errorDetails = null,
                    suggestedActions = emptyList(),
                    rawResponseSnippet = snippet.ifBlank { "OK (HTTP $responseCode)" }
                )
            } else {
                // Parse Google API error details
                val parsedError = parseApiError(responseBody, responseCode, normalizedModel)
                GeminiDiagnosticResult(
                    isSuccess = false,
                    testedModel = rawModel,
                    normalizedModel = normalizedModel,
                    latencyMs = latency,
                    httpStatusCode = responseCode,
                    statusSummary = parsedError.summary,
                    errorDetails = parsedError.details,
                    suggestedActions = parsedError.actions,
                    rawResponseSnippet = responseBody.take(200)
                )
            }
        } catch (e: UnknownHostException) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "DNS / Host resolution error", e)
            GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                latencyMs = latency,
                httpStatusCode = null,
                statusSummary = "Lỗi kết nối mạng (DNS / Host)",
                errorDetails = "Không thể kết nối tới máy chủ Google (generativelanguage.googleapis.com). Thiết bị có thể đang mất mạng hoặc DNS bị chặn.",
                suggestedActions = listOf(
                    "Kiểm tra kết nối Wi-Fi hoặc 4G/5G trên thiết bị.",
                    "Kiểm tra xem thiết bị có đang sử dụng VPN hoặc mạng chặn dịch vụ Google không."
                )
            )
        } catch (e: SocketTimeoutException) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Socket timeout error", e)
            GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                latencyMs = latency,
                httpStatusCode = null,
                statusSummary = "Quá thời gian chờ (Timeout > 15s)",
                errorDetails = "Máy chủ không phản hồi trong vòng 15 giây. Kết nối mạng có thể quá chậm hoặc máy chủ Google đang quá tải.",
                suggestedActions = listOf(
                    "Thử lại sau ít phút.",
                    "Chuyển sang mô hình nhẹ hơn: 'gemini-3.1-flash-lite-preview' hoặc 'gemini-3.5-flash'.",
                    "Kiểm tra lại tốc độ đường truyền Internet."
                )
            )
        } catch (e: IOException) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "I/O error during diagnostic", e)
            GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                latencyMs = latency,
                httpStatusCode = null,
                statusSummary = "Lỗi truyền tải mạng (I/O)",
                errorDetails = e.message ?: "Không thể thiết lập kết nối socket tới Google API.",
                suggestedActions = listOf(
                    "Kiểm tra kết nối Internet của thiết bị.",
                    "Khởi động lại ứng dụng hoặc thử lại sau."
                )
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Unexpected error during diagnostic", e)
            GeminiDiagnosticResult(
                isSuccess = false,
                testedModel = rawModel,
                normalizedModel = normalizedModel,
                latencyMs = latency,
                httpStatusCode = null,
                statusSummary = "Lỗi không xác định",
                errorDetails = e.localizedMessage ?: e.toString(),
                suggestedActions = listOf(
                    "Kiểm tra lại cấu hình API Key và tên mô hình.",
                    "Thử sử dụng mô hình mặc định: 'gemini-3.5-flash-lite'."
                )
            )
        }
    }

    private data class ParsedError(
        val summary: String,
        val details: String,
        val actions: List<String>
    )

    private fun parseApiError(responseBody: String, code: Int, model: String): ParsedError {
        var serverMessage = ""
        var serverStatus = ""
        try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            serverMessage = error?.optString("message", "") ?: ""
            serverStatus = error?.optString("status", "") ?: ""
        } catch (_: Exception) {
            serverMessage = responseBody.take(150)
        }

        return when (code) {
            400 -> {
                if (serverMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
                    serverMessage.contains("API key not valid", ignoreCase = true)) {
                    ParsedError(
                        summary = "Khóa API Key không hợp lệ (HTTP 400)",
                        details = "Google từ chối khóa API: $serverMessage",
                        actions = listOf(
                            "Kiểm tra lại chính xác chuỗi ký tự API Key trong Google AI Studio.",
                            "Tạo một khóa mới tại https://aistudio.google.com/app/apikey.",
                            "Đảm bảo không có dấu cách hoặc ký tự ẩn ở hai đầu."
                        )
                    )
                } else {
                    ParsedError(
                        summary = "Yêu cầu không hợp lệ (HTTP 400)",
                        details = if (serverMessage.isNotBlank()) serverMessage else "Google API trả về lỗi tham số (400 Bad Request)",
                        actions = listOf(
                            "Kiểm tra lại tên mô hình ('$model').",
                            "Thử sử dụng mô hình gợi ý: 'gemini-3.5-flash-lite'."
                        )
                    )
                }
            }
            403 -> {
                ParsedError(
                    summary = "Truy cập bị từ chối / Giới hạn quyền (HTTP 403)",
                    details = if (serverMessage.isNotBlank()) serverMessage else "Quyền truy cập API bị chặn (403 Forbidden).",
                    actions = listOf(
                        "Kiểm tra xem dự án Google Cloud có bị giới hạn IP hoặc API restriction không.",
                        "Đảm bảo dịch vụ 'Generative Language API' đã được bật trong Google Cloud Console.",
                        "Tạo khóa API mới không gán hạn chế (Unrestricted key)."
                    )
                )
            }
            404 -> {
                ParsedError(
                    summary = "Không tìm thấy mô hình (HTTP 404)",
                    details = "Mô hình '$model' không tồn tại hoặc chưa được kích hoạt trên phiên bản API v1beta ($serverMessage).",
                    actions = listOf(
                        "Chuyển sang 'gemini-3.5-flash-lite' (nhanh & ổn định nhất).",
                        "Hoặc thử 'gemini-2.5-flash' hoặc 'gemini-3.5-flash'.",
                        "Đảm bảo không có lỗi chính tả trong tên mô hình."
                    )
                )
            }
            429 -> {
                ParsedError(
                    summary = "Vượt quá giới hạn gọi API (HTTP 429 Rate Limit)",
                    details = "Bạn đã gửi quá nhiều yêu cầu hoặc vượt mức hạn ngạch (Quota/RPM/TPM): $serverMessage",
                    actions = listOf(
                        "Đợi khoảng 30 đến 60 giây rồi thử lại.",
                        "Sử dụng mô hình 'gemini-3.5-flash-lite' để tiết kiệm hạn ngạch tối đa.",
                        "Nâng cấp hạn ngạch (Tier) hoặc thêm billing trong Google Cloud nếu cần tải cao."
                    )
                )
            }
            500, 502, 503, 504 -> {
                ParsedError(
                    summary = "Máy chủ Google AI gặp sự cố (HTTP $code)",
                    details = "Hệ thống máy chủ Generative Language của Google tạm thời gián đoạn: $serverMessage",
                    actions = listOf(
                        "Vui lòng thử lại sau vài giây.",
                        "Kiểm tra trang trạng thái dịch vụ của Google Cloud."
                    )
                )
            }
            else -> {
                ParsedError(
                    summary = "Lỗi phản hồi từ Google (HTTP $code)",
                    details = serverMessage.ifBlank { "Mã lỗi: $code ($serverStatus)" },
                    actions = listOf(
                        "Kiểm tra lại API Key và mô hình đã chọn.",
                        "Thử lại với mô hình 'gemini-3.5-flash-lite'."
                    )
                )
            }
        }
    }

    private fun extractCandidateSnippet(responseBody: String): String {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "").trim()
        } catch (_: Exception) {
            ""
        }
    }
}
