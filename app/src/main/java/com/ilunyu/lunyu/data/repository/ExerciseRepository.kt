package com.ilunyu.lunyu.data.repository

import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.ExerciseIndex
import com.ilunyu.lunyu.data.model.RelatedQuestion
import com.ilunyu.lunyu.data.resource.ResourceLocationType
import com.ilunyu.lunyu.data.resource.ResourcePackageStorage
import com.ilunyu.lunyu.data.resource.ResourceRepository
import com.ilunyu.lunyu.data.resource.ResolvedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class ExerciseRepository(
    private val storage: ResourcePackageStorage,
    private val resourceRepository: ResourceRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedGeneration = Long.MIN_VALUE
    private var cachedList: List<Exercise>? = null
    private var cachedSearchList: List<Exercise>? = null
    private var cachedAllInstalledList: List<Exercise>? = null
    private var exerciseOrigins: Map<String, ResolvedPackage> = emptyMap()
    private var allInstalledOrigins: Map<String, ResolvedPackage> = emptyMap()
    private val detailedExerciseCache = ConcurrentHashMap<String, Exercise>()
    private val relatedQuestionCache = ConcurrentHashMap<String, List<RelatedQuestion>>()

    suspend fun getExerciseList(): List<Exercise> = withContext(Dispatchers.IO) {
        synchronizeGeneration()
        cachedList?.let { return@withContext it }
        val byId = linkedMapOf<String, Exercise>()
        val origins = mutableMapOf<String, ResolvedPackage>()
        currentPackages().forEach { resource ->
            readIndex(resource, search = false).exercises.forEach { exercise ->
                byId[exercise.id] = exercise
                origins[exercise.id] = resource
            }
        }
        exerciseOrigins = origins
        byId.values.toList().also { cachedList = it }
    }

    suspend fun getAllInstalledExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        synchronizeGeneration()
        cachedAllInstalledList?.let { return@withContext it }
        val byId = linkedMapOf<String, Exercise>()
        val origins = mutableMapOf<String, ResolvedPackage>()
        allPackages().forEach { resource ->
            readIndex(resource, search = false).exercises.forEach { exercise ->
                byId[exercise.id] = exercise
                origins[exercise.id] = resource
            }
        }
        allInstalledOrigins = origins
        byId.values.toList().also { cachedAllInstalledList = it }
    }

    suspend fun getSearchExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        synchronizeGeneration()
        cachedSearchList?.let { return@withContext it }
        val byId = linkedMapOf<String, Exercise>()
        currentPackages().forEach { resource ->
            readIndex(resource, search = true).exercises.forEach { exercise -> byId[exercise.id] = exercise }
        }
        byId.values.toList().also { cachedSearchList = it }
    }

    suspend fun getExerciseDetail(id: String): Exercise? = withContext(Dispatchers.IO) {
        val canonicalId = UserPreferencesRepository.migrateExerciseId(id)
        synchronizeGeneration()
        detailedExerciseCache[canonicalId]?.let { return@withContext it }
        if (exerciseOrigins.isEmpty()) getExerciseList()
        var resource = exerciseOrigins[canonicalId]
        if (resource == null) {
            if (allInstalledOrigins.isEmpty()) getAllInstalledExercises()
            resource = allInstalledOrigins[canonicalId]
        }
        val fallbackSummary = cachedList?.find { it.id == canonicalId }
            ?: cachedAllInstalledList?.find { it.id == canonicalId }

        if (resource == null) return@withContext fallbackSummary

        val path = if (resource.locationType == ResourceLocationType.BUNDLED) {
            "content/exercises/$canonicalId.json"
        } else {
            "content/items/$canonicalId.json"
        }
        val detail = runCatching {
            storage.open(resource, path).bufferedReader().use { reader ->
                json.decodeFromString<Exercise>(reader.readText())
            }
        }.getOrNull() ?: fallbackSummary
        detail?.also { loaded -> detailedExerciseCache[canonicalId] = loaded }
    }

    suspend fun getFilterOptions(): Triple<List<String>, List<String>, List<String>> {
        val list = getExerciseList()
        val years = list.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
        val sources = list.map { it.source }.filter { it.isNotBlank() }.distinct().sorted()
        val types = list.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
        return Triple(years, sources, types)
    }

    suspend fun search(query: String): List<Exercise> {
        if (query.isBlank()) return emptyList()
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return getSearchExercises().filter { exercise ->
            val text = listOf(exercise.title, exercise.source, exercise.year, exercise.type)
                .joinToString("\n")
            words.all { word -> text.contains(word, ignoreCase = true) }
        }
    }

    suspend fun getRelatedQuestions(chapterId: String): List<RelatedQuestion> {
        relatedQuestionCache[chapterId]?.let { return it }
        val result = getExerciseList().mapNotNull { summary ->
            val detail = getExerciseDetail(summary.id) ?: return@mapNotNull null
            val referencesChapter = detail.question.any { block ->
                block.isMaterial && block.sourceid?.toString() == chapterId
            }
            if (referencesChapter) {
                RelatedQuestion(
                    id = detail.id,
                    title = detail.title,
                    year = detail.year,
                    source = detail.source,
                    type = detail.type,
                )
            } else {
                null
            }
        }
        relatedQuestionCache[chapterId] = result
        return result
    }

    private fun readIndex(resource: ResolvedPackage, search: Boolean): ExerciseIndex {
        val path = when {
            resource.locationType == ResourceLocationType.BUNDLED && search -> "content/exercises/search.json"
            resource.locationType == ResourceLocationType.BUNDLED -> "content/exercises/index.json"
            search -> "content/search.json"
            else -> "content/index.json"
        }
        return runCatching {
            storage.open(resource, path).bufferedReader().use { reader ->
                json.decodeFromString<ExerciseIndex>(reader.readText())
            }
        }.getOrElse { ExerciseIndex() }
    }

    private fun currentPackages(): List<ResolvedPackage> = resourceRepository.contentSnapshot.value.enabledExercisePackages

    private fun allPackages(): List<ResolvedPackage> = resourceRepository.contentSnapshot.value.allExercisePackages

    private fun synchronizeGeneration() {
        val generation = resourceRepository.contentSnapshot.value.generation
        if (cachedGeneration == generation) return
        cachedGeneration = generation
        cachedList = null
        cachedSearchList = null
        cachedAllInstalledList = null
        exerciseOrigins = emptyMap()
        allInstalledOrigins = emptyMap()
        detailedExerciseCache.clear()
        relatedQuestionCache.clear()
    }
}
