package com.example.rimereader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
    val progress = if (totalTimeMs > 0) currentTimeMs.toFloat() / totalTimeMs.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                modifier = Modifier.size(40.dp).clickable { onPlaylistClick() }.padding(4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.bookmark),
                contentDescription = "Bookmarks",
                modifier = Modifier.size(40.dp).clickable { onBookmarksClick() }.padding(4.dp)
            )
        }

        Text(
            text = songTitle.ifEmpty { "Brak utworu" },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).basicMarquee()
        )

        Image(
            painter = painterResource(id = R.drawable.album_cover),
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(250.dp)
        )

        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.width(280.dp).padding(top = 24.dp, bottom = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.DarkGray,
                activeTrackColor = Color.DarkGray
            )
        )

        Row(
            modifier = Modifier.width(250.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = TimeUtils.formatTime(currentTimeMs))
            Text(text = TimeUtils.formatTime(totalTimeMs))
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.rewind), "Rewind", Modifier.size(40.dp).clickable { onRewindClick() })
            val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
            Icon(painterResource(id = playPauseIcon), "Play/Pause", Modifier.size(40.dp).clickable { onPlayPauseClick() })
            Icon(painterResource(id = R.drawable.forward), "Forward", Modifier.size(40.dp).clickable { onForwardClick() })
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.stop), "Stop", Modifier.size(40.dp).clickable { onStopClick() })
            val loopTint = if (isLooping) Color.Blue else Color.Black
            Icon(painterResource(id = R.drawable.loop), "Loop", Modifier.size(40.dp).clickable { onLoopClick() }, tint = loopTint)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSpeedClick() }) {
                Icon(painterResource(id = R.drawable.speed), "Speed", Modifier.size(40.dp))
                Text(text = "${currentSpeed}x", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.width(250.dp).padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(painterResource(id = R.drawable.previous), "Previous", Modifier.size(40.dp).clickable { onPrevClick() })
            Icon(painterResource(id = R.drawable.next), "Next", Modifier.size(40.dp).clickable { onNextClick() })
        }
    }
}


@Composable
fun SongItem(song: AudioFile, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = if (isCurrent) Color.Blue else Color.Black
        Icon(
            painter = painterResource(id = R.drawable.play),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.padding(end = 16.dp).size(24.dp)
        )
        Text(
            text = song.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = TimeUtils.formatTime(song.duration),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun BookmarkItem(bookmark: Bookmark, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Czas: ${bookmark.displayTime}",
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Lista utworów",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Zakładki",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onAddBookmark,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(60.dp).padding(bottom = 16.dp)
        ) {
            Icon(painterResource(id = R.drawable.add), contentDescription = "Dodaj", modifier = Modifier.fillMaxSize())
        }

        if (bookmarks.isEmpty()) {
            Text(
                text = "Brak zakładek",
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