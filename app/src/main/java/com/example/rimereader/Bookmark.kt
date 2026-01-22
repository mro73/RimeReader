package com.example.rimereader

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Long,
    val timeMillis: Int,
    val displayTime: String
)