package com.example.rimereader

import android.net.Uri

// Audio file data class with properties
data class AudioFile(
    val id: Long, // Unique identifier
    val title: String, // Title of the audio file
    val uri: Uri, // URI (safe path) of the audio file
    val duration: Int // Duration in milliseconds
)