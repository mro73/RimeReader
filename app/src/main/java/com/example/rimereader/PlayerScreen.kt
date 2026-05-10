package com.example.rimereader

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    songTitle: String,
    currentTimeMs: Int,
    totalTimeMs: Int,
    isPlaying: Boolean,
    isLooping: Boolean,
    currentSpeed: Float,
    currentArtwork: ByteArray?,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onLoopClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onPlaylistClick: () -> Unit,
    onBookmarksClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color.DarkGray
    val iconColor = if (isDark) Color(0xFFE0E0E0) else Color.Black
    val activeColor = if (isDark) Color(0xFF90CAF9) else Color.Blue
    val sliderInactive = if (isDark) Color.DarkGray else Color.LightGray

    val progress = if (totalTimeMs > 0) currentTimeMs.toFloat() / totalTimeMs.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = "Playlist",
                tint = iconColor,
                modifier = Modifier.size(40.dp).clickable { onPlaylistClick() }.padding(4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.bookmark),
                contentDescription = "Bookmarks",
                tint = iconColor,
                modifier = Modifier.size(40.dp).clickable { onBookmarksClick() }.padding(4.dp)
            )
        }

        Text(
            text = songTitle.ifEmpty { "Brak utworu" },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().basicMarquee()
        )

        val bitmap = remember(currentArtwork) {
            currentArtwork?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(250.dp),
            contentAlignment = Alignment.Center // Zawsze centruje zawartość
        ) {
            if (bitmap != null) {
                // Prawdziwa okładka wypełnia całe 250.dp
                Image(
                    bitmap = bitmap,
                    contentDescription = "Okładka z pliku",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Nutka ma 150.dp, ale siedzi grzecznie na środku 250-pikselowego pudełka
                Icon(
                    painter = painterResource(id = R.drawable.ic_default_cover),
                    contentDescription = "Domyślna okładka",
                    tint = iconColor,
                    modifier = Modifier.size(150.dp)
                )
            }
        }

        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.width(280.dp).padding(bottom = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = textColor,
                inactiveTrackColor = sliderInactive
            )
        )

        Row(
            modifier = Modifier.width(250.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = TimeUtils.formatTime(currentTimeMs), color = textColor)
            Text(text = TimeUtils.formatTime(totalTimeMs), color = textColor)
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.rewind), "Rewind", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onRewindClick() })
            val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
            Icon(painterResource(id = playPauseIcon), "Play/Pause", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onPlayPauseClick() })
            Icon(painterResource(id = R.drawable.forward), "Forward", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onForwardClick() })
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.stop), "Stop", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onStopClick() })
            val loopTint = if (isLooping) activeColor else iconColor
            Icon(painterResource(id = R.drawable.loop), "Loop", tint = loopTint, modifier = Modifier.size(40.dp).clickable { onLoopClick() })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSpeedClick() }) {
                Icon(painterResource(id = R.drawable.speed), "Speed", tint = iconColor, modifier = Modifier.size(40.dp))
                Text(text = "${currentSpeed}x", fontSize = 12.sp, color = textColor)
            }
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.previous), "Previous", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onPrevClick() })
            Icon(painterResource(id = R.drawable.next), "Next", tint = iconColor, modifier = Modifier.size(40.dp).clickable { onNextClick() })
        }
    }
}


@Composable
fun SongItem(song: AudioFile, isCurrent: Boolean, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color.DarkGray
    val iconColor = if (isDark) Color(0xFFE0E0E0) else Color.Black
    val activeColor = if (isDark) Color(0xFF90CAF9) else Color.Blue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = if (isCurrent) activeColor else iconColor
        Icon(
            painter = painterResource(id = R.drawable.play),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.padding(end = 16.dp).size(24.dp)
        )
        Text(
            text = song.title,
            color = textColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = TimeUtils.formatTime(song.duration),
            color = textColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun BookmarkItem(bookmark: Bookmark, onClick: () -> Unit, onDelete: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color.DarkGray
    val iconColor = if (isDark) Color(0xFFE0E0E0) else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Czas: ${bookmark.displayTime}",
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                tint = iconColor,
                contentDescription = "Usuń"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    playlist: List<AudioFile>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color.DarkGray

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor
    ) {
        Text(
            text = "Lista utworów",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(playlist) { index, song ->
                SongItem(
                    song = song,
                    isCurrent = index == currentIndex,
                    onClick = { onSongClick(index) }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onAddBookmark: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color.DarkGray
    val iconColor = if (isDark) Color(0xFFE0E0E0) else Color.Black

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor
    ) {
        Text(
            text = "Zakładki",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onAddBookmark,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(60.dp).padding(bottom = 16.dp)
        ) {
            Icon(
                painterResource(id = R.drawable.add),
                contentDescription = "Dodaj",
                tint = iconColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (bookmarks.isEmpty()) {
            Text(
                text = "Brak zakładek",
                color = textColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(bookmarks) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark) },
                        onDelete = { onDeleteBookmark(bookmark) }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}