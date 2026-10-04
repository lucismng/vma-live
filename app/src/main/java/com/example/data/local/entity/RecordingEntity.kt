package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val channelName: String,
    val channelLogo: String? = null,
    val programTitle: String? = null,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val durationSeconds: Long = 0L,
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long = 0L,
    val targetDestination: String = "LOCAL", // "LOCAL" or "GOOGLE_DRIVE"
    val driveFileId: String? = null,
    val driveWebViewLink: String? = null,
    val isUploadedToDrive: Boolean = false,
    val status: String = "RECORDING" // "RECORDING", "COMPLETED", "FAILED", "UPLOADING"
)
