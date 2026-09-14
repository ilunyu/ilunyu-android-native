package com.ilunyu.lunyu.data.repository

import android.content.Context
import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.EditionIndex
import com.ilunyu.lunyu.data.model.EditionSummary
import com.ilunyu.lunyu.data.model.Pian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AnalectsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedLibrary: AnalectsLibrary? = null
    private var cachedEditions: List<EditionSummary> = emptyList()

    suspend fun getEditions(): List<EditionSummary> = withContext(Dispatchers.IO) {
        if (cachedEditions.isNotEmpty()) return@withContext cachedEditions
        try {
            val content = context.assets.open("content/editions/index.json").bufferedReader().use { it.readText() }
            val index = json.decodeFromString<EditionIndex>(content)
            cachedEditions = index.editions
            cachedEditions
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLibrary(): AnalectsLibrary = withContext(Dispatchers.IO) {
        cachedLibrary?.let { return@withContext it }
        val content = context.assets.open("content/analects.json").bufferedReader().use { it.readText() }
        val library = json.decodeFromString<AnalectsLibrary>(content)
        cachedLibrary = library
        library
    }

    suspend fun getPianBySlug(slug: String): Pian? {
        val library = getLibrary()
        return library.pians.find { it.slug == slug }
    }

    suspend fun getChapter(pianSlug: String, chapterNumber: Int): Pair<Pian, Chapter>? = withContext(Dispatchers.IO) {
        val pian = getPianBySlug(pianSlug) ?: return@withContext null
        val chapterSummary = pian.chapters.find { it.number == chapterNumber } ?: return@withContext null
        try {
            val detailPath = "content/editions/yangbojun-chapter-${chapterSummary.id}.json"
            val detailContent = context.assets.open(detailPath).bufferedReader().use { it.readText() }
            val detailedChapter = json.decodeFromString<Chapter>(detailContent)
            pian to detailedChapter
        } catch (_: Exception) {
            pian to chapterSummary
        }
    }

    suspend fun getChapterById(chapterId: String): Pair<Pian, Chapter>? = withContext(Dispatchers.IO) {
        val library = getLibrary()
        for (pian in library.pians) {
            val chapterSummary = pian.chapters.find { it.id == chapterId }
            if (chapterSummary != null) {
                return@withContext try {
                    val detailPath = "content/editions/yangbojun-chapter-${chapterSummary.id}.json"
                    val detailContent = context.assets.open(detailPath).bufferedReader().use { it.readText() }
                    val detailedChapter = json.decodeFromString<Chapter>(detailContent)
                    pian to detailedChapter
                } catch (_: Exception) {
                    pian to chapterSummary
                }
            }
        }
        null
    }

    suspend fun getAdjacentChapters(pianSlug: String, chapterNumber: Int): Pair<Pair<Pian, Chapter>?, Pair<Pian, Chapter>?> {
        val library = getLibrary()
        val allChapters = mutableListOf<Pair<Pian, Chapter>>()
        for (pian in library.pians) {
            for (ch in pian.chapters) {
                allChapters.add(pian to ch)
            }
        }
        val currentIndex = allChapters.indexOfFirst { it.first.slug == pianSlug && it.second.number == chapterNumber }
        if (currentIndex == -1) return null to null
        val prev = if (currentIndex > 0) allChapters[currentIndex - 1] else null
        val next = if (currentIndex < allChapters.size - 1) allChapters[currentIndex + 1] else null
        return prev to next
    }

    suspend fun search(query: String): List<Pair<Pian, Chapter>> {
        if (query.isBlank()) return emptyList()
        val library = getLibrary()
        val q = query.trim()
        val results = mutableListOf<Pair<Pian, Chapter>>()
        for (pian in library.pians) {
            for (chapter in pian.chapters) {
                if (chapter.plainText.contains(q, ignoreCase = true) ||
                    chapter.text.contains(q, ignoreCase = true) ||
                    chapter.translation.contains(q, ignoreCase = true) ||
                    chapter.displayId.contains(q, ignoreCase = true)
                ) {
                    results.add(pian to chapter)
                }
            }
        }
        return results
    }
}
