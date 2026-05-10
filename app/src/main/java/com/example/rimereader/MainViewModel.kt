package com.example.rimereader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// View model for the main screen
class MainViewModel(
    private val bookmarkDao: BookmarkDao,
    private val audioRepository: AudioRepository
) : ViewModel() {

    // State variables for the main screen
    var songTitle by mutableStateOf("") // Title of the current file
    var currentArtwork by mutableStateOf<ByteArray?>(null) // Artwork of the current file
    var currentTimeMs by mutableIntStateOf(0) // Current time in milliseconds
    var totalTimeMs by mutableIntStateOf(0) // Total time in milliseconds

    var isPlaying by mutableStateOf(false) // Flag to indicate if the player is playing
    var isLooping by mutableStateOf(false) // Flag to indicate if the player is looping
    var currentSpeed by mutableFloatStateOf(1.0f) // Current playback speed

    var showPlaylistSheet by mutableStateOf(false) // Flag to control the visibility of the playlist bottom sheet dialog
    var showBookmarksSheet by mutableStateOf(false) // Flag to control the visibility of the bookmarks bottom sheet dialog
    var currentBookmarks by mutableStateOf<List<Bookmark>>(emptyList()) // Holds the list of bookmarks fetched from the database for the currently playing song

    var playlist by mutableStateOf<List<AudioFile>>(emptyList()) // List of audio files to play
    var currentSongIndex by mutableIntStateOf(0) // Index of the currently playing song in the playlist

    val speeds = floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f) // Available playback speeds
    var currentSpeedIndex by mutableIntStateOf(0) // Index of the current playback speed in the speeds array


    // Function to refresh the list of bookmarks for the currently playing song
    fun refreshBookmarks(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) { // Launch the process in a background thread
            val dbList = bookmarkDao.getBookmarksForSong(songId) // Fetch bookmarks from the database
            withContext(Dispatchers.Main) { // Update the UI in the main thread
                currentBookmarks = dbList
            }
        }
    }

    // Function to add a bookmark to the database and update the current bookmarks list
    fun addBookmark(songId: Long, timeMs: Int) {
        viewModelScope.launch(Dispatchers.IO) { // Launch the process in a background thread
            bookmarkDao.insert(Bookmark(songId = songId, timeMillis = timeMs, displayTime = TimeUtils.formatTime(timeMs)))
            refreshBookmarks(songId)
        }
    }

    // Function to delete a bookmark from the database and update the current bookmarks list
    fun deleteBookmark(bookmark: Bookmark, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) { // Launch the process in a background thread
            bookmarkDao.delete(bookmark)
            refreshBookmarks(songId)
        }
    }

    fun loadAudioFiles() {
        // ViewModel używa swojego prywatnego audioRepository
        val tempPlaylist = audioRepository.getAudioFiles()
        if (playlist.map { it.id } == tempPlaylist.map { it.id }) return
        playlist = tempPlaylist
    }
}