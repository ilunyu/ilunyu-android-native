package com.ilunyu.lunyu.data.repository

import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.EditionSummary
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.data.resource.ResourceLocationType
import com.ilunyu.lunyu.data.resource.ResourcePackageStorage
import com.ilunyu.lunyu.data.resource.ResourceRepository
import com.ilunyu.lunyu.data.resource.ResolvedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class AnalectsRepository(
    private val storage: ResourcePackageStorage,
    private val resourceRepository: ResourceRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedGeneration = Long.MIN_VALUE
    private var cachedLibrary: AnalectsLibrary? = null
    private val detailedChapterCache = ConcurrentHashMap<String, Chapter>()

    suspend fun getEditions(): List<EditionSummary> = withContext(Dispatchers.IO) {
        val active = activePackage() ?: return@withContext emptyList()
        listOf(
            EditionSummary(
                id = active.packageId.removePrefix("edition."),
                name = active.name,
                shortName = active.name,
            )
        )
    }

    suspend fun getLibrary(): AnalectsLibrary = withContext(Dispatchers.IO) {
        val active = activePackage() ?: return@withContext AnalectsLibrary()
        synchronizeGeneration()
        cachedLibrary?.let { return@withContext it }
        val path = if (active.locationType == ResourceLocationType.BUNDLED) {
            "content/analects.json"
        } else {
            "content/catalog.json"
        }
        val catalog = storage.open(active, path).bufferedReader().use { reader ->
            json.decodeFromString<AnalectsLibrary>(reader.readText())
        }
        // Downloaded catalogues contain only enough data to establish the
        // book structure. The list screens need each chapter's preview text,
        // which is kept in a lightweight per-pian file rather than the full
        // annotated chapter payload.
        val library = if (active.locationType == ResourceLocationType.BUNDLED) {
            catalog
        } else {
            catalog.copy(
                pians = catalog.pians.map { pian -> loadPianPreview(active, pian) }
            )
        }
        cachedLibrary = library
        library
    }

    suspend fun getPianBySlug(slug: String): Pian? {
        val active = activePackage() ?: return null
        val library = getLibrary()
        val summary = library.pians.find { it.slug == slug } ?: return null
        return summary
    }

    suspend fun getChapter(pianSlug: String, chapterNumber: Int): Pair<Pian, Chapter>? = withContext(Dispatchers.IO) {
        val active = activePackage() ?: return@withContext null
        val pian = getPianBySlug(pianSlug) ?: return@withContext null
        val chapterSummary = pian.chapters.find { it.number == chapterNumber } ?: return@withContext null
        val key = "${active.packageId}:${active.versionCode}:${chapterSummary.id}"
        detailedChapterCache[key]?.let { return@withContext pian to it }
        val detail = loadChapterDetail(active, chapterSummary)
        detailedChapterCache[key] = detail
        pian to detail
    }

    suspend fun getChapterById(chapterId: String): Pair<Pian, Chapter>? = withContext(Dispatchers.IO) {
        val active = activePackage() ?: return@withContext null
        val library = getLibrary()
        val pian = library.pians.firstOrNull { candidate -> candidate.chapters.any { it.id == chapterId } }
            ?: return@withContext null
        val summary = pian.chapters.first { it.id == chapterId }
        val key = "${active.packageId}:${active.versionCode}:${chapterId}"
        detailedChapterCache[key]?.let { return@withContext pian to it }
        val detail = loadChapterDetail(active, summary)
        detailedChapterCache[key] = detail
        pian to detail
    }

    suspend fun getAdjacentChapters(pianSlug: String, chapterNumber: Int): Pair<Pair<Pian, Chapter>?, Pair<Pian, Chapter>?> {
        val library = getLibrary()
        val allChapters = library.pians.flatMap { pian -> pian.chapters.map { chapter -> pian to chapter } }
        val currentIndex = allChapters.indexOfFirst { it.first.slug == pianSlug && it.second.number == chapterNumber }
        if (currentIndex == -1) return null to null
        val prev = if (currentIndex > 0) allChapters[currentIndex - 1] else null
        val next = if (currentIndex < allChapters.lastIndex) allChapters[currentIndex + 1] else null
        return prev to next
    }

    suspend fun search(query: String): List<Pair<Pian, Chapter>> {
        if (query.isBlank()) return emptyList()
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val active = activePackage() ?: return emptyList()
        val library = if (active.locationType == ResourceLocationType.BUNDLED) {
            getLibrary()
        } else {
            runCatching {
                storage.open(active, "content/search.json").bufferedReader().use { reader ->
                    json.decodeFromString<AnalectsLibrary>(reader.readText())
                }
            }.getOrElse { getLibrary() }
        }
        return library.pians.flatMap { pian ->
            pian.chapters.filter { chapter ->
                val content = listOf(chapter.plainText, chapter.text, chapter.translation, chapter.displayId)
                    .joinToString("\n")
                words.all { word -> content.contains(word, ignoreCase = true) }
            }.map { chapter -> pian to chapter }
        }
    }

    private suspend fun loadChapterDetail(active: ResolvedPackage, summary: Chapter): Chapter {
        val path = if (active.locationType == ResourceLocationType.BUNDLED) {
            "content/editions/yangbojun-chapter-${summary.id}.json"
        } else {
            "content/chapters/${summary.id}.json"
        }
        return runCatching {
            storage.open(active, path).bufferedReader().use { reader ->
                json.decodeFromString<Chapter>(reader.readText())
            }
        }.getOrElse { summary }
    }

    private fun loadPianPreview(active: ResolvedPackage, summary: Pian): Pian = runCatching {
        storage.open(active, "content/pian/${summary.slug}.json").bufferedReader().use { reader ->
            json.decodeFromString<Pian>(reader.readText())
        }
    }.getOrElse { summary }

    private fun activePackage(): ResolvedPackage? = resourceRepository.contentSnapshot.value.activeEdition

    private fun synchronizeGeneration() {
        val generation = resourceRepository.contentSnapshot.value.generation
        if (cachedGeneration == generation) return
        cachedGeneration = generation
        cachedLibrary = null
        detailedChapterCache.clear()
    }
}
