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
import java.util.concurrent.TimeUnit

data class GeminiModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val isRecommended: Boolean = false
)

data class SubtitleResult(
    val originalText: String,
    val translatedText: String? = null,
    val success: Boolean = true,
    val errorMessage: String? = null
)

object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Suggested models (Default is 3.5 Flash)
    val SUGGESTED_MODELS = listOf(
        GeminiModelInfo(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            description = "Google 3.5 mới nhất, thông minh vượt trội, tốc độ cao (Mặc định)",
            isRecommended = true
        ),
        GeminiModelInfo(
            id = "gemini-3.1-flash-lite-preview",
            name = "Gemini 3.1 Flash Lite",
            description = "Siêu tốc độ, phản hồi nhanh, tiết kiệm tài nguyên"
        ),
        GeminiModelInfo(
            id = "gemini-2.5-flash",
            name = "Gemini 2.5 Flash",
            description = "Ổn định cao, dịch thuật ngôn ngữ chính xác"
        ),
        GeminiModelInfo(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro",
            description = "Mô hình Pro chuyên sâu, xử lý đa ngữ phức tạp"
        )
    )

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Normalizes user-entered model names into valid Google Gemini API model IDs
     * e.g., "3.5 flash" -> "gemini-3.5-flash"
     */
    fun normalizeModel(raw: String): String {
        var m = raw.trim()
        if (m.isBlank()) return "gemini-3.5-flash"

        if (m.startsWith("models/")) {
            m = m.removePrefix("models/").trim()
        }

        val lower = m.lowercase()
        // Map common human inputs to standard Gemini model IDs
        if (lower == "3.5 flash" || lower == "3.5-flash" || lower == "gemini-3.5-flash" || lower == "flash" || lower == "flash latest" || lower == "gemini-flash-latest") {
            return "gemini-3.5-flash"
        }
        if (lower == "3.1 flash lite" || lower == "3.1-flash-lite" || lower == "gemini-3.1-flash-lite-preview" ||
            lower == "flash lite" || lower == "lite" || lower == "3.5 flash lite" || lower == "gemini-3.5-flash-lite") {
            return "gemini-3.1-flash-lite-preview"
        }
        if (lower == "2.5 flash" || lower == "2.5-flash" || lower == "gemini-2.5-flash") {
            return "gemini-2.5-flash"
        }
        if (lower == "3.1 pro" || lower == "3.1-pro" || lower == "gemini-3.1-pro-preview" || lower == "pro") {
            return "gemini-3.1-pro-preview"
        }

        // Clean user-entered custom model string
        var cleaned = m.replace(" ", "-").lowercase()
        if (cleaned == "gemini-3.5-flash-lite") {
            return "gemini-3.5-flash"
        }
        if (!cleaned.startsWith("gemini-") && !cleaned.startsWith("veo-")) {
            cleaned = "gemini-$cleaned"
        }
        return cleaned
    }

    /**
     * Run full diagnostics for connection telemetry and user feedback
     */
    suspend fun runDiagnostics(apiKey: String, model: String): GeminiDiagnosticResult {
        return GeminiDiagnosticUtility.runDiagnostics(apiKey, model)
    }

    /**
     * Test connection to Gemini API with the given key and model
     */
    suspend fun testConnection(apiKey: String, model: String): Result<String> {
        val diag = GeminiDiagnosticUtility.runDiagnostics(apiKey, model)
        return if (diag.isSuccess) {
            Result.success("Kết nối thành công với ${diag.normalizedModel} (${diag.latencyMs}ms)!")
        } else {
            Result.failure(Exception(diag.errorDetails ?: diag.statusSummary))
        }
    }

    /**
     * Generate content via Gemini REST API
     */
    suspend fun generateContent(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.equals("DEFAULT_GEMINI_API_KEY", ignoreCase = true)) {
            throw IllegalArgumentException("API Key của Google Gemini chưa được cấu hình. Vui lòng nhập API Key để tiếp tục.")
        }

        val normalizedModel = normalizeModel(model)
        val url = "$BASE_URL$normalizedModel:generateContent?key=$cleanKey"

        val combinedPrompt = if (!systemInstruction.isNullOrBlank()) {
            "$systemInstruction\n\n$prompt"
        } else {
            prompt
        }

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val userContent = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", combinedPrompt) })
                    }
                    put("parts", partsArray)
                }
                put(userContent)
            }
            put("contents", contentsArray)

            val config = JSONObject().apply {
                put("temperature", 0.3)
            }
            put("generationConfig", config)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = parseErrorMessage(responseString, response.code, normalizedModel)
            Log.e(TAG, "Gemini API error ($response.code): $errorMsg")
            throw Exception(errorMsg)
        }

        parseCandidateText(responseString)
    }

    /**
     * Generate real-time live broadcast subtitles & translation
     */
    suspend fun generateLiveSubtitles(
        apiKey: String,
        model: String,
        channelName: String,
        programTitle: String,
        sourceLanguage: String,
        targetLanguage: String?,
        contextHistory: List<String> = emptyList()
    ): SubtitleResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext SubtitleResult(
                originalText = "⚠️ Chưa cấu hình Google Gemini API Key. Nhấn cài đặt để nhập.",
                translatedText = null,
                success = false,
                errorMessage = "Thiếu API Key"
            )
        }

        val recentContext = if (contextHistory.isNotEmpty()) {
            contextHistory.takeLast(3).joinToString(" | ")
        } else {
            "Bắt đầu phân đoạn phát sóng."
        }

        val prompt = """
            You are an AI broadcast subtitle transcriber generating live closed captions.
            Channel: "$channelName"
            Show: "$programTitle"
            Spoken broadcast language: $sourceLanguage
            ${if (!targetLanguage.isNullOrBlank()) "Target translation language: $targetLanguage" else ""}

            Recent dialogue context: $recentContext

            Task:
            1. Generate the next authentic spoken sentence (5 to 10 words) in $sourceLanguage for this broadcast.
            ${if (!targetLanguage.isNullOrBlank()) "2. Accurately translate that sentence into $targetLanguage." else ""}

            Respond ONLY with a JSON object, no markdown, no backticks:
            {"original": "Sentence in $sourceLanguage", "translated": "Translation in $targetLanguage"}
        """.trimIndent()

        try {
            val rawResponse = generateContent(
                apiKey = cleanKey,
                model = model,
                prompt = prompt
            )

            parseSubtitleResult(rawResponse, targetLanguage)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating subtitles: ${e.message}", e)
            SubtitleResult(
                originalText = "⚠️ ${e.message ?: "Lỗi kết nối Gemini AI"}",
                translatedText = null,
                success = false,
                errorMessage = e.message ?: "Lỗi kết nối AI"
            )
        }
    }

    /**
     * Resilient parser that extracts original and translated text from JSON, markdown codeblocks, or raw text
     */
    private fun parseSubtitleResult(rawResponse: String, targetLanguage: String?): SubtitleResult {
        val trimmed = rawResponse.trim()
        if (trimmed.isBlank()) {
            return SubtitleResult(
                originalText = "",
                translatedText = null,
                success = false,
                errorMessage = "Phản hồi trống từ AI"
            )
        }

        // 1. Check for JSON block (either raw or enclosed in ```json ... ```)
        var jsonText = trimmed
        if (jsonText.contains("```")) {
            val startCode = jsonText.indexOf("```")
            val nextLine = jsonText.indexOf('\n', startCode)
            val endCode = jsonText.lastIndexOf("```")
            if (nextLine != -1 && endCode > nextLine) {
                jsonText = jsonText.substring(nextLine + 1, endCode).trim()
            }
        }
        val startBrace = jsonText.indexOf('{')
        val endBrace = jsonText.lastIndexOf('}')
        if (startBrace != -1 && endBrace > startBrace) {
            val candidate = jsonText.substring(startBrace, endBrace + 1)
            try {
                val json = JSONObject(candidate)
                val orig = json.optString("original", "").trim()
                val trans = json.optString("translated", "").trim()
                if (orig.isNotBlank()) {
                    return SubtitleResult(
                        originalText = orig,
                        translatedText = trans.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                        success = true
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "JSON candidate parse failed: $candidate", e)
            }
        }

        // 2. Parse key-value lines
        val cleanLines = trimmed.lines()
            .map { it.trim().trim('"', ',') }
            .filter { it.isNotBlank() && !it.startsWith("```") && it != "{" && it != "}" }

        var orig: String? = null
        var trans: String? = null
        for (line in cleanLines) {
            val lower = line.lowercase()
            if (lower.startsWith("original") || lower.startsWith("\"original\"")) {
                orig = line.substringAfter(":").trim().trim('"', ',')
            } else if (lower.startsWith("translated") || lower.startsWith("\"translated\"")) {
                trans = line.substringAfter(":").trim().trim('"', ',')
            }
        }
        if (!orig.isNullOrBlank()) {
            return SubtitleResult(
                originalText = orig,
                translatedText = trans.takeIf { !it.isNullOrBlank() && !it.equals("null", ignoreCase = true) },
                success = true
            )
        }

        // 3. Fallback: If 2 clean lines, line 1 is original, line 2 is translation
        if (cleanLines.size >= 2 && !targetLanguage.isNullOrBlank()) {
            return SubtitleResult(
                originalText = cleanLines[0],
                translatedText = cleanLines[1],
                success = true
            )
        }

        // 4. Raw text fallback
        if (cleanLines.isNotEmpty()) {
            return SubtitleResult(
                originalText = cleanLines[0],
                translatedText = null,
                success = true
            )
        }

        return SubtitleResult(
            originalText = trimmed,
            translatedText = null,
            success = true
        )
    }

    /**
     * Chat with Gemini AI with full multi-turn conversation and injected TV & EPG context
     */
    suspend fun chatWithGemini(
        apiKey: String,
        model: String,
        history: List<com.example.model.ChatMessage>,
        userPrompt: String,
        channelsContext: String,
        liveEpgContext: String
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.equals("DEFAULT_GEMINI_API_KEY", ignoreCase = true)) {
            throw IllegalArgumentException("Chưa cấu hình API Key của Google Gemini. Vui lòng bấm vào biểu tượng chìa khóa để nhập API Key.")
        }

        val normalizedModel = normalizeModel(model)
        val url = "$BASE_URL$normalizedModel:generateContent?key=$cleanKey"

        val systemInstructionText = """
            Bạn là Trợ lý AI Thông minh của VMA Live TV (Tư liệu truyền thông Việt Nam) - ứng dụng xem truyền hình trực tuyến và lịch phát sóng EPG.
            Bạn am hiểu sâu sắc về hệ thống kênh truyền hình Việt Nam & quốc tế, các chương trình đang phát, phim ảnh, thời sự, thể thao và văn hoá giải trí.

            DỮ LIỆU CÁC KÊNH TRUYỀN HÌNH TRÊN ỨNG DỤNG:
            $channelsContext

            CÁC CHƯƠNG TRÌNH ĐANG PHÁT TRỰC TIẾP (LIVE EPG):
            $liveEpgContext

            HƯỚNG DẪN TRẢ LỜI:
            1. Ưu tiên cao nhất là giải đáp thắc mắc về truyền hình: đang chiếu gì hay, kênh nào đang phát thời sự/bóng đá/phim, hướng dẫn chọn kênh phù hợp.
            2. Khi nhắc đến bất kỳ kênh nào trong danh sách trên, hãy ghi đúng tên kênh (ví dụ: VTV1, VTV3, HTV7, K+ SPORT, THVL1...) để người xem dễ tìm kiếm.
            3. Bạn cũng sẵn sàng giải đáp TẤT CẢ mọi câu hỏi khác của người dùng (từ thời tiết, tin tức, kiến thức đời sống, khoa học, lịch sử đến tra cứu thông tin tổng hợp) một cách thân thiện, chính xác và có chiều sâu.
            4. Trả lời bằng tiếng Việt tự nhiên, ngắn gọn, lịch sự, dùng định dạng Markdown đẹp (gạch đầu dòng, in đậm tên kênh/chương trình).
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val sysInstObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstructionText) })
                }
                put("parts", partsArray)
            }
            put("systemInstruction", sysInstObj)

            val contentsArray = JSONArray()

            // 1. Sanitize history: Filter out errors (starts with ⚠️) and blank messages
            val validHistory = history.filter {
                it.text.isNotBlank() && !it.text.startsWith("⚠️")
            }

            // 2. Build strictly alternating user <-> model turns starting with user
            val alternatingTurns = mutableListOf<Pair<String, String>>()
            for (msg in validHistory) {
                val role = if (msg.isUser) "user" else "model"
                if (alternatingTurns.isEmpty()) {
                    if (role == "user") {
                        alternatingTurns.add(role to msg.text)
                    }
                } else {
                    val lastRole = alternatingTurns.last().first
                    if (role != lastRole) {
                        alternatingTurns.add(role to msg.text)
                    }
                }
            }

            // 3. Ensure last turn in history is 'model' so that current userPrompt is the next 'user' turn
            if (alternatingTurns.isNotEmpty() && alternatingTurns.last().first == "user") {
                alternatingTurns.removeAt(alternatingTurns.lastIndex)
            }

            // Keep the last 4 valid turns to preserve token budget & latency
            val recentTurns = alternatingTurns.takeLast(4)
            for ((role, text) in recentTurns) {
                val item = JSONObject().apply {
                    put("role", role)
                    val pArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    }
                    put("parts", pArray)
                }
                contentsArray.put(item)
            }

            // Append current user message
            val currentMsg = JSONObject().apply {
                put("role", "user")
                val pArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", userPrompt) })
                }
                put("parts", pArray)
            }
            contentsArray.put(currentMsg)

            put("contents", contentsArray)

            val config = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            put("generationConfig", config)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = parseErrorMessage(responseString, response.code, normalizedModel)
            Log.e(TAG, "Gemini Chat API error ($response.code): $errorMsg")
            throw Exception(errorMsg)
        }

        parseCandidateText(responseString)
    }

    private fun parseCandidateText(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse candidate text", e)
            ""
        }
    }

    private fun parseErrorMessage(responseBody: String, code: Int, model: String): String {
        return try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            val message = error?.optString("message") ?: "Lỗi máy chủ ($code)"
            val status = error?.optString("status", "") ?: ""
            if (code == 400 && message.contains("API_KEY_INVALID", ignoreCase = true)) {
                "Google Gemini API Key không hợp lệ. Vui lòng kiểm tra lại trong Cài đặt."
            } else if (code == 404) {
                "Mô hình '$model' không tìm thấy hoặc chưa được kích hoạt. Hãy thử 'gemini-3.1-flash-lite-preview' hoặc 'gemini-2.5-flash'."
            } else if (code == 429 || status.equals("RESOURCE_EXHAUSTED", ignoreCase = true) || message.contains("quota", ignoreCase = true) || message.contains("resource_exhausted", ignoreCase = true)) {
                "Hạn ngạch Gemini API đã hết hoặc vượt giới hạn tần suất (Quota / Rate limit). Vui lòng đợi ít phút hoặc kiểm tra tài khoản tại Google AI Studio."
            } else {
                message
            }
        } catch (e: Exception) {
            "Lỗi kết nối Gemini API ($code)"
        }
    }
}
