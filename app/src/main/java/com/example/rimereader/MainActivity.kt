package com.example.rimereader

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer
    private val playlist = ArrayList<AudioFile>()
    private val speeds = floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f)
    private var currentSpeedIndex = 0
    private var currentSongIndex = 0

    private lateinit var db: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao

    private val handler = Handler(Looper.getMainLooper())

    private var composeSongTitle by mutableStateOf("")
    private var composeCurrentTimeMs by mutableIntStateOf(0)
    private var composeTotalTimeMs by mutableIntStateOf(0)
    private var composeIsPlaying by mutableStateOf(false)
    private var composeIsLooping by mutableStateOf(false)
    private var composeCurrentSpeed by mutableFloatStateOf(1.0f)

    private var showPlaylistSheet by mutableStateOf(false)
    private var showBookmarksSheet by mutableStateOf(false)

    private var currentBookmarks by mutableStateOf<List<Bookmark>>(emptyList())

    private val updateSeekBar: Runnable = object : Runnable {
        override fun run() {
            if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                composeCurrentTimeMs = exoPlayer.currentPosition.toInt()
                handler.postDelayed(this, 200)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) loadAudioFiles() else Toast.makeText(this, "Brak uprawnień", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "audiobook-db")
            .fallbackToDestructiveMigration()
            .build()
        bookmarkDao = db.bookmarkDao()

        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) playNextFile()
                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    handler.removeCallbacks(updateSeekBar)
                    handler.post(updateSeekBar)
                }
                composeIsPlaying = exoPlayer.isPlaying
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(this@MainActivity, "Błąd odtwarzania", Toast.LENGTH_SHORT).show()
            }
        })

        setContent {
            PlayerScreen(
                songTitle = composeSongTitle,
                currentTimeMs = composeCurrentTimeMs,
                totalTimeMs = composeTotalTimeMs,
                isPlaying = composeIsPlaying,
                isLooping = composeIsLooping,
                currentSpeed = composeCurrentSpeed,
                onPlayPauseClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause()
                    else {
                        exoPlayer.play()
                        applySpeed()
                        handler.post(updateSeekBar)
                    }
                    composeIsPlaying = exoPlayer.isPlaying
                },
                onStopClick = {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    composeIsPlaying = false
                    composeCurrentTimeMs = 0
                    playTrack(currentSongIndex, autoStart = false)
                },
                onRewindClick = {
                    val newPosition = exoPlayer.currentPosition - 15000
                    exoPlayer.seekTo(if (newPosition > 0) newPosition else 0)
                    composeCurrentTimeMs = exoPlayer.currentPosition.toInt()
                },
                onForwardClick = {
                    val newPosition = exoPlayer.currentPosition + 30000
                    val duration = exoPlayer.duration
                    exoPlayer.seekTo(if (newPosition < duration) newPosition else duration)
                    composeCurrentTimeMs = exoPlayer.currentPosition.toInt()
                },
                onPrevClick = {
                    if (playlist.isNotEmpty()) {
                        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
                        playTrack(currentSongIndex, autoStart = true)
                    }
                },
                onNextClick = { playNextFile() },
                onLoopClick = {
                    composeIsLooping = !composeIsLooping
                    exoPlayer.repeatMode = if (composeIsLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                },
                onSpeedClick = {
                    currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
                    composeCurrentSpeed = speeds[currentSpeedIndex]
                    applySpeed()
                },
                onSeek = { progressFraction ->
                    val targetMs = (progressFraction * composeTotalTimeMs).toLong()
                    exoPlayer.seekTo(targetMs)
                    composeCurrentTimeMs = targetMs.toInt()
                },
                onPlaylistClick = { showPlaylistSheet = true },
                onBookmarksClick = {
                    if (playlist.isNotEmpty()) {
                        refreshBookmarks()
                        showBookmarksSheet = true
                    }
                }
            )

            if (showPlaylistSheet) {
                PlaylistBottomSheet(
                    playlist = playlist,
                    currentIndex = currentSongIndex,
                    onDismiss = { showPlaylistSheet = false },
                    onSongClick = { clickedIndex ->
                        currentSongIndex = clickedIndex
                        playTrack(currentSongIndex, autoStart = true)
                        showPlaylistSheet = false
                    }
                )
            }

            if (showBookmarksSheet) {
                BookmarksBottomSheet(
                    bookmarks = currentBookmarks,
                    onDismiss = { showBookmarksSheet = false },
                    onAddBookmark = {
                        val currentMs = exoPlayer.currentPosition.toInt()
                        val currentFile = playlist[currentSongIndex]
                        lifecycleScope.launch(Dispatchers.IO) {
                            bookmarkDao.insert(Bookmark(songId = currentFile.id, timeMillis = currentMs, displayTime = TimeUtils.formatTime(currentMs)))
                            refreshBookmarks()
                        }
                    },
                    onBookmarkClick = { bookmark ->
                        exoPlayer.seekTo(bookmark.timeMillis.toLong())
                        composeCurrentTimeMs = bookmark.timeMillis
                        showBookmarksSheet = false
                    },
                    onDeleteBookmark = { bookmark ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            bookmarkDao.delete(bookmark)
                            refreshBookmarks()
                        }
                    }
                )
            }
        }
    }

    private fun playNextFile() {
        if (playlist.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % playlist.size
            playTrack(currentSongIndex, autoStart = true)
        }
    }

    private fun refreshBookmarks() {
        if (playlist.isEmpty()) return
        val currentFileId = playlist[currentSongIndex].id
        lifecycleScope.launch(Dispatchers.IO) {
            val dbList = bookmarkDao.getBookmarksForSong(currentFileId)
            withContext(Dispatchers.Main) {
                currentBookmarks = dbList // Aktualizacja stanu spowoduje odświeżenie okna
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) loadAudioFiles()
        else requestPermissionLauncher.launch(permission)
    }

    private fun loadAudioFiles() {
        val tempPlaylist = ArrayList<AudioFile>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                tempPlaylist.add(AudioFile(id, it.getString(titleCol), ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id), it.getInt(durCol)))
            }
        }

        if (playlist.map { it.id } == tempPlaylist.map { it.id }) return
        playlist.clear()
        playlist.addAll(tempPlaylist)

        if (playlist.isEmpty()) Toast.makeText(this, "Brak plików", Toast.LENGTH_LONG).show()
        else playTrack(0, autoStart = false)
    }

    private fun playTrack(index: Int, autoStart: Boolean) {
        if (playlist.isEmpty()) return
        val currentFile = playlist[index]
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(currentFile.uri))
            exoPlayer.prepare()

            composeSongTitle = currentFile.title
            composeTotalTimeMs = currentFile.duration
            composeCurrentTimeMs = 0

            exoPlayer.repeatMode = if (composeIsLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            if (autoStart) {
                exoPlayer.play()
                applySpeed()
                handler.post(updateSeekBar)
            } else {
                exoPlayer.pause()
                exoPlayer.seekTo(0)
            }
            composeIsPlaying = exoPlayer.isPlaying

            refreshBookmarks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySpeed() {
        exoPlayer.playbackParameters = PlaybackParameters(speeds[currentSpeedIndex])
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        exoPlayer.release()
    }
}