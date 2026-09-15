package com.ilunyu.lunyu

import com.ilunyu.lunyu.ui.favorites.FavoritesSortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScrollAndFilterStateTest {

    @Test
    fun testStudyFilterChangeResetsScroll() {
        var studyScrollIndex = 15
        var studyScrollOffset = 120
        var studyYears = setOf("2024")
        var studySources = setOf<String>()
        var studyGrades = setOf<Int>()
        var studyTypes = setOf<String>()

        fun setStudyFilters(years: Set<String>, sources: Set<String>, grades: Set<Int>, types: Set<String>) {
            if (studyYears != years || studySources != sources || studyGrades != grades || studyTypes != types) {
                studyYears = years
                studySources = sources
                studyGrades = grades
                studyTypes = types
                studyScrollIndex = 0
                studyScrollOffset = 0
            }
        }

        // Apply same filters: should NOT reset
        setStudyFilters(setOf("2024"), emptySet(), emptySet(), emptySet())
        assertEquals(15, studyScrollIndex)
        assertEquals(120, studyScrollOffset)

        // Change filter: MUST reset
        setStudyFilters(setOf("2023"), emptySet(), emptySet(), emptySet())
        assertEquals(0, studyScrollIndex)
        assertEquals(0, studyScrollOffset)
    }

    @Test
    fun testFavoritesSortChangeResetsScroll() {
        var chapterScrollIndex = 8
        var chapterScrollOffset = 50
        var exerciseScrollIndex = 12
        var exerciseScrollOffset = 80
        var sortMode = FavoritesSortMode.DEFAULT

        fun setFavoritesSortMode(mode: FavoritesSortMode) {
            if (sortMode != mode) {
                sortMode = mode
                chapterScrollIndex = 0
                chapterScrollOffset = 0
                exerciseScrollIndex = 0
                exerciseScrollOffset = 0
            }
        }

        setFavoritesSortMode(FavoritesSortMode.DEFAULT)
        assertEquals(8, chapterScrollIndex)
        assertEquals(12, exerciseScrollIndex)

        setFavoritesSortMode(FavoritesSortMode.NEWEST_FIRST)
        assertEquals(0, chapterScrollIndex)
        assertEquals(0, exerciseScrollIndex)
    }

    @Test
    fun testFavoritesFilterChangeResetsScroll() {
        var exerciseScrollIndex = 20
        var exerciseScrollOffset = 100
        var years = setOf<String>()

        fun setFavoritesFilters(newYears: Set<String>) {
            if (years != newYears) {
                years = newYears
                exerciseScrollIndex = 0
                exerciseScrollOffset = 0
            }
        }

        setFavoritesFilters(setOf("2024"))
        assertEquals(0, exerciseScrollIndex)
        assertEquals(0, exerciseScrollOffset)
    }

    @Test
    fun testPianScrollMapIsolation() {
        val pianScrollMap = mutableMapOf<String, Pair<Int, Int>>()

        fun savePianScroll(slug: String, index: Int, offset: Int) {
            pianScrollMap[slug] = index to offset
        }

        savePianScroll("01-xueer", 5, 40)
        savePianScroll("02-weizheng", 10, 80)

        assertEquals(5 to 40, pianScrollMap["01-xueer"])
        assertEquals(10 to 80, pianScrollMap["02-weizheng"])
        assertEquals(null, pianScrollMap["03-bayi"])
    }
}
