package com.example.model

import com.example.data.local.entity.ChannelEntity

object InternationalChannelsSeed {

    fun getSeedChannels(): List<ChannelEntity> {
        val list = mutableListOf<ChannelEntity>()
        var order = 1000

        // ==========================================
        // 1. HÀN QUỐC 🇰🇷
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://kbs-world-live.akamaized.net/hls/live/2043446/kbs_world/master.m3u8",
                tvgId = "KBSWorld.kr",
                tvgName = "KBS World HD",
                tvgLogo = "https://i.imgur.com/eQ4Lp6Z.png",
                groupTitle = "Hàn Quốc 🇰🇷",
                channelName = "KBS World HD 🇰🇷",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://amdlive-ch01-ctnd-hls.akamaized.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8",
                tvgId = "ArirangTV.kr",
                tvgName = "Arirang TV",
                tvgLogo = "https://i.imgur.com/0P6Uq2Y.png",
                groupTitle = "Hàn Quốc 🇰🇷",
                channelName = "Arirang TV Korea 🇰🇷",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://live.ytn.co.kr/live/ytn_live.smil/playlist.m3u8",
                tvgId = "YTN.kr",
                tvgName = "YTN News 24",
                tvgLogo = "https://i.imgur.com/7w8r1Yq.png",
                groupTitle = "Hàn Quốc 🇰🇷",
                channelName = "YTN News 24 🇰🇷",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://live-yonhap.hls.akamaized.net/live/yonhap.m3u8",
                tvgId = "YonhapNews.kr",
                tvgName = "Yonhap News TV",
                tvgLogo = "https://i.imgur.com/1G6Kj7H.png",
                groupTitle = "Hàn Quốc 🇰🇷",
                channelName = "Yonhap News TV 🇰🇷",
                orderIndex = order++,
                isFavorite = false
            )
        )

        // ==========================================
        // 2. TRUNG QUỐC 🇨🇳
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://news.cgtn.com/resource/live/english/cgtn-news.m3u8",
                tvgId = "CGTN.cn",
                tvgName = "CGTN News",
                tvgLogo = "https://i.imgur.com/6U9Tq8Z.png",
                groupTitle = "Trung Quốc 🇨🇳",
                channelName = "CGTN News China 🇨🇳",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://news.cgtn.com/resource/live/documentary/cgtn-doc.m3u8",
                tvgId = "CGTNDocumentary.cn",
                tvgName = "CGTN Documentary",
                tvgLogo = "https://i.imgur.com/6U9Tq8Z.png",
                groupTitle = "Trung Quốc 🇨🇳",
                channelName = "CGTN Documentary 🇨🇳",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://news.cgtn.com/resource/live/french/cgtn-fr.m3u8",
                tvgId = "CGTNFrancais.cn",
                tvgName = "CGTN Français",
                tvgLogo = "https://i.imgur.com/6U9Tq8Z.png",
                groupTitle = "Trung Quốc 🇨🇳",
                channelName = "CGTN Français 🇨🇳",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://cctv4.akamaized.net/hls/live/2014167/cctv4/master.m3u8",
                tvgId = "CCTV4.cn",
                tvgName = "CCTV-4 International",
                tvgLogo = "https://i.imgur.com/w9U1m5K.png",
                groupTitle = "Trung Quốc 🇨🇳",
                channelName = "CCTV-4 Quốc Tế 🇨🇳",
                orderIndex = order++,
                isFavorite = false
            )
        )

        // ==========================================
        // 3. THÁI LAN 🇹🇭
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://thaipbs-live.cdn.byteark.com/live/playlist.m3u8",
                tvgId = "ThaiPBS.th",
                tvgName = "Thai PBS HD",
                tvgLogo = "https://i.imgur.com/8Q7V6wP.png",
                groupTitle = "Thái Lan 🇹🇭",
                channelName = "Thai PBS HD 🇹🇭",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://altv-live.cdn.byteark.com/live/playlist.m3u8",
                tvgId = "ALTV.th",
                tvgName = "ALTV Thailand",
                tvgLogo = "https://i.imgur.com/y4L2m8R.png",
                groupTitle = "Thái Lan 🇹🇭",
                channelName = "ALTV Thái Lan 🇹🇭",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://tnn16-live.cdn.byteark.com/live/playlist.m3u8",
                tvgId = "TNN16.th",
                tvgName = "TNN 16 Thailand",
                tvgLogo = "https://i.imgur.com/m2Z5X1A.png",
                groupTitle = "Thái Lan 🇹🇭",
                channelName = "TNN 16 Tin Tức 🇹🇭",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://parliament-live.cdn.byteark.com/live/playlist.m3u8",
                tvgId = "ParliamentTV.th",
                tvgName = "Parliament TV Thai",
                tvgLogo = "https://i.imgur.com/3B1X8qY.png",
                groupTitle = "Thái Lan 🇹🇭",
                channelName = "Truyền Hình Quốc Hội Thái 🇹🇭",
                orderIndex = order++,
                isFavorite = false
            )
        )

        // ==========================================
        // 4. ANH QUỐC 🇬🇧
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://live.sky.com/hls/live/2009285/skynewsgb/master.m3u8",
                tvgId = "SkyNews.uk",
                tvgName = "Sky News UK HD",
                tvgLogo = "https://i.imgur.com/4N3m1PZ.png",
                groupTitle = "Anh Quốc 🇬🇧",
                channelName = "Sky News UK 🇬🇧",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://stream.gbnews.uk/live/master.m3u8",
                tvgId = "GBNews.uk",
                tvgName = "GB News",
                tvgLogo = "https://i.imgur.com/7L3m1PZ.png",
                groupTitle = "Anh Quốc 🇬🇧",
                channelName = "GB News UK 🇬🇧",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://talktv-live.akamaized.net/hls/live/2034927/talktv/master.m3u8",
                tvgId = "TalkTV.uk",
                tvgName = "TalkTV UK",
                tvgLogo = "https://i.imgur.com/2P5m1PZ.png",
                groupTitle = "Anh Quốc 🇬🇧",
                channelName = "TalkTV UK 🇬🇧",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://liveprodupm.global.ssl.fastly.net/us/Channel-HD-AWS-westeurope-1/live.m3u8",
                tvgId = "BloombergUK.uk",
                tvgName = "Bloomberg UK HD",
                tvgLogo = "https://i.imgur.com/9K3m1PZ.png",
                groupTitle = "Anh Quốc 🇬🇧",
                channelName = "Bloomberg UK 🇬🇧",
                orderIndex = order++,
                isFavorite = false
            )
        )

        // ==========================================
        // 5. ISRAEL 🇮🇱
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://bcovlive-a.akamaihd.net/19b02700e5764d0a8e0f6b3e6c3be5ec/eu-central-1/5377161796001/playlist.m3u8",
                tvgId = "i24NewsEN.il",
                tvgName = "i24 News English",
                tvgLogo = "https://i.imgur.com/5V3m1PZ.png",
                groupTitle = "Israel 🇮🇱",
                channelName = "i24 News English 🇮🇱",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://bcovlive-a.akamaihd.net/06a090e94bb548489d81d77cbcf8b84d/eu-central-1/5377161796001/playlist.m3u8",
                tvgId = "i24NewsFR.il",
                tvgName = "i24 News Français",
                tvgLogo = "https://i.imgur.com/5V3m1PZ.png",
                groupTitle = "Israel 🇮🇱",
                channelName = "i24 News Français 🇮🇱",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://kan11.media.kan.org.il/hls/live/2024514/2024514/master.m3u8",
                tvgId = "Kan11.il",
                tvgName = "Kan 11 HD",
                tvgLogo = "https://i.imgur.com/8N4m1PZ.png",
                groupTitle = "Israel 🇮🇱",
                channelName = "Kan 11 Israel 🇮🇱",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://makan.media.kan.org.il/hls/live/2024623/2024623/master.m3u8",
                tvgId = "Makan33.il",
                tvgName = "Makan 33 Israel",
                tvgLogo = "https://i.imgur.com/1N4m1PZ.png",
                groupTitle = "Israel 🇮🇱",
                channelName = "Makan 33 Israel 🇮🇱",
                orderIndex = order++,
                isFavorite = false
            )
        )

        // ==========================================
        // 6. QUỐC TẾ 🌍
        // ==========================================
        list.add(
            ChannelEntity(
                streamUrl = "https://euronews-euronews-world-1-au.samsung.wurl.tv/playlist.m3u8",
                tvgId = "Euronews.int",
                tvgName = "Euronews English",
                tvgLogo = "https://i.imgur.com/3Q9Tq8Z.png",
                groupTitle = "Quốc tế 🌍",
                channelName = "Euronews World 🌍",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                tvgId = "DW.int",
                tvgName = "DW English News",
                tvgLogo = "https://i.imgur.com/3W1m1PZ.png",
                groupTitle = "Quốc tế 🌍",
                channelName = "DW English HD 🌍",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                tvgId = "France24.int",
                tvgName = "France 24 English",
                tvgLogo = "https://i.imgur.com/9P2m1PZ.png",
                groupTitle = "Quốc tế 🌍",
                channelName = "France 24 English 🌍",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
                tvgId = "AlJazeera.int",
                tvgName = "Al Jazeera English",
                tvgLogo = "https://i.imgur.com/8Q3m1PZ.png",
                groupTitle = "Quốc tế 🌍",
                channelName = "Al Jazeera English 🌍",
                orderIndex = order++,
                isFavorite = false
            )
        )
        list.add(
            ChannelEntity(
                streamUrl = "https://nhkworld.web.hls.stream.co.jp/nhkworld/live/master.m3u8",
                tvgId = "NHKWorld.int",
                tvgName = "NHK World-Japan",
                tvgLogo = "https://i.imgur.com/7P3m1PZ.png",
                groupTitle = "Quốc tế 🌍",
                channelName = "NHK World-Japan 🌍",
                orderIndex = order++,
                isFavorite = false
            )
        )

        return list
    }
}
