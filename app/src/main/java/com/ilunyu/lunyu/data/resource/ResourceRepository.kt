package com.ilunyu.lunyu.data.resource

import android.content.Context
import com.ilunyu.lunyu.data.db.AppDatabase
import com.ilunyu.lunyu.data.db.InstalledResourceEntity
import com.ilunyu.lunyu.data.db.ResourceActivationEntity
import com.ilunyu.lunyu.data.db.ResourcePreferenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ResourceRepository(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val resourceDao = database.resourceDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bundledEdition = ResolvedPackage(
        packageId = "edition.yangbojun",
        kind = ResourceKind.EDITION,
        name = "杨伯峻《论语译注》",
        versionName = "内置",
        versionCode = 0,
        locationType = ResourceLocationType.BUNDLED,
        rootPath = "",
    )
    private val bundledExercises = ResolvedPackage(
        packageId = "exercise.bundled",
        kind = ResourceKind.EXERCISE,
        name = "内置试题库",
        versionName = "内置",
        versionCode = 0,
        locationType = ResourceLocationType.BUNDLED,
        rootPath = "",
    )

    private val hasBundledEdition = assetExists("content/editions/index.json")
    private val hasBundledExercises = assetExists("content/exercises/index.json")

    private val defaultSnapshot = ContentSnapshot(
        generation = 0,
        activeEdition = bundledEdition.takeIf { hasBundledEdition },
        enabledExercisePackages = listOfNotNull(bundledExercises.takeIf { hasBundledExercises }),
        allExercisePackages = listOfNotNull(bundledExercises.takeIf { hasBundledExercises }),
    )

    private val availableInstalledResources = resourceDao.observeInstalledResources().map { resources ->
        resources.filter(::isAvailable)
    }

    val contentSnapshot: StateFlow<ContentSnapshot> = combine(
        availableInstalledResources,
        resourceDao.observeActivations(),
        resourceDao.observePreferences(),
    ) { installed, activations, preference ->
        buildSnapshot(installed, activations, preference)
    }.stateIn(scope, SharingStarted.Eagerly, defaultSnapshot)

    val installedResources: StateFlow<List<InstalledResourceEntity>> = availableInstalledResources
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            resourceDao.initializeBuiltIns(
                resources = buildList {
                    if (hasBundledEdition) add(InstalledResourceEntity(
                        packageId = bundledEdition.packageId,
                        kind = bundledEdition.kind,
                        name = bundledEdition.name,
                        versionCode = bundledEdition.versionCode,
                        versionName = bundledEdition.versionName,
                        locationType = bundledEdition.locationType,
                        rootPath = bundledEdition.rootPath,
                    ))
                    if (hasBundledExercises) add(InstalledResourceEntity(
                        packageId = bundledExercises.packageId,
                        kind = bundledExercises.kind,
                        name = bundledExercises.name,
                        versionCode = bundledExercises.versionCode,
                        versionName = bundledExercises.versionName,
                        locationType = bundledExercises.locationType,
                        rootPath = bundledExercises.rootPath,
                    ))
                },
                activations = buildList {
                    if (hasBundledEdition) add(ResourceActivationEntity(
                        packageId = bundledEdition.packageId,
                        activeVersionCode = bundledEdition.versionCode,
                        activeLocationType = bundledEdition.locationType,
                    ))
                    if (hasBundledExercises) add(ResourceActivationEntity(
                        packageId = bundledExercises.packageId,
                        activeVersionCode = bundledExercises.versionCode,
                        activeLocationType = bundledExercises.locationType,
                    ))
                },
                preference = ResourcePreferenceEntity(
                    activeEditionPackageId = bundledEdition.packageId.takeIf { hasBundledEdition }.orEmpty(),
                ),
            )
        }
    }

    suspend fun registerDownloadedPackage(
        manifest: ResourceManifest,
        packageDirectory: File,
        sha256: String,
        originUrl: String,
    ) {
        val installed = InstalledResourceEntity(
            packageId = manifest.packageId,
            kind = manifest.kind,
            name = manifest.name,
            versionCode = manifest.versionCode,
            versionName = manifest.versionName,
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = packageDirectory.absolutePath,
            originUrl = originUrl.ifBlank { manifest.sourceRepository },
            sha256 = sha256,
        )
        resourceDao.installAndActivate(
            resource = installed,
            activation = ResourceActivationEntity(
                packageId = manifest.packageId,
                activeVersionCode = manifest.versionCode,
                activeLocationType = ResourceLocationType.DOWNLOADED,
                enabled = true,
            ),
        )
    }

    suspend fun selectEdition(packageId: String, versionCode: Int, locationType: String) {
        resourceDao.activateEdition(packageId, versionCode, locationType)
    }

    suspend fun setExerciseEnabled(packageId: String, enabled: Boolean) {
        resourceDao.setEnabled(packageId, enabled)
        resourceDao.advanceGeneration()
    }

    suspend fun removeDownloadedPackage(packageId: String) {
        resourceDao.removeDownloadedPackage(packageId)
    }

    private fun buildSnapshot(
        installed: List<InstalledResourceEntity>,
        activations: List<ResourceActivationEntity>,
        preference: ResourcePreferenceEntity?,
    ): ContentSnapshot {
        val activationByPackage = activations.associateBy { it.packageId }
        val resolved = installed.mapNotNull { resource ->
            val activation = activationByPackage[resource.packageId] ?: return@mapNotNull null
            if (activation.activeVersionCode != resource.versionCode ||
                activation.activeLocationType != resource.locationType
            ) {
                return@mapNotNull null
            }
            ResolvedPackage(
                packageId = resource.packageId,
                kind = resource.kind,
                name = resource.name,
                versionName = resource.versionName,
                versionCode = resource.versionCode,
                locationType = resource.locationType,
                rootPath = resource.rootPath,
            )
        }
        val activeEditionId = preference?.activeEditionPackageId.orEmpty()
        val activeEdition = resolved.firstOrNull {
            it.kind == ResourceKind.EDITION && it.packageId == activeEditionId
        } ?: resolved.firstOrNull { it.kind == ResourceKind.EDITION }
        val exercises = resolved.filter { resource ->
            resource.kind == ResourceKind.EXERCISE && activationByPackage[resource.packageId]?.enabled == true
        }.sortedWith(
            compareBy<ResolvedPackage> { it.locationType != ResourceLocationType.BUNDLED }
                .thenBy { it.packageId }
        )
        val allExercises = resolved.filter { resource ->
            resource.kind == ResourceKind.EXERCISE
        }.sortedWith(
            compareBy<ResolvedPackage> { it.locationType != ResourceLocationType.BUNDLED }
                .thenBy { it.packageId }
        )
        return ContentSnapshot(
            generation = preference?.contentGeneration ?: 0,
            activeEdition = activeEdition,
            enabledExercisePackages = exercises,
            allExercisePackages = allExercises,
        )
    }

    private fun assetExists(path: String): Boolean = runCatching {
        context.assets.open(path).close()
    }.isSuccess

    private fun isAvailable(resource: InstalledResourceEntity): Boolean = when (resource.locationType) {
        ResourceLocationType.BUNDLED -> when (resource.packageId) {
            bundledEdition.packageId -> hasBundledEdition
            bundledExercises.packageId -> hasBundledExercises
            else -> false
        }
        else -> File(resource.rootPath).isDirectory
    }
}
