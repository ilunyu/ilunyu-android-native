package com.ilunyu.lunyu

import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.Exercise
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

    @Test
    fun testExerciseSearchJsonParsingAndSearch() {
        val file = File("src/main/assets/content/exercises/search.json")
        assertTrue("exercises/search.json must exist", file.exists())

        val content = file.readText()
        val index = json.decodeFromString<ExerciseIndex>(content)
        assertTrue("search exercises size should be > 70", index.exercises.size >= 70)

        // Verify that question content actually exists
        val firstEx = index.exercises.first { it.id == "201506-bjgk" }
        assertTrue("first exercise must have questions", firstEx.question.isNotEmpty())
        val firstMaterial = firstEx.question.firstOrNull { it.isMaterial }
        assertNotNull("first exercise must have material", firstMaterial)
        assertTrue(firstMaterial!!.paragraphs.any { it.text.contains("侍坐") || it.text.contains("夫子何哂由也") })

        // Test searching for classical quotes: "侍坐", "富与贵", "温故而知新"
        fun Exercise.toContentText(): String {
            val sb = StringBuilder()
            for (b in question) {
                sb.append(b.text).append('\n').append(b.blocktitle).append('\n').append(b.sourcename).append('\n')
                for (p in b.paragraphs) sb.append(p.text).append('\n')
            }
            return sb.toString()
        }

        val queryShizuo = "侍坐"
        val matchesShizuo = index.exercises.filter { it.toContentText().contains(queryShizuo) }
        assertTrue("Searching '侍坐' must find matching exercises", matchesShizuo.isNotEmpty())

        val queryFuyugui = "富与贵"
        val matchesFuyugui = index.exercises.filter { it.toContentText().contains(queryFuyugui) }
        assertTrue("Searching '富与贵' must find matching exercises", matchesFuyugui.isNotEmpty())

        // Test that title/source metadata like "昌平二模" or "2024" without question content does not match
        val matchesMockTitle = index.exercises.filter { it.toContentText().contains("昌平二模") }
        assertTrue("Searching '昌平二模' in question content should return empty", matchesMockTitle.isEmpty())
    }
}
