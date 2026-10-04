package com.example.model

import androidx.compose.runtime.Immutable
import com.example.data.local.entity.ChannelEntity

@Immutable
enum class CountryFilter(
    val id: String,
    val displayName: String,
    val flag: String,
    val description: String
) {
    ALL("all", "Tất cả", "🌐", "Toàn bộ danh sách kênh"),
    VIETNAM("vn", "Việt Nam", "🇻🇳", "Kênh truyền hình Việt Nam"),
    KOREA("kr", "Hàn Quốc", "🇰🇷", "Kênh truyền hình Hàn Quốc"),
    CHINA("cn", "Trung Quốc", "🇨🇳", "Kênh truyền hình Trung Quốc"),
    THAILAND("th", "Thái Lan", "🇹🇭", "Kênh truyền hình Thái Lan"),
    UK("gb", "Anh Quốc", "🇬🇧", "Kênh truyền hình Anh Quốc"),
    ISRAEL("il", "Israel", "🇮🇱", "Kênh truyền hình Israel"),
    INTERNATIONAL("int", "Quốc tế", "🌍", "Kênh truyền hình quốc tế & tin tức");

    val fullLabel: String get() = "$displayName $flag"

    companion object {
        fun fromId(id: String): CountryFilter {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
        }

        /**
         * Phân loại một kênh vào quốc gia tương ứng dựa theo groupTitle, tên kênh, tvgId hoặc url
         */
        fun classify(channelName: String, groupTitle: String, tvgId: String, tvgName: String): CountryFilter {
            val name = channelName.trim().lowercase()
            val group = groupTitle.trim().lowercase()
            val id = tvgId.trim().lowercase()
            val tvg = tvgName.trim().lowercase()

            // 1. Hàn Quốc 🇰🇷
            if (group.contains("hàn quốc") || group.contains("korea") || group == "kr" || group.contains("k-pop") ||
                name.contains("kbs") || name.contains("sbs") || name.contains("mbc") || name.contains("ytn") ||
                name.contains("arirang") || name.contains("yonhap") || name.contains("tvn") || name.contains("jtbc") ||
                id.endsWith(".kr") || id.contains("korea") || tvg.contains("kbs")
            ) {
                return KOREA
            }

            // 2. Trung Quốc 🇨🇳
            if (group.contains("trung quốc") || group.contains("china") || group == "cn" ||
                name.contains("cctv") || name.contains("cgtn") || name.contains("chinanews") ||
                name.contains("phoenix") || name.contains("dragon tv") || name.contains("mango") ||
                id.endsWith(".cn") || id.contains("china")
            ) {
                return CHINA
            }

            // 3. Thái Lan 🇹🇭
            if (group.contains("thái lan") || group.contains("thailand") || group == "th" ||
                name.contains("thai pbs") || name.contains("altv") || name.contains("ch3") ||
                name.contains("ch7") || name.contains("workpoint") || name.contains("gmm") ||
                name.contains("tnn16") || name.contains("amarin") || id.endsWith(".th") || id.contains("thai")
            ) {
                return THAILAND
            }

            // 4. Anh Quốc 🇬🇧
            if (group.contains("anh quốc") || group.contains("united kingdom") || group.contains("british") ||
                group == "uk" || group == "gb" ||
                name.contains("sky news") || name.contains("sky sports") || name.contains("bbc") ||
                name.contains("gb news") || name.contains("itv") || name.contains("channel 4") ||
                name.contains("channel 5") || name.contains("talktv") || id.endsWith(".uk") || id.contains("bbc")
            ) {
                return UK
            }

            // 5. Israel 🇮🇱
            if (group.contains("israel") || group.contains("do thái") || group == "il" ||
                name.contains("kan 11") || name.contains("keshet 12") || name.contains("reshet 13") ||
                name.contains("now 14") || name.contains("i24") || name.contains("i24news") ||
                name.contains("makan 33") || name.contains("knesset") || id.endsWith(".il") || id.contains("israel")
            ) {
                return ISRAEL
            }

            // 6. Quốc tế 🌍
            if (group.contains("quốc tế") || group.contains("quoc te") || group.contains("international") ||
                group.contains("foreign") || group.contains("world") || group.contains("tin tức thế giới") ||
                name.contains("cnn") || name.contains("bloomberg") || name.contains("dw ") || name.contains("france 24") ||
                name.contains("al jazeera") || name.contains("nhk world") || name.contains("euronews") ||
                name.contains("trt world") || name.contains("cna") || name.contains("reuters") ||
                name.contains("discovery") || name.contains("national geographic") || name.contains("hbo") ||
                name.contains("cinemax") || name.contains("disney") || name.contains("cartoon network")
            ) {
                return INTERNATIONAL
            }

            // 7. Mặc định là Việt Nam 🇻🇳 (VTV, HTV, VTC, Truyền hình tỉnh, v.v.)
            return VIETNAM
        }
    }
}

fun ChannelItem.countryFilter(): CountryFilter {
    return CountryFilter.classify(channelName, groupTitle, tvgId, tvgName)
}

fun ChannelEntity.countryFilter(): CountryFilter {
    return CountryFilter.classify(channelName, groupTitle, tvgId, tvgName)
}
