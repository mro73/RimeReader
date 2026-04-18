package com.example.rimereader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

// DAO (Data Access Object) for Room database
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE songId = :songId ORDER BY timeMillis ASC") // Query to retrieve bookmarks for a specific song
    suspend fun getBookmarksForSong(songId: Long): List<Bookmark> // Suspend function to run on a background thread

    @Insert
    suspend fun insert(bookmark: Bookmark) // Insert a new bookmark

    @Delete
    suspend fun delete(bookmark: Bookmark) // Delete a bookmark
}