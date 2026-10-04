package com.example.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["channelTvgId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["channelTvgId", "startTime"], unique = true)
    ]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long, // Unix epoch milliseconds
    val endTime: Long,   // Unix epoch milliseconds
    val thumbnailUrl: String? = null
)
