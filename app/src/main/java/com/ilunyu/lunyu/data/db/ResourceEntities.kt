package com.ilunyu.lunyu.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installed_resources",
    primaryKeys = ["package_id", "version_code", "location_type"],
    indices = [Index(value = ["kind"])]
)
data class InstalledResourceEntity(
    @ColumnInfo(name = "package_id")
    val packageId: String,
    val kind: String,
    val name: String,
    @ColumnInfo(name = "version_code")
    val versionCode: Int,
    @ColumnInfo(name = "version_name")
    val versionName: String,
    @ColumnInfo(name = "location_type")
    val locationType: String,
    @ColumnInfo(name = "root_path")
    val rootPath: String,
    @ColumnInfo(name = "origin_url")
    val originUrl: String = "",
    val sha256: String = "",
    @ColumnInfo(name = "installed_at")
    val installedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "resource_activations")
data class ResourceActivationEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String,
    @ColumnInfo(name = "active_version_code")
    val activeVersionCode: Int,
    @ColumnInfo(name = "active_location_type")
    val activeLocationType: String,
    val enabled: Boolean = true,
)

@Entity(tableName = "resource_preferences")
data class ResourcePreferenceEntity(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "active_edition_package_id")
    val activeEditionPackageId: String,
    @ColumnInfo(name = "content_generation")
    val contentGeneration: Long = 0,
)

@Entity(
    tableName = "chapter_exercise_cross_ref",
    primaryKeys = ["chapter_id", "exercise_package_id", "exercise_id"],
    indices = [Index(value = ["chapter_id"]), Index(value = ["exercise_id"])]
)
data class ChapterExerciseCrossRefEntity(
    @ColumnInfo(name = "chapter_id")
    val chapterId: String,
    @ColumnInfo(name = "exercise_package_id")
    val exercisePackageId: String,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
)
