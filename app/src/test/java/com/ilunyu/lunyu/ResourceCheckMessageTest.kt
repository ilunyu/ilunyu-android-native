package com.ilunyu.lunyu

import com.ilunyu.lunyu.data.db.InstalledResourceEntity
import com.ilunyu.lunyu.data.resource.ResourceKind
import com.ilunyu.lunyu.data.resource.ResourceLocationType
import com.ilunyu.lunyu.data.resource.ResourceRegistry
import com.ilunyu.lunyu.data.resource.ResourceRegistryPackage
import com.ilunyu.lunyu.ui.settings.buildResourceCheckMessage
import com.ilunyu.lunyu.ui.settings.checkUninstalled
import com.ilunyu.lunyu.ui.settings.checkUpdates
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceCheckMessageTest {

    private val pkgEdition = ResourceRegistryPackage(
        packageId = "edition.yangbojun",
        kind = ResourceKind.EDITION,
        name = "杨伯峻《论语译注》",
        versionName = "1.0.0",
        versionCode = 1,
        minAppVersionCode = 1,
        size = 550349,
        sha256 = "dummy",
    )

    private val pkgExercise1 = ResourceRegistryPackage(
        packageId = "exercise.2022-2023",
        kind = ResourceKind.EXERCISE,
        name = "2022—2023 学年试题",
        versionName = "1.0.0",
        versionCode = 1,
        minAppVersionCode = 1,
        size = 42900,
        sha256 = "dummy",
    )

    private val pkgExercise2 = ResourceRegistryPackage(
        packageId = "exercise.2023-2024",
        kind = ResourceKind.EXERCISE,
        name = "2023—2024 学年试题",
        versionName = "1.0.0",
        versionCode = 1,
        minAppVersionCode = 1,
        size = 37344,
        sha256 = "dummy",
    )

    @Test
    fun testCheckUpdatesAndUninstalled_freshInstallScenario() {
        val registry = ResourceRegistry(
            schemaVersion = 1,
            updatedAt = "2026-09-26T00:00:00Z",
            packages = listOf(pkgEdition, pkgExercise1, pkgExercise2),
        )

        // 场景 1: 刚安装软件时啥都没有 (installedResources 为空)
        val installedEmpty = emptyList<InstalledResourceEntity>()
        val updatesEmpty = checkUpdates(registry, installedEmpty)
        val uninstalledAll = checkUninstalled(registry, installedEmpty)

        assertEquals(0, updatesEmpty.size)
        assertEquals(3, uninstalledAll.size)

        val msgEmpty = buildResourceCheckMessage(
            newUpdates = updatesEmpty,
            uninstalled = uninstalledAll,
            hasInstalledResources = installedEmpty.isNotEmpty(),
        )
        assertEquals("杨伯峻《论语译注》等 3 个资源未安装，可下载", msgEmpty)
    }

    @Test
    fun testCheckUpdatesAndUninstalled_partialInstallScenario() {
        val registry = ResourceRegistry(
            schemaVersion = 1,
            updatedAt = "2026-09-26T00:00:00Z",
            packages = listOf(pkgEdition, pkgExercise1, pkgExercise2),
        )

        // 场景 2: 已安装杨伯峻译注（已为最新版），未安装 2 门试题
        val installedEditionOnly = listOf(
            InstalledResourceEntity(
                packageId = pkgEdition.packageId,
                kind = pkgEdition.kind,
                name = pkgEdition.name,
                versionCode = 1,
                versionName = "1.0.0",
                locationType = ResourceLocationType.DOWNLOADED,
                rootPath = "/test",
            )
        )

        val updates = checkUpdates(registry, installedEditionOnly)
        val uninstalled = checkUninstalled(registry, installedEditionOnly)

        assertEquals(0, updates.size)
        assertEquals(2, uninstalled.size)

        val msg = buildResourceCheckMessage(
            newUpdates = updates,
            uninstalled = uninstalled,
            hasInstalledResources = installedEditionOnly.isNotEmpty(),
        )
        assertEquals("已安装资源均为最新版，另有 2 个资源未安装", msg)
    }

    @Test
    fun testCheckUpdatesAndUninstalled_singleUninstalledScenario() {
        val registry = ResourceRegistry(
            schemaVersion = 1,
            updatedAt = "2026-09-26T00:00:00Z",
            packages = listOf(pkgEdition, pkgExercise1),
        )

        val installedEditionOnly = listOf(
            InstalledResourceEntity(
                packageId = pkgEdition.packageId,
                kind = pkgEdition.kind,
                name = pkgEdition.name,
                versionCode = 1,
                versionName = "1.0.0",
                locationType = ResourceLocationType.DOWNLOADED,
                rootPath = "/test",
            )
        )

        val updates = checkUpdates(registry, installedEditionOnly)
        val uninstalled = checkUninstalled(registry, installedEditionOnly)

        assertEquals(0, updates.size)
        assertEquals(1, uninstalled.size)

        val msg = buildResourceCheckMessage(
            newUpdates = updates,
            uninstalled = uninstalled,
            hasInstalledResources = installedEditionOnly.isNotEmpty(),
        )
        assertEquals("已安装资源均为最新版，另有 2022—2023 学年试题 未安装", msg)
    }

    @Test
    fun testCheckUpdatesAndUninstalled_updateAndUninstalledScenario() {
        val registry = ResourceRegistry(
            schemaVersion = 1,
            updatedAt = "2026-09-26T00:00:00Z",
            packages = listOf(
                pkgEdition.copy(versionCode = 2, versionName = "2.0.0"),
                pkgExercise1,
            ),
        )

        // 杨伯峻为旧版本 1，试题库未安装
        val installedOldEdition = listOf(
            InstalledResourceEntity(
                packageId = pkgEdition.packageId,
                kind = pkgEdition.kind,
                name = pkgEdition.name,
                versionCode = 1,
                versionName = "1.0.0",
                locationType = ResourceLocationType.DOWNLOADED,
                rootPath = "/test",
            )
        )

        val updates = checkUpdates(registry, installedOldEdition)
        val uninstalled = checkUninstalled(registry, installedOldEdition)

        assertEquals(1, updates.size)
        assertEquals(1, uninstalled.size)

        val msg = buildResourceCheckMessage(
            newUpdates = updates,
            uninstalled = uninstalled,
            hasInstalledResources = installedOldEdition.isNotEmpty(),
        )
        assertEquals("杨伯峻《论语译注》有更新，另有 2022—2023 学年试题 未安装", msg)
    }

    @Test
    fun testCheckUpdatesAndUninstalled_allLatestScenario() {
        val registry = ResourceRegistry(
            schemaVersion = 1,
            updatedAt = "2026-09-26T00:00:00Z",
            packages = listOf(pkgEdition, pkgExercise1),
        )

        val installedAll = listOf(
            InstalledResourceEntity(
                packageId = pkgEdition.packageId,
                kind = pkgEdition.kind,
                name = pkgEdition.name,
                versionCode = 1,
                versionName = "1.0.0",
                locationType = ResourceLocationType.DOWNLOADED,
                rootPath = "/test",
            ),
            InstalledResourceEntity(
                packageId = pkgExercise1.packageId,
                kind = pkgExercise1.kind,
                name = pkgExercise1.name,
                versionCode = 1,
                versionName = "1.0.0",
                locationType = ResourceLocationType.DOWNLOADED,
                rootPath = "/test2",
            )
        )

        val updates = checkUpdates(registry, installedAll)
        val uninstalled = checkUninstalled(registry, installedAll)

        assertEquals(0, updates.size)
        assertEquals(0, uninstalled.size)

        val msg = buildResourceCheckMessage(
            newUpdates = updates,
            uninstalled = uninstalled,
            hasInstalledResources = installedAll.isNotEmpty(),
        )
        assertEquals("所有资源均为最新版", msg)
    }
}
