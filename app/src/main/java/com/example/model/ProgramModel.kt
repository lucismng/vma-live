package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProgramItem(
    val id: Long,
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val isLive: Boolean = false,
    val progressFraction: Float = 0f,
    val timeRangeFormatted: String = "",
    val thumbnailUrl: String? = null
)

@Immutable
data class SyncState(
    val isSyncingM3u: Boolean = false,
    val isSyncingEpg: Boolean = false,
    val lastSyncTime: Long = 0L,
    val channelCount: Int = 0,
    val programCount: Int = 0,
    val message: String? = null
)
