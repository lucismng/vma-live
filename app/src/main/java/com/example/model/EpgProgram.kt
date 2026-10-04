package com.example.model

data class EpgProgram(
    val id: String = "",
    val channelId: String = "",
    val title: String = "",
    val description: String = "",
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val formattedTime: String = ""
)
