package com.ilunyu.lunyu

import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.ExerciseIndex
import com.ilunyu.lunyu.data.model.AppFontPreference
import com.ilunyu.lunyu.data.model.AppThemeMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnalectsDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testAnalectsJsonParsing() {
        val file = File("src/main/assets/content/analects.json")
        assertTrue("analects.json must exist", file.exists())

        val content = file.readText()
        val library = json.decodeFromString<AnalectsLibrary>(content)

        assertEquals(20, library.stats.pianCount)
        assertEquals(512, library.stats.chapterCount)
        assertEquals(20, library.pians.size)

        val firstPian = library.pians.first()
        assertEquals("01-xueer", firstPian.slug)
        assertEquals(16, firstPian.chapters.size)

        val firstChapter = firstPian.chapters.first()
        assertEquals("1·1", firstChapter.displayId)
        assertTrue(firstChapter.text.contains("学而"))
        assertTrue(firstChapter.plainText.contains("学而时习之"))
        assertTrue(firstChapter.translation.isNotBlank())
        assertEquals(8, firstChapter.annotations.size)
    }

    @Test
    fun testExercisesJsonParsing() {
        val file = File("src/main/assets/content/exercises/index.json")
        assertTrue("exercises/index.json must exist", file.exists())

        val content = file.readText()
        val index = json.decodeFromString<ExerciseIndex>(content)

        assertEquals(72, index.exercises.size)

        val years = index.exercises.map { it.year }.filter { it.isNotBlank() }.distinct()
        val sources = index.exercises.map { it.source }.filter { it.isNotBlank() }.distinct()
        val types = index.exercises.map { it.type }.filter { it.isNotBlank() }.distinct()

        assertTrue("Years should not be empty", years.isNotEmpty())
        assertTrue("Sources should not be empty", sources.isNotEmpty())
        assertTrue("Types should not be empty", types.isNotEmpty())
    }

    @Test
    fun testSettingsModelKeys() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("system"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromKey("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromKey("dark"))

        assertEquals(AppFontPreference.SANS, AppFontPreference.fromKey("sans"))
        assertEquals(AppFontPreference.SERIF, AppFontPreference.fromKey("serif"))
        assertEquals(AppFontPreference.SYSTEM, AppFontPreference.fromKey("system"))
    }
}
