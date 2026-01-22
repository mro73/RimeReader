package com.example.rimereader

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Bookmark::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}