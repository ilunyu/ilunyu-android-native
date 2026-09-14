package com.ilunyu.lunyu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EditionIndex(
    val defaultEdition: String = "yangbojun",
    val editions: List<EditionSummary> = emptyList()
)

@Serializable
data class EditionSummary(
    val id: String,
    val name: String,
    val shortName: String,
    val author: String = "",
    val publisher: String = "",
    val description: String = "",
    val asset: String = ""
)

@Serializable
data class AnalectsLibrary(
    val formatVersion: Int = 1,
    val source: AnalectsSource = AnalectsSource(),
    val stats: AnalectsStats = AnalectsStats(),
    val pians: List<Pian> = emptyList()
)

@Serializable
data class AnalectsSource(
    val key: String = "",
    val name: String = "",
    val author: String = "",
    val publisher: String = "",
    val description: String = ""
)

@Serializable
data class AnalectsStats(
    val pianCount: Int = 20,
    val chapterCount: Int = 512
)

@Serializable
data class Pian(
    val id: String,
    val number: Int,
    val slug: String,
    val title: String,
    val shortTitle: String,
    val comment: String = "",
    val chapterCount: Int = 0,
    val chapters: List<Chapter> = emptyList()
)

@Serializable
data class Chapter(
    val id: String,
    val number: Int,
    val displayId: String,
    val text: String,
    val plainText: String,
    val translation: String = "",
    val annotations: List<Annotation> = emptyList()
)

@Serializable
data class Annotation(
    val index: Int,
    val label: String,
    val text: String
)
