package com.example.tts_app.data.remote

import com.example.tts_app.data.local.Chapter
import com.example.tts_app.data.local.Novel

class StubSource : NovelSource {
    override suspend fun searchNovels(query: String): List<Novel> {
        return emptyList()
    }

    override suspend fun getNovelDetails(novelUrl: String): Pair<Novel, List<Chapter>> {
        return Pair(Novel(url = novelUrl, title = "Source Not Available"), emptyList())
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        return "This is a placeholder source"
    }

    override suspend fun getChaptersBatch(novelUrl: String, page: Int): List<Chapter> {
        return emptyList()
    }
}