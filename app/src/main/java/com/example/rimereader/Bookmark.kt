package com.example.rimereader

import androidx.room.Entity
import androidx.room.PrimaryKey

// Bookmark entity for Room database
@Entity(tableName = "bookmarks") // Table name in the database
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique identifier for each bookmark
    val songId: Long, // ID of the associated song
    val timeMillis: Int, // Time in milliseconds
    val displayTime: String // Formatted time string (in HH:MM:SS format)
)