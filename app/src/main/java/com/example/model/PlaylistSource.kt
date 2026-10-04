package com.example.model

data class PlaylistSource(
    val id: String,
    val name: String,
    val m3uUrl: String,
    val epgUrl: String = "",
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true
)
