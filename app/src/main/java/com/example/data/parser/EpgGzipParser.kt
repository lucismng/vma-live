package com.example.data.parser

import android.util.Xml
import com.example.data.local.entity.ProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Calendar
import java.util.TimeZone
import java.util.zip.GZIPInputStream

object EpgGzipParser {

    private const val BATCH_SIZE = 500

    /**
     * Parses an EPG stream (GZIP or raw XML) directly into batches of [ProgramEntity].
     */
    suspend fun parseStreaming(
        inputStream: InputStream,
        onBatchParsed: suspend (List<ProgramEntity>) -> Unit
    ) {
        val bufferedInput = BufferedInputStream(inputStream)
        bufferedInput.mark(2)
        val header = ByteArray(2)
        val bytesRead = bufferedInput.read(header)
        bufferedInput.reset()

        val isGzip = bytesRead == 2 && header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()
        val effectiveStream: InputStream = if (isGzip) {
            GZIPInputStream(bufferedInput)
        } else {
            bufferedInput
        }

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(effectiveStream, "UTF-8")
        }

        val batch = ArrayList<ProgramEntity>(BATCH_SIZE)
        var eventType = parser.eventType

        var currentChannel: String? = null
        var currentStart: Long? = null
        var currentStop: Long? = null
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentIcon: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "programme" -> {
                            currentChannel = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStart = startStr?.let { parseXmltvTimestamp(it) }
                            currentStop = stopStr?.let { parseXmltvTimestamp(it) }
                            currentTitle = null
                            currentDesc = null
                            currentIcon = null
                        }
                        "title" -> {
                            currentTitle = parser.nextText()?.trim()
                        }
                        "desc" -> {
                            currentDesc = parser.nextText()?.trim()
                        }
                        "icon" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (!src.isNullOrBlank()) {
                                currentIcon = src.trim()
                            }
                        }
                        "image" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (!src.isNullOrBlank()) {
                                currentIcon = src.trim()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (tagName == "programme") {
                        val channel = currentChannel
                        val start = currentStart
                        val stop = currentStop
                        val title = currentTitle

                        if (!channel.isNullOrBlank() && start != null && stop != null && !title.isNullOrBlank()) {
                            batch.add(
                                ProgramEntity(
                                    channelTvgId = channel,
                                    title = title,
                                    description = currentDesc,
                                    startTime = start,
                                    endTime = stop,
                                    thumbnailUrl = currentIcon
                                )
                            )

                            if (batch.size >= BATCH_SIZE) {
                                onBatchParsed(ArrayList(batch))
                                batch.clear()
                            }
                        }

                        currentChannel = null
                        currentStart = null
                        currentStop = null
                        currentTitle = null
                        currentDesc = null
                        currentIcon = null
                    }
                }
            }
            eventType = parser.next()
        }

        if (batch.isNotEmpty()) {
            onBatchParsed(ArrayList(batch))
            batch.clear()
        }
    }

    /**
     * Parse XMLTV timestamp such as "20260923053000 +0700", "20260923053000 Z", or "20260923053000".
     * Returns Unix Epoch milliseconds.
     */
    fun parseXmltvTimestamp(timestamp: String): Long? {
        val trimmed = timestamp.trim()
        if (trimmed.length < 14) return null
        return try {
            val year = trimmed.substring(0, 4).toInt()
            val month = trimmed.substring(4, 6).toInt()
            val day = trimmed.substring(6, 8).toInt()
            val hour = trimmed.substring(8, 10).toInt()
            val minute = trimmed.substring(10, 12).toInt()
            val second = trimmed.substring(12, 14).toInt()

            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, month - 1, day, hour, minute, second)
            }
            var timeMs = cal.timeInMillis

            val remaining = trimmed.substring(14).trim()
            if (remaining.isNotEmpty() && remaining != "Z") {
                val sign = if (remaining.startsWith("-")) -1 else 1
                val offsetDigits = remaining
                    .replace("+", "")
                    .replace("-", "")
                    .replace(":", "")
                    .trim()

                if (offsetDigits.length >= 4) {
                    val offHours = offsetDigits.substring(0, 2).toIntOrNull() ?: 0
                    val offMins = offsetDigits.substring(2, 4).toIntOrNull() ?: 0
                    val offsetMs = (offHours * 3600L + offMins * 60L) * 1000L * sign
                    timeMs -= offsetMs
                }
            }
            timeMs
        } catch (_: Exception) {
            null
        }
    }
}
