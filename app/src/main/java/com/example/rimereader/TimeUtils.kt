package com.example.rimereader

import java.util.concurrent.TimeUnit

// Time formatting utility
object TimeUtils {
    fun formatTime(milliseconds: Int): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds.toLong()) // Get hours from milliseconds
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()) % 60 // Get minutes from milliseconds
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60 // Get seconds from milliseconds
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds) // If longer than an hour, format time with hours, minutes and seconds
        } else {
            String.format("%d:%02d", minutes, seconds) // If shorter than an hour, format time with minutes and seconds
        }
    }
}