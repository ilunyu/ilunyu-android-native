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
)

@Serializable
data class ExerciseBlock(
    val type: String = "regular",
    val text: String = "",
    val sourceid: Int? = null,
    val paragraphs: List<ExerciseParagraph> = emptyList()
)

@Serializable
data class ExerciseParagraph(
    val text: String = ""
)
