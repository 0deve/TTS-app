package com.example.tts_app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class Novel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val coverUrl: String = "",
    val author: String = "",
    val summary: String = "",
    val inLibrary: Boolean = false,
    val totalChapters: Int = 0,
    val currentChapterIndex: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = Novel::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId")]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val novelId: Int,
    val index: Int,
    val title: String,
    val url: String,
    val content: String = "",
    val isDownloaded: Boolean = false
)