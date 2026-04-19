package com.example.rimereader

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

// A method to get audio files from the device's storage
class AudioRepository(private val context: Context) {
    fun getAudioFiles(): List<AudioFile> {
        val tempPlaylist = mutableListOf<AudioFile>() // Temporary list to store audio files
        val projection = arrayOf( // Array of values to query
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC" // Sort ascending by title

        val cursor = context.contentResolver.query( // Query for getting audio files
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // Get audio files from external storage
            projection, // Values to query
            null, // All files
            null,
            sortOrder
        )

        cursor?.use { // Safely use the cursor, close it when done;
            // Get column numbers for each value
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) { // Iterate through the cursor
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val duration = it.getInt(durCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id) // Get content URI for the audio file

                tempPlaylist.add(AudioFile(id, title, contentUri, duration)) // Add audio file to the temporary list
            }
        }

        return tempPlaylist
    }
}