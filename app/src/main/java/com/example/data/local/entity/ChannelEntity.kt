package com.example.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["groupTitle"]),
        Index(value = ["tvgId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey
    val streamUrl: String,
    val tvgId: String,
    val tvgName: String,
    val tvgLogo: String,
    val groupTitle: String,
    val channelName: String,
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val supportsCatchup: Boolean = false,
    val catchupType: String = "",
    val catchupDays: Int = 0,
    val catchupSource: String = ""
)
