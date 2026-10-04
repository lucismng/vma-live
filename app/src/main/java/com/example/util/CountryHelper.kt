package com.example.util

data class CountryInfo(
    val code: String,
    val name: String,
    val flag: String
)

object CountryHelper {
    val ALL_COUNTRIES = listOf(
        CountryInfo("ALL", "Tất cả", "🌐"),
        CountryInfo("VN", "Việt Nam", "🇻🇳"),
        CountryInfo("KR", "Hàn Quốc", "🇰🇷"),
        CountryInfo("CN", "Trung Quốc", "🇨🇳"),
        CountryInfo("TH", "Thái Lan", "🇹🇭"),
        CountryInfo("UK", "Vương quốc Anh", "🇬🇧"),
        CountryInfo("IL", "Israel", "🇮🇱"),
        CountryInfo("INT", "Quốc tế", "🌍")
    )

    fun detectCountry(name: String, group: String): String {
        val upperName = name.uppercase()
        val upperGroup = group.uppercase()
        val combined = "$upperName $upperGroup"

        return when {
            // Vietnam
            combined.contains("VTV") || combined.contains("HTV") || combined.contains("THVL") ||
            combined.contains("SCTV") || combined.contains("VTC") || combined.contains("VOV") ||
            combined.contains("K+") || combined.contains("VIETNAM") || combined.contains("VIỆT NAM") ||
            combined.contains("HÀ NỘI") || combined.contains("TRUYỀN HÌNH") || combined.contains("VN") -> "VN"

            // Korea
            combined.contains("KOREA") || combined.contains("KBS") || combined.contains("SBS") ||
            combined.contains("MBC") || combined.contains("TVN") || combined.contains("JTBC") ||
            combined.contains("YTN") || combined.contains("HÀN QUỐC") || combined.contains("MNET") -> "KR"

            // China
            combined.contains("CCTV") || combined.contains("CGTN") || combined.contains("CHINA") ||
            combined.contains("TRUNG QUỐC") || combined.contains("PHOENIX") || combined.contains("HUNAN") ||
            combined.contains("ZHEJIANG") || combined.contains("DRAGON TV") -> "CN"

            // Thailand
            combined.contains("THAI") || combined.contains("THÁI LAN") || combined.contains("CH3") ||
            combined.contains("CH7") || combined.contains("ONE31") || combined.contains("GMM") ||
            combined.contains("THAIRATH") || combined.contains("MONO29") -> "TH"

            // United Kingdom
            combined.contains("BBC") || combined.contains("ITV") || combined.contains("SKY") ||
            combined.contains("CHANNEL 4") || combined.contains("CHANNEL 5") || combined.contains("UK") ||
            combined.contains("BRITISH") || combined.contains("ANH QUỐC") -> "UK"

            // Israel
            combined.contains("KAN") || combined.contains("KESHET") || combined.contains("RESHET") ||
            combined.contains("ISRAEL") || combined.contains("CHANNEL 11") || combined.contains("CHANNEL 12") ||
            combined.contains("CHANNEL 13") || combined.contains("CHANNEL 14") || combined.contains("I24") -> "IL"

            else -> "INT"
        }
    }
}
