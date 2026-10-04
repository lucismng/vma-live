package com.example.util

import com.example.model.ChannelItem
import com.example.model.ProgramItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for detecting IPTV Catch-up / Timeshift / Replay support
 * and constructing playback URLs for past programs.
 */
object CatchupHelper {

    /**
     * Checks whether a program is eligible for replay on a channel.
     * Must be:
     * 1. Channel supports catchup
     * 2. Program has finished or started in the past
     * 3. Program is within the catchup-days retention window (default 7 days)
     */
    fun isProgramReplayable(channel: ChannelItem, program: ProgramItem): Boolean {
        if (!channel.supportsCatchup) return false
        val now = System.currentTimeMillis()
        if (program.startTime >= now) return false // Future program cannot be replayed

        val maxCatchupDays = if (channel.catchupDays > 0) channel.catchupDays else 7
        val earliestAllowedMillis = now - (maxCatchupDays.toLong() * 24L * 60L * 60L * 1000L)
        return program.startTime >= earliestAllowedMillis
    }

    /**
     * Builds the playable stream URL for a past program using IPTV standard catchup protocols:
     * - append: appends query params (e.g. ?utc={utc}&lutc={lutc})
     * - shift: appends timeshift offset query (e.g. ?timeshift={offset})
     * - flussonic: rewrites stream path to /timeshift_abs-{start}.m3u8 or /archive-{start}-{duration}.m3u8
     * - xc (Xtream Codes): rewrites live path to timeshift path or appends query
     * - custom template: replaces ${start}, ${end}, ${duration}, ${offset}, ${timestamp}, etc.
     */
    fun buildCatchupUrl(channel: ChannelItem, program: ProgramItem): String {
        val streamUrl = channel.streamUrl.trim()
        val startSec = program.startTime / 1000L
        val endSec = program.endTime / 1000L
        val durationSec = ((program.endTime - program.startTime) / 1000L).coerceAtLeast(60L)
        val nowSec = System.currentTimeMillis() / 1000L
        val offsetSec = (nowSec - startSec).coerceAtLeast(0L)

        // Time formatters in UTC
        val utcFormat = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcStartStr = utcFormat.format(program.startTime)

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = program.startTime
        }
        val yyyy = cal.get(Calendar.YEAR).toString()
        val mm = "%02d".format(cal.get(Calendar.MONTH) + 1)
        val dd = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
        val hh = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
        val min = "%02d".format(cal.get(Calendar.MINUTE))
        val ss = "%02d".format(cal.get(Calendar.SECOND))

        val template = channel.catchupSource.trim()
        if (template.isNotBlank()) {
            val replaced = template
                .replace("\${start}", startSec.toString())
                .replace("{start}", startSec.toString())
                .replace("\${utc}", startSec.toString())
                .replace("{utc}", startSec.toString())
                .replace("\${end}", endSec.toString())
                .replace("{end}", endSec.toString())
                .replace("\${lutc}", endSec.toString())
                .replace("{lutc}", endSec.toString())
                .replace("\${duration}", durationSec.toString())
                .replace("{duration}", durationSec.toString())
                .replace("\${offset}", offsetSec.toString())
                .replace("{offset}", offsetSec.toString())
                .replace("\${timestamp}", nowSec.toString())
                .replace("{timestamp}", nowSec.toString())
                .replace("\${Y}", yyyy)
                .replace("\${m}", mm)
                .replace("\${d}", dd)
                .replace("\${H}", hh)
                .replace("\${M}", min)
                .replace("\${S}", ss)
                .replace("{Y}", yyyy)
                .replace("{m}", mm)
                .replace("{d}", dd)
                .replace("{H}", hh)
                .replace("{M}", min)
                .replace("{S}", ss)

            return when {
                replaced.startsWith("http://", ignoreCase = true) || replaced.startsWith("https://", ignoreCase = true) -> replaced
                replaced.startsWith("?") || replaced.startsWith("&") -> {
                    val delimiter = if (streamUrl.contains("?")) "&" else "?"
                    val cleanParam = replaced.removePrefix("?").removePrefix("&")
                    "$streamUrl$delimiter$cleanParam"
                }
                else -> {
                    val delimiter = if (streamUrl.contains("?")) "&" else "?"
                    "$streamUrl$delimiter$replaced"
                }
            }
        }

        // Default handling based on catchupType
        val type = channel.catchupType.lowercase()
        return when {
            type.contains("flussonic") || type.contains("fs") -> {
                when {
                    streamUrl.contains("/video.m3u8") -> streamUrl.replace("/video.m3u8", "/archive-$startSec-$durationSec.m3u8")
                    streamUrl.contains("/mono.m3u8") -> streamUrl.replace("/mono.m3u8", "/archive-$startSec-$durationSec.m3u8")
                    streamUrl.contains(".m3u8") -> streamUrl.replace(".m3u8", "/timeshift_abs-$startSec.m3u8")
                    else -> appendQueryParams(streamUrl, "utc=$startSec&lutc=$endSec")
                }
            }
            type.contains("shift") || type.contains("timeshift") -> {
                appendQueryParams(streamUrl, "timeshift=$offsetSec")
            }
            type.contains("xc") -> {
                if (streamUrl.contains("/live/")) {
                    streamUrl.replace("/live/", "/timeshift/")
                        .replace(".m3u8", "/$durationSec/$utcStartStr.ts")
                } else {
                    appendQueryParams(streamUrl, "utc=$startSec&lutc=$endSec")
                }
            }
            else -> {
                // Standard append mode
                appendQueryParams(streamUrl, "utc=$startSec&lutc=$endSec")
            }
        }
    }

    private fun appendQueryParams(url: String, params: String): String {
        val delimiter = if (url.contains("?")) "&" else "?"
        return "$url$delimiter$params"
    }
}
