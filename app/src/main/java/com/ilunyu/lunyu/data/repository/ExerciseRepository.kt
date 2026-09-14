package com.ilunyu.lunyu.data.repository

import android.content.Context
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.ExerciseIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ExerciseRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedList: List<Exercise>? = null

    suspend fun getExerciseList(): List<Exercise> = withContext(Dispatchers.IO) {
        cachedList?.let { return@withContext it }
        val content = context.assets.open("content/exercises/index.json").bufferedReader().use { it.readText() }
        val index = json.decodeFromString<ExerciseIndex>(content)
        cachedList = index.exercises
        index.exercises
    }

    suspend fun getExerciseDetail(id: String): Exercise? = withContext(Dispatchers.IO) {
        try {
            val content = context.assets.open("content/exercises/$id.json").bufferedReader().use { it.readText() }
            json.decodeFromString<Exercise>(content)
        } catch (_: Exception) {
            getExerciseList().find { it.id == id }
        }
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
        val list = getExerciseList()
        val q = query.trim()
        return list.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.source.contains(q, ignoreCase = true) ||
            it.year.contains(q, ignoreCase = true) ||
            it.type.contains(q, ignoreCase = true)
        }
    }
}
