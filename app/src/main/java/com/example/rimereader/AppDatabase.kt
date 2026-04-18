package com.example.rimereader

import androidx.room.Database
import androidx.room.RoomDatabase

// Database class for Room
@Database(entities = [Bookmark::class], version = 1) // Define the entities and version of the database
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao // Abstract method to get the DAO for bookmarks
}