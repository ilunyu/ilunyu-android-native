package com.ilunyu.lunyu.data.model

import kotlinx.serialization.SerialName
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
    @SerialName("id")
    val key: String = "",
    val name: String = "",
    val shortName: String = "",
    val author: String = "",
    val publisher: String = "",
    val description: String = ""
) {
    val displayName: String
        get() {
            if (shortName.isNotEmpty()) return shortName
            val book = name.split("（").first().trim()
            return if (author.isEmpty()) book else "$author《$book》"
        }
}

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
data class RelatedQuestion(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val source: String = "",
    val type: String = ""
)

@Serializable
data class Chapter(
    val id: String,
    val number: Int,
    val displayId: String,
    val text: String = "",
    val plainText: String = "",
    val translation: String = "",
    val annotations: List<Annotation> = emptyList(),
    val comment: String = "",
    val sourceReference: String = "",
    val relatedQuestions: List<RelatedQuestion> = emptyList()
)

@Serializable
data class Annotation(
    val index: Int,
    val label: String,
    val text: String
)
