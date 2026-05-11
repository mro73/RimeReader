package com.example.rimereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "audiobook-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = AudioRepository(applicationContext)
        MainViewModelFactory(db.bookmarkDao(), repository)
    }

    private lateinit var exoPlayer: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    private val updateSeekBar: Runnable = object : Runnable {
        override fun run() {
            if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                viewModel.currentTimeMs = exoPlayer.currentPosition.toInt()
                handler.postDelayed(this, 200)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) loadFilesAndStart() else Toast.makeText(this, "Brak uprawnień", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) playNextFile()
                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    handler.removeCallbacks(updateSeekBar)
                    handler.post(updateSeekBar)
                }
                viewModel.isPlaying = exoPlayer.isPlaying
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(this@MainActivity, "Błąd odtwarzania", Toast.LENGTH_SHORT).show()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                viewModel.currentArtwork = mediaMetadata.artworkData
            }
        })

        setContent {
            PlayerScreen(
                songTitle = viewModel.songTitle,
                currentTimeMs = viewModel.currentTimeMs,
                totalTimeMs = viewModel.totalTimeMs,
                isPlaying = viewModel.isPlaying,
                isLooping = viewModel.isLooping,
                currentSpeed = viewModel.currentSpeed,
                currentArtwork = viewModel.currentArtwork,
                onPlayPauseClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause()
                    else {
                        exoPlayer.play()
                        applySpeed()
                        handler.post(updateSeekBar)
                    }
                    viewModel.isPlaying = exoPlayer.isPlaying
                },
                onStopClick = {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    viewModel.isPlaying = false
                    viewModel.currentTimeMs = 0
                },
                onRewindClick = {
                    val newPosition = exoPlayer.currentPosition - 15000
                    exoPlayer.seekTo(if (newPosition > 0) newPosition else 0)
                    viewModel.currentTimeMs = exoPlayer.currentPosition.toInt()
                },
                onForwardClick = {
                    val newPosition = exoPlayer.currentPosition + 30000
                    val duration = exoPlayer.duration
                    exoPlayer.seekTo(if (newPosition < duration) newPosition else duration)
                    viewModel.currentTimeMs = exoPlayer.currentPosition.toInt()
                },
                onPrevClick = {
                    if (viewModel.playlist.isNotEmpty()) {
                        viewModel.currentSongIndex = if (viewModel.currentSongIndex > 0) viewModel.currentSongIndex - 1 else viewModel.playlist.size - 1
                        playTrack(viewModel.currentSongIndex, autoStart = true)
                    }
                },
                onNextClick = { playNextFile() },
                onLoopClick = {
                    viewModel.isLooping = !viewModel.isLooping
                    exoPlayer.repeatMode = if (viewModel.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                },
                onSpeedClick = {
                    viewModel.currentSpeedIndex = (viewModel.currentSpeedIndex + 1) % viewModel.speeds.size
                    viewModel.currentSpeed = viewModel.speeds[viewModel.currentSpeedIndex]
                    applySpeed()
                },
                onSeek = { progressFraction ->
                    val targetMs = (progressFraction * viewModel.totalTimeMs).toLong()
                    exoPlayer.seekTo(targetMs)
                    viewModel.currentTimeMs = targetMs.toInt()
                },
                onPlaylistClick = { viewModel.showPlaylistSheet = true },
                onBookmarksClick = {
                    if (viewModel.playlist.isNotEmpty()) {
                        viewModel.refreshBookmarks(viewModel.playlist[viewModel.currentSongIndex].id)
                        viewModel.showBookmarksSheet = true
                    }
                }
            )

            if (viewModel.showPlaylistSheet) {
                PlaylistBottomSheet(
                    playlist = viewModel.playlist,
                    currentIndex = viewModel.currentSongIndex,
                    onDismiss = { viewModel.showPlaylistSheet = false },
                    onSongClick = { clickedIndex ->
                        viewModel.currentSongIndex = clickedIndex
                        playTrack(viewModel.currentSongIndex, autoStart = true)
                        viewModel.showPlaylistSheet = false
                    }
                )
            }

            if (viewModel.showBookmarksSheet) {
                BookmarksBottomSheet(
                    bookmarks = viewModel.currentBookmarks,
                    onDismiss = { viewModel.showBookmarksSheet = false },
                    onAddBookmark = {
                        val currentMs = exoPlayer.currentPosition.toInt()
                        val currentFile = viewModel.playlist[viewModel.currentSongIndex]
                        viewModel.addBookmark(currentFile.id, currentMs)
                    },
                    onBookmarkClick = { bookmark ->
                        exoPlayer.seekTo(bookmark.timeMillis.toLong())
                        viewModel.currentTimeMs = bookmark.timeMillis
                        viewModel.showBookmarksSheet = false
                    },
                    onDeleteBookmark = { bookmark ->
                        val currentFile = viewModel.playlist[viewModel.currentSongIndex]
                        viewModel.deleteBookmark(bookmark, currentFile.id)
                    }
                )
            }
        }
    }

    private fun playNextFile() {
        if (viewModel.playlist.isNotEmpty()) {
            viewModel.currentSongIndex = (viewModel.currentSongIndex + 1) % viewModel.playlist.size
            playTrack(viewModel.currentSongIndex, autoStart = true)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) loadFilesAndStart()
        else requestPermissionLauncher.launch(permission)
    }

    private fun loadFilesAndStart() {
        val wasEmpty = viewModel.playlist.isEmpty()
        viewModel.loadAudioFiles()

        if (viewModel.playlist.isEmpty()) {
            Toast.makeText(this, "Brak plików", Toast.LENGTH_LONG).show()
        } else if (wasEmpty) {
            playTrack(viewModel.currentSongIndex, autoStart = false)
        }
    }

    private fun playTrack(index: Int, autoStart: Boolean) {
        if (viewModel.playlist.isEmpty()) return
        val currentFile = viewModel.playlist[index]
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(currentFile.uri))
            exoPlayer.prepare()

            viewModel.songTitle = currentFile.title
            viewModel.totalTimeMs = currentFile.duration
            viewModel.currentTimeMs = 0

            exoPlayer.repeatMode = if (viewModel.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            if (autoStart) {
                exoPlayer.play()
                applySpeed()
                handler.post(updateSeekBar)
            } else {
                exoPlayer.pause()
                exoPlayer.seekTo(0)
            }
            viewModel.isPlaying = exoPlayer.isPlaying

            viewModel.refreshBookmarks(currentFile.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySpeed() {
        exoPlayer.playbackParameters = PlaybackParameters(viewModel.speeds[viewModel.currentSpeedIndex])
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        exoPlayer.release()
    }
}