package com.example.tts_app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM novels WHERE inLibrary = 1 ORDER BY lastAccessed DESC")
    fun getLibraryNovels(): Flow<List<Novel>>

    @Query("SELECT DISTINCT n.* FROM novels n INNER JOIN chapters c ON n.id = c.novelId WHERE n.inLibrary = 1 AND c.isDownloaded = 1 ORDER BY n.lastAccessed DESC")
    fun getDownloadedNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: Int): Novel?

    @Query("SELECT * FROM novels WHERE url = :url LIMIT 1")
    suspend fun getNovelByUrl(url: String): Novel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: Novel): Long

    @Update
    suspend fun updateNovel(novel: Novel)

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovel(id: Int)

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `index` ASC")
    fun getChapters(novelId: Int): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `index` ASC")
    suspend fun getChapterList(novelId: Int): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Int): Chapter?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND `index` = :index LIMIT 1")
    suspend fun getChapterByIndex(novelId: Int, index: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)
}