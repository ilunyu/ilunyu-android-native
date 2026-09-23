package com.ilunyu.lunyu.data.resource

import kotlinx.serialization.Serializable

object ResourceKind {
    const val EDITION = "edition"
    const val EXERCISE = "exercise"
}

object ResourceLocationType {
    const val BUNDLED = "bundled"
    const val DOWNLOADED = "downloaded"
}

@Serializable
data class ResourceManifest(
    val packageFormat: Int,
    val contentSchema: Int,
    val packageId: String,
    val kind: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val minAppVersionCode: Int,
    val sourceRepository: String = "",
    val license: String = "",
    val createdAt: String = "",
    val files: Map<String, String> = emptyMap(),
)

@Serializable
data class ResourceRegistry(
    val schemaVersion: Int,
    val updatedAt: String,
    val packages: List<ResourceRegistryPackage> = emptyList(),
)

@Serializable
data class ResourceRegistryPackage(
    val packageId: String,
    val kind: String,
    val name: String,
    val description: String = "",
    val versionName: String,
    val versionCode: Int,
    val minAppVersionCode: Int,
    val size: Long,
    val sha256: String,
    val sourceRepository: String = "",
    val releasePageUrl: String = "",
    val downloadUrls: List<String> = emptyList(),
)

@Serializable
internal data class ResourceReleaseMetadata(
    val packageId: String = "",
    val kind: String = "",
    val name: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val minAppVersionCode: Int = 0,
    val size: Long = 0,
    val sha256: String = "",
    val sourceRepository: String = "",
)

@Serializable
internal data class GithubRelease(
    val assets: List<GithubReleaseAsset> = emptyList(),
)

@Serializable
internal data class GithubReleaseAsset(
    val name: String,
    @kotlinx.serialization.SerialName("browser_download_url")
    val downloadUrl: String,
)

data class ResolvedPackage(
    val packageId: String,
    val kind: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val locationType: String,
    val rootPath: String,
)

data class ContentSnapshot(
    val generation: Long,
    val activeEdition: ResolvedPackage?,
    val enabledExercisePackages: List<ResolvedPackage>,
    val allExercisePackages: List<ResolvedPackage> = emptyList(),
)

sealed interface ResourceOperationState {
    data object Idle : ResourceOperationState
    data class Downloading(val packageId: String, val receivedBytes: Long, val totalBytes: Long?) : ResourceOperationState
    data class Installing(val packageId: String) : ResourceOperationState
    data class Complete(val packageId: String, val versionName: String) : ResourceOperationState
    data class Failed(val packageId: String?, val message: String) : ResourceOperationState
}
