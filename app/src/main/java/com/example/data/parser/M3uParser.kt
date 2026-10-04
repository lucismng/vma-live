package com.example.data.parser

import com.example.data.local.entity.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object M3uParser {

    private val TVG_ID_REGEX = Pattern.compile("""tvg-id=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_REGEX = Pattern.compile("""tvg-name=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("""tvg-logo=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("""group-title=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val CHANNEL_NAME_REGEX = Pattern.compile(""",\s*(.*)$""")
    private val CATCHUP_REGEX = Pattern.compile("""catchup=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val CATCHUP_DAYS_REGEX = Pattern.compile("""(?:catchup-days|timeshift)=["']?([0-9]+)["']?""", Pattern.CASE_INSENSITIVE)
    private val CATCHUP_SOURCE_REGEX = Pattern.compile("""catchup-source=["']([^"']*)["']""", Pattern.CASE_INSENSITIVE)
    private val TVG_REC_REGEX = Pattern.compile("""tvg-rec=["']?([0-9]+)["']?""", Pattern.CASE_INSENSITIVE)

    fun parse(
        inputStream: InputStream,
        onChannelParsed: ((ChannelEntity) -> Unit)? = null
    ): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var globalCatchup = ""
        var globalCatchupDays: Int? = null
        var globalCatchupSource = ""

        var currentTvgId = ""
        var currentTvgName = ""
        var currentTvgLogo = ""
        var currentGroupTitle = ""
        var currentChannelName = ""
        var currentCatchup = ""
        var currentCatchupDays: Int? = null
        var currentCatchupSource = ""
        var currentTvgRec = ""
        var hasPendingHeader = false
        var orderIndex = 0

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTM3U", ignoreCase = true)) {
                    globalCatchup = extractMatch(CATCHUP_REGEX, line)
                    globalCatchupDays = extractMatch(CATCHUP_DAYS_REGEX, line).toIntOrNull()
                    globalCatchupSource = extractMatch(CATCHUP_SOURCE_REGEX, line)
                } else if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    currentTvgId = extractMatch(TVG_ID_REGEX, line)
                    currentTvgName = extractMatch(TVG_NAME_REGEX, line)
                    currentTvgLogo = extractMatch(TVG_LOGO_REGEX, line)
                    currentGroupTitle = extractMatch(GROUP_TITLE_REGEX, line)
                    currentChannelName = extractMatch(CHANNEL_NAME_REGEX, line).ifBlank { currentTvgName }
                    currentCatchup = extractMatch(CATCHUP_REGEX, line)
                    currentCatchupDays = extractMatch(CATCHUP_DAYS_REGEX, line).toIntOrNull()
                    currentCatchupSource = extractMatch(CATCHUP_SOURCE_REGEX, line)
                    currentTvgRec = extractMatch(TVG_REC_REGEX, line)

                    if (currentGroupTitle.isBlank()) {
                        currentGroupTitle = "Khác"
                    }

                    hasPendingHeader = true
                } else if (!line.startsWith("#") && hasPendingHeader) {
                    val streamUrl = line
                    if (streamUrl.startsWith("http://", ignoreCase = true) || 
                        streamUrl.startsWith("https://", ignoreCase = true) ||
                        streamUrl.startsWith("rtmp://", ignoreCase = true) ||
                        streamUrl.startsWith("rtsp://", ignoreCase = true)) {
                        
                        val resolvedName = currentChannelName.ifBlank {
                            currentTvgName.ifBlank { "Kênh ${orderIndex + 1}" }
                        }

                        // Determine catch-up support strictly from explicit M3U attributes on the channel
                        val channelDisabled = currentCatchup.equals("none", ignoreCase = true) || 
                                              currentCatchup.equals("false", ignoreCase = true)

                        val hasChannelCatchup = currentCatchup.isNotBlank() && !channelDisabled
                        val hasChannelDays = (currentCatchupDays != null && currentCatchupDays > 0)
                        val hasChannelSource = currentCatchupSource.isNotBlank()
                        val hasRecTag = currentTvgRec == "1"

                        // Catch-up is supported only if channel explicitly declares catchup attributes or valid source
                        val isCatchupSupported = !channelDisabled && (
                            hasChannelCatchup || 
                            hasChannelDays || 
                            hasChannelSource || 
                            hasRecTag
                        )

                        val resolvedType = when {
                            currentCatchup.isNotBlank() && !channelDisabled -> currentCatchup
                            globalCatchup.isNotBlank() && !channelDisabled -> globalCatchup
                            else -> "append"
                        }

                        val explicitDays = currentCatchupDays ?: globalCatchupDays
                        val resolvedDays = explicitDays ?: if (isCatchupSupported) 7 else 0
                        val explicitSource = currentCatchupSource.ifBlank { globalCatchupSource }

                        val channel = ChannelEntity(
                            streamUrl = streamUrl,
                            tvgId = currentTvgId,
                            tvgName = currentTvgName.ifBlank { resolvedName },
                            tvgLogo = currentTvgLogo,
                            groupTitle = currentGroupTitle,
                            channelName = resolvedName,
                            orderIndex = orderIndex++,
                            isFavorite = false,
                            supportsCatchup = isCatchupSupported,
                            catchupType = if (isCatchupSupported) resolvedType else "",
                            catchupDays = resolvedDays,
                            catchupSource = explicitSource
                        )
                        channels.add(channel)
                        onChannelParsed?.invoke(channel)
                    }
                    hasPendingHeader = false
                    currentTvgId = ""
                    currentTvgName = ""
                    currentTvgLogo = ""
                    currentGroupTitle = ""
                    currentChannelName = ""
                    currentCatchup = ""
                    currentCatchupDays = null
                    currentCatchupSource = ""
                    currentTvgRec = ""
                }
            }
        }

        return channels
    }

    private fun extractMatch(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.trim() ?: ""
        } else {
            ""
        }
    }
}
