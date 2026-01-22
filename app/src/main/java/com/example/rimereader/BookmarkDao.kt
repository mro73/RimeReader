package com.example.rimereader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY timeMillis ASC")
    suspend fun getBookmarksForSong(songId: Long): List<Bookmark>

    @Insert
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)
}