package com.ilunyu.lunyu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("SELECT * FROM installed_resources ORDER BY package_id, version_code")
    fun observeInstalledResources(): Flow<List<InstalledResourceEntity>>

    @Query("SELECT * FROM resource_activations")
    fun observeActivations(): Flow<List<ResourceActivationEntity>>

    @Query("SELECT * FROM resource_preferences WHERE id = 0")
    fun observePreferences(): Flow<ResourcePreferenceEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResourcesIgnoringExisting(resources: List<InstalledResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivationsIgnoringExisting(activations: List<ResourceActivationEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPreferenceIgnoringExisting(preference: ResourcePreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResource(resource: InstalledResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivation(activation: ResourceActivationEntity)

    @Query("UPDATE resource_preferences SET active_edition_package_id = :packageId, content_generation = content_generation + 1 WHERE id = 0")
    suspend fun selectEdition(packageId: String)

    @Query("UPDATE resource_activations SET enabled = :enabled WHERE package_id = :packageId")
    suspend fun setEnabled(packageId: String, enabled: Boolean)

    @Query("UPDATE resource_preferences SET content_generation = content_generation + 1 WHERE id = 0")
    suspend fun advanceGeneration()

    @Query("DELETE FROM chapter_exercise_cross_ref WHERE exercise_package_id = :packageId")
    suspend fun clearChapterExerciseReferences(packageId: String)

    @Query("DELETE FROM installed_resources WHERE package_id = :packageId")
    suspend fun deleteResources(packageId: String)

    @Query("DELETE FROM resource_activations WHERE package_id = :packageId")
    suspend fun deleteActivation(packageId: String)

    @Query("UPDATE resource_preferences SET active_edition_package_id = '', content_generation = content_generation + 1 WHERE active_edition_package_id = :packageId")
    suspend fun clearActiveEditionIfSelected(packageId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterExerciseReferences(refs: List<ChapterExerciseCrossRefEntity>)

    @Transaction
    suspend fun installAndActivate(
        resource: InstalledResourceEntity,
        activation: ResourceActivationEntity,
    ) {
        upsertResource(resource)
        upsertActivation(activation)
        advanceGeneration()
    }

    @Transaction
    suspend fun activateEdition(
        packageId: String,
        versionCode: Int,
        locationType: String,
    ) {
        upsertActivation(
            ResourceActivationEntity(
                packageId = packageId,
                activeVersionCode = versionCode,
                activeLocationType = locationType,
                enabled = true,
            )
        )
        selectEdition(packageId)
    }

    @Transaction
    suspend fun initializeBuiltIns(
        resources: List<InstalledResourceEntity>,
        activations: List<ResourceActivationEntity>,
        preference: ResourcePreferenceEntity,
    ) {
        insertResourcesIgnoringExisting(resources)
        insertActivationsIgnoringExisting(activations)
        insertPreferenceIgnoringExisting(preference)
    }

    @Transaction
    suspend fun removeDownloadedPackage(packageId: String) {
        clearChapterExerciseReferences(packageId)
        deleteActivation(packageId)
        deleteResources(packageId)
        clearActiveEditionIfSelected(packageId)
        advanceGeneration()
    }
}
