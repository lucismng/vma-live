package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChannelItem(
    val streamUrl: String,
    val tvgId: String,
    val tvgName: String,
    val tvgLogo: String,
    val groupTitle: String,
    val channelName: String,
    val isFavorite: Boolean = false,
    val currentProgramTitle: String? = null,
    val currentProgramThumbnail: String? = null,
    val supportsCatchup: Boolean = false,
    val catchupType: String = "",
    val catchupDays: Int = 0,
    val catchupSource: String = ""
)
