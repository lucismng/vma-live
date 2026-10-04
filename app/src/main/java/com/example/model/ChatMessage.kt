package com.example.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedChannel: ChannelItem? = null
)
