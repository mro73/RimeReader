package com.example.rimereader

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

// Dla dokładniejszego czasu
//import androidx.media3.extractor.DefaultExtractorsFactory
//import androidx.media3.extractor.mp3.Mp3Extractor
//import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

class MainActivity : AppCompatActivity() {
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var textSongTitle: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var buttonPlay: ImageView
    private lateinit var buttonStop: ImageView
    private lateinit var buttonRewind: ImageView
    private lateinit var buttonForward: ImageView
    private lateinit var buttonSpeed: ImageView
    private lateinit var buttonLoop: ImageView
    private var isLooping = false
    private lateinit var buttonPrevFile: ImageView
    private lateinit var buttonNextFile: ImageView
    private val playlist = ArrayList<AudioFile>()
    private val speeds = floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f)
    private var currentSpeedIndex = 0
    private var currentSongIndex = 0

    private lateinit var buttonBookmarks: ImageView
    private lateinit var buttonPlaylist: ImageView

    private lateinit var db: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao

    private val handler = Handler(Looper.getMainLooper())

    private val updateSeekBar: Runnable = object : Runnable {
        override fun run() {
            if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration.coerceAtLeast(1)

                seekBar.max = duration.toInt()
                seekBar.progress = currentPos.toInt()
                textCurrentTime.text = formatTime(currentPos.toInt())

                handler.postDelayed(this, 200)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult( // Uprawnienia
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadAudioFiles()
        } else {
            Toast.makeText(this, "Brak uprawnień do odczytywania plików", Toast.LENGTH_LONG).show()
        }
    }

    // Przyciski
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "audiobook-db")
            .fallbackToDestructiveMigration()
            .build()
        bookmarkDao = db.bookmarkDao()

        initViews()

        exoPlayer = ExoPlayer.Builder(this).build()
        // Dla dokładniejszego czasu: Zamienić powyższą 1 linijkę na te poniżej
        //val extractorsFactory = DefaultExtractorsFactory()
        //    .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        //val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
        //exoPlayer = ExoPlayer.Builder(this)
        //    .setMediaSourceFactory(mediaSourceFactory)
        //    .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    buttonNextFile.performClick()
                }
                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    handler.removeCallbacks(updateSeekBar)
                    handler.post(updateSeekBar)
                }

                updatePlayPauseIcon()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(this@MainActivity, "Błąd odtwarzania", Toast.LENGTH_SHORT).show()
            }
        })

        buttonPlay.setOnClickListener {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
                applySpeed()
                handler.post(updateSeekBar)
            }
            updatePlayPauseIcon()
        }

        buttonStop.setOnClickListener {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            updatePlayPauseIcon()

            playTrack(currentSongIndex, autoStart = false) // od początku
        }

        buttonRewind.setOnClickListener {
            val newPosition = exoPlayer.currentPosition - 15000
            exoPlayer.seekTo(if (newPosition > 0) newPosition else 0) // bez ujemnych

            textCurrentTime.text = formatTime(exoPlayer.currentPosition.toInt())
            seekBar.progress = newPosition.toInt()
        }

        buttonForward.setOnClickListener {
            val newPosition = exoPlayer.currentPosition + 30000
            val duration = exoPlayer.duration
            exoPlayer.seekTo(if (newPosition < duration) newPosition else duration) // nie więcej niż długość

            textCurrentTime.text = formatTime(exoPlayer.currentPosition.toInt())
            seekBar.progress = newPosition.toInt()
        }

        buttonSpeed.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
            applySpeed()
            Toast.makeText(this, "Prędkość odtwarzania: ${speeds[currentSpeedIndex]}x", Toast.LENGTH_SHORT).show()
        }

        buttonPrevFile.setOnClickListener {
            if (playlist.isNotEmpty()) {
                currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
                playTrack(currentSongIndex, autoStart = true)
            }
        }

        buttonNextFile.setOnClickListener {
            if (playlist.isNotEmpty()) {
                currentSongIndex = (currentSongIndex + 1) % playlist.size
                playTrack(currentSongIndex, autoStart = true)
            }
        }

        buttonLoop.setOnClickListener {
            isLooping = !isLooping
            exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            if (isLooping) {
                buttonLoop.setColorFilter(android.graphics.Color.BLUE)
                Toast.makeText(this, "Powtarzanie włączone", Toast.LENGTH_SHORT).show()
            } else {
                buttonLoop.clearColorFilter()
                Toast.makeText(this, "Powtarzanie wyłączone", Toast.LENGTH_SHORT).show()
            }
        }

        buttonBookmarks = findViewById(R.id.buttonBookmarks)
        buttonBookmarks.setOnClickListener { showBookmarksDialog() }

        buttonPlaylist = findViewById(R.id.buttonPlaylist)
        buttonPlaylist.setOnClickListener { showPlaylistDialog() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    textCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(updateSeekBar)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    exoPlayer.seekTo(seekBar.progress.toLong())
                    if (exoPlayer.playWhenReady) {
                        handler.post(updateSeekBar)
                    }
                }
            }
        })
    }

    override fun onResume() { // Sprawdzanie nowych plików przy otwarciu aplikacji
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun loadAudioFiles() {
        val tempPlaylist = ArrayList<AudioFile>() // Tymczasowa lista

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val duration = it.getInt(durationColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                tempPlaylist.add(AudioFile(id, title, contentUri, duration))
            }
        }

        val oldIds = playlist.map { it.id }
        val newIds = tempPlaylist.map { it.id }

        if (oldIds == newIds) { // Jeśli nie ma nowych plików
            return
        }

        playlist.clear()
        playlist.addAll(tempPlaylist)

        if (playlist.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono żadnych plików audio", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Odświeżono listę utworów", Toast.LENGTH_SHORT).show()

            playTrack(0, autoStart = false) // Wczytanie pliku 0 (pierwszego z listy)
        }
    }

    private fun playTrack(index: Int, autoStart: Boolean) { // Odtwarzanie
        if (playlist.isEmpty()) return

        val currentFile = playlist[index]

        try {
            val mediaItem = MediaItem.fromUri(currentFile.uri)

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            textSongTitle.text = currentFile.title

            textTotalTime.text = formatTime(currentFile.duration)
            textCurrentTime.text = "0:00"
            seekBar.progress = 0
            seekBar.max = currentFile.duration

            exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            loadAlbumArt(currentFile.uri)

            if (autoStart) { // Przy next/previous
                exoPlayer.play()
                applySpeed()
                handler.post(updateSeekBar)
            } else {
                exoPlayer.pause()
                exoPlayer.seekTo(0)
            }
            updatePlayPauseIcon()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Błąd wczytywania pliku", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySpeed() {
        val params = PlaybackParameters(speeds[currentSpeedIndex])
        exoPlayer.playbackParameters = params
    }

    private fun loadAlbumArt(uri: Uri) { // Okładki
        val retriever = android.media.MediaMetadataRetriever()
        val imageView = findViewById<ImageView>(R.id.imageView)
        try {
            retriever.setDataSource(this, uri)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.album_cover) // Domyślna
            }
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.album_cover)
        } finally {
            retriever.release()
        }
    }

    private fun initViews() {
        textSongTitle = findViewById(R.id.textSongTitle)
        textSongTitle.isSelected = true
        seekBar = findViewById(R.id.seekBar)
        textCurrentTime = findViewById(R.id.textCurrentTime)
        textTotalTime = findViewById(R.id.textTotalTime)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonStop = findViewById(R.id.buttonStop)
        buttonRewind = findViewById(R.id.buttonRewind)
        buttonForward = findViewById(R.id.buttonForward)
        buttonSpeed = findViewById(R.id.buttonSpeed)
        buttonPrevFile = findViewById(R.id.buttonPrevFile)
        buttonNextFile = findViewById(R.id.buttonNextFile)
        buttonPlaylist = findViewById(R.id.buttonPlaylist)
        buttonLoop = findViewById(R.id.buttonLoop)
    }

    private fun formatTime(milliseconds: Int): String { // Zamiana czasu
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds.toLong())
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun showBookmarksDialog() { // Zakładki
        if(playlist.isEmpty()) return

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_bookmarks, null)
        dialog.setContentView(view)

        val btnAdd = view.findViewById<ImageView>(R.id.btnAddBookmark)
        val container = view.findViewById<android.widget.LinearLayout>(R.id.bookmarksContainer)

        val currentFile = playlist[currentSongIndex]

        fun refreshBookmarksView() {
            lifecycleScope.launch(Dispatchers.IO) {
                val dbList = bookmarkDao.getBookmarksForSong(currentFile.id)

                withContext(Dispatchers.Main) {
                    container.removeAllViews()
                    for (bookmark in dbList) {
                        val itemView = layoutInflater.inflate(R.layout.item_bookmark, null)
                        val textTime = itemView.findViewById<TextView>(R.id.textBookmarkTime)
                        val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteBookmark)

                        textTime.text = "Czas: ${bookmark.displayTime}"

                        itemView.setOnClickListener { // Kliknięcie w zakładkę
                            exoPlayer.seekTo(bookmark.timeMillis.toLong())
                            textCurrentTime.text = formatTime(bookmark.timeMillis)
                            dialog.dismiss()
                        }
                        btnDelete.setOnClickListener {
                            lifecycleScope.launch(Dispatchers.IO) {
                                bookmarkDao.delete(bookmark)
                                withContext(Dispatchers.Main) { refreshBookmarksView() }
                            }
                        }
                        container.addView(itemView)
                    }
                    if (dbList.isEmpty()) {
                        val emptyInfo = TextView(this@MainActivity)
                        emptyInfo.text = "Brak zakładek"
                        emptyInfo.gravity = android.view.Gravity.CENTER
                        emptyInfo.setPadding(0, 50, 0, 0)
                        container.addView(emptyInfo)
                    }
                }
            }
        }

        btnAdd.setOnClickListener { // Nowa zakładka
            val currentMs = exoPlayer.currentPosition.toInt()
            val display = formatTime(currentMs)
            val newBookmark = Bookmark(songId = currentFile.id, timeMillis = currentMs, displayTime = display)

            lifecycleScope.launch(Dispatchers.IO) {
                bookmarkDao.insert(newBookmark)
                withContext(Dispatchers.Main) { refreshBookmarksView() }
            }
        }
        refreshBookmarksView()
        dialog.show()
    }

    private fun updatePlayPauseIcon() {
        if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
            buttonPlay.setImageResource(R.drawable.pause)
        } else {
            buttonPlay.setImageResource(R.drawable.play)
        }
    }

    private fun showPlaylistDialog() { // Lista plików
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_playlist, null)
        dialog.setContentView(view)

        val container = view.findViewById<android.widget.LinearLayout>(R.id.playlistContainer)

        for (i in playlist.indices) {
            val audioFile = playlist[i]

            val itemView = layoutInflater.inflate(R.layout.item_song, null)
            val textName = itemView.findViewById<TextView>(R.id.textSongName)
            val textDuration = itemView.findViewById<TextView>(R.id.textSongDuration)
            val icon = itemView.findViewById<ImageView>(R.id.iconNote)

            textName.text = audioFile.title
            textDuration.text = formatTime(audioFile.duration)

            if (i == currentSongIndex) { // Dla aktualnego utworu
                textName.setTypeface(null, android.graphics.Typeface.BOLD)
                icon.setColorFilter(android.graphics.Color.BLUE)
            }

            itemView.setOnClickListener {
                currentSongIndex = i
                playTrack(currentSongIndex, autoStart = true)
                dialog.dismiss()
            }
            container.addView(itemView)
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        exoPlayer.release()
    }
}

data class AudioFile(
    val id: Long,
    val title: String,
    val uri: android.net.Uri,
    val duration: Int
)