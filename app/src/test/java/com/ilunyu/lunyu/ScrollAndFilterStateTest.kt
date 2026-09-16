package com.ilunyu.lunyu

import com.ilunyu.lunyu.ui.favorites.FavoritesSortMode
import com.ilunyu.lunyu.ui.study.getGradeChipLabel
import com.ilunyu.lunyu.ui.study.getSourceChipLabel
import com.ilunyu.lunyu.ui.study.getTypeChipLabel
import com.ilunyu.lunyu.ui.study.getYearChipLabel
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

    @Test
    fun testCollapsibleSingleTopBarGeometry() {
        val maxHeight = 2100
        val topBarHeight = 150 // e.g. 56dp

        // Test fully expanded (offset = 0)
        run {
            val currentOffset = 0
            val contentTop = topBarHeight + currentOffset
            val contentHeight = (maxHeight - contentTop).coerceAtLeast(0)
            assertEquals(150, contentTop)
            assertEquals(1950, contentHeight)
            assertEquals(maxHeight, contentTop + contentHeight)
        }

        // Test fully collapsed (offset = -topBarHeight)
        run {
            val currentOffset = -topBarHeight
            val contentTop = topBarHeight + currentOffset
            val contentHeight = (maxHeight - contentTop).coerceAtLeast(0)
            assertEquals(0, contentTop)
            assertEquals(maxHeight, contentHeight)
            assertEquals(maxHeight, contentTop + contentHeight)
        }

        // Test partially collapsed (offset = -75)
        run {
            val currentOffset = -75
            val contentTop = topBarHeight + currentOffset
            val contentHeight = (maxHeight - contentTop).coerceAtLeast(0)
            assertEquals(75, contentTop)
            assertEquals(2025, contentHeight)
            assertEquals(maxHeight, contentTop + contentHeight)
        }
    }

    @Test
    fun testCollapsibleTabLayoutGeometry() {
        val maxHeight = 2100
        val topBarHeight = 150 // 56dp
        val tabBarHeight = 130 // 48dp

        // Test fully expanded (offset = 0)
        run {
            val currentOffset = 0
            val contentTop = topBarHeight + tabBarHeight + currentOffset
            val contentHeight = (maxHeight - contentTop).coerceAtLeast(0)
            assertEquals(280, contentTop)
            assertEquals(1820, contentHeight)
            assertEquals(maxHeight, contentTop + contentHeight)
        }

        // Test fully collapsed (offset = -topBarHeight)
        run {
            val currentOffset = -topBarHeight
            val contentTop = topBarHeight + tabBarHeight + currentOffset
            val contentHeight = (maxHeight - contentTop).coerceAtLeast(0)
            assertEquals(tabBarHeight, contentTop)
            assertEquals(maxHeight - tabBarHeight, contentHeight)
            assertEquals(maxHeight, contentTop + contentHeight)
        }
    }

    @Test
    fun testExerciseFilterChipLabels() {
        // 学年
        assertEquals("学年", getYearChipLabel(emptySet<String>()))
        assertEquals("2024", getYearChipLabel(setOf("2024")))
        assertEquals("2个学年", getYearChipLabel(setOf("2023", "2024")))

        // 地区
        assertEquals("地区", getSourceChipLabel(emptySet<String>()))
        assertEquals("东城", getSourceChipLabel(setOf("东城")))
        assertEquals("3个地区", getSourceChipLabel(setOf("东城", "西城", "海淀")))

        // 年级
        assertEquals("年级", getGradeChipLabel(emptySet<Int>()))
        assertEquals("高一", getGradeChipLabel(setOf(1)))
        assertEquals("高二", getGradeChipLabel(setOf(2)))
        assertEquals("高三", getGradeChipLabel(setOf(3)))
        assertEquals("2个年级", getGradeChipLabel(setOf(1, 2)))
        assertEquals("全部年级", getGradeChipLabel(setOf(1, 2, 3)))

        // 类别
        assertEquals("类别", getTypeChipLabel(emptySet<String>()))
        assertEquals("期末", getTypeChipLabel(setOf("期末")))
        assertEquals("4类试卷", getTypeChipLabel(setOf("期末", "期中", "模拟", "练习")))
    }
}
