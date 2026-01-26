package com.example.tts_app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val totalChapters: Int,
    val currentChapterIndex: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis()
)