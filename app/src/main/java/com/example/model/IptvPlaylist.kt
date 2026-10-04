package com.example.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model đại diện cho một danh sách phát IPTV M3U.
 * Hỗ trợ chạy song song và gộp nhiều Playlist cùng lúc.
 */
@Immutable
data class IptvPlaylist(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val channelCount: Int = 0,
    val lastSyncTime: Long = 0L
) {
    val isHiddenAdmin: Boolean
        get() = id == "admin_m3u" || id == "vmttv" ||
                url.contains("ADMIN.m3u", ignoreCase = true) ||
                name.contains("ADMIN", ignoreCase = true) ||
                name.contains("Quản Trị", ignoreCase = true)

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("url", url)
            put("isEnabled", isEnabled)
            put("channelCount", channelCount)
            put("lastSyncTime", lastSyncTime)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): IptvPlaylist {
            return IptvPlaylist(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Playlist M3U"),
                url = json.optString("url", ""),
                isEnabled = json.optBoolean("isEnabled", true),
                channelCount = json.optInt("channelCount", 0),
                lastSyncTime = json.optLong("lastSyncTime", 0L)
            )
        }

        fun listToJson(list: List<IptvPlaylist>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String): List<IptvPlaylist> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<IptvPlaylist>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
