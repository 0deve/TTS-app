package com.example.tts_app.data.remote

import com.example.tts_app.data.local.Chapter
import com.example.tts_app.data.local.Novel

interface NovelSource {
    suspend fun searchNovels(query: String): List<Novel>
    suspend fun getNovelDetails(novelUrl: String): Pair<Novel, List<Chapter>>
    suspend fun getChapterContent(chapterUrl: String): String
}