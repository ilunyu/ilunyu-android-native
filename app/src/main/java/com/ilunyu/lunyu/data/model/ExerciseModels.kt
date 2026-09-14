package com.ilunyu.lunyu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseIndex(
    val formatVersion: Int = 2,
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class Exercise(
    val id: String,
    val title: String,
    val year: String = "",
    val month: Int = 0,
    val source: String = "",
    val grade: Int = 3,
    val type: String = "",
    val number: Int = 0,
    val score: Int = 0,
    val question: List<ExerciseBlock> = emptyList(),
    val answer: List<ExerciseBlock> = emptyList()
) {
    val monthLabel: String
        get() {
            if (month <= 0) return ""
            val y = month / 100
            val m = month % 100
            if (y <= 0 || m <= 0 || m > 12) return "$month"
            return "${y}年${m}月"
        }
}

@Serializable
data class ExerciseBlock(
    val type: String = "regular",
    val text: String = "",
    val sourceid: Int? = null,
    val blocktitle: String = "",
    val sourcename: String = "",
    val paragraphs: List<ExerciseParagraph> = emptyList()
) {
    val isMaterial: Boolean get() = type == "material"
    val isNote: Boolean get() = type == "note"
}

@Serializable
data class ExerciseParagraph(
    val text: String = ""
)
