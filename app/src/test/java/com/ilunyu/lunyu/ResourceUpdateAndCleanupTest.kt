package com.ilunyu.lunyu

import com.ilunyu.lunyu.data.db.ChapterExerciseCrossRefEntity
import com.ilunyu.lunyu.data.db.InstalledResourceEntity
import com.ilunyu.lunyu.data.db.ResourceActivationEntity
import com.ilunyu.lunyu.data.db.ResourceDao
import com.ilunyu.lunyu.data.db.ResourcePreferenceEntity
import com.ilunyu.lunyu.data.resource.ResourceKind
import com.ilunyu.lunyu.data.resource.ResourceLocationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeResourceDao : ResourceDao {
    val resources = mutableListOf<InstalledResourceEntity>()
    val activations = mutableListOf<ResourceActivationEntity>()
    var preference: ResourcePreferenceEntity? = ResourcePreferenceEntity(id = 0, activeEditionPackageId = "", contentGeneration = 0)

    private val resourcesFlow = MutableStateFlow<List<InstalledResourceEntity>>(emptyList())
    private val activationsFlow = MutableStateFlow<List<ResourceActivationEntity>>(emptyList())
    private val preferenceFlow = MutableStateFlow<ResourcePreferenceEntity?>(preference)

    private fun emit() {
        resourcesFlow.value = resources.toList()
        activationsFlow.value = activations.toList()
        preferenceFlow.value = preference
    }

    override fun observeInstalledResources(): Flow<List<InstalledResourceEntity>> = resourcesFlow
    override fun observeActivations(): Flow<List<ResourceActivationEntity>> = activationsFlow
    override fun observePreferences(): Flow<ResourcePreferenceEntity?> = preferenceFlow

    override suspend fun insertResourcesIgnoringExisting(resources: List<InstalledResourceEntity>) {
        for (r in resources) {
            if (this.resources.none { it.packageId == r.packageId && it.versionCode == r.versionCode && it.locationType == r.locationType }) {
                this.resources.add(r)
            }
        }
        emit()
    }

    override suspend fun insertActivationsIgnoringExisting(activations: List<ResourceActivationEntity>) {
        for (a in activations) {
            if (this.activations.none { it.packageId == a.packageId }) {
                this.activations.add(a)
            }
        }
        emit()
    }

    override suspend fun insertPreferenceIgnoringExisting(preference: ResourcePreferenceEntity) {
        if (this.preference == null) {
            this.preference = preference
            emit()
        }
    }

    override suspend fun upsertResource(resource: InstalledResourceEntity) {
        resources.removeAll { it.packageId == resource.packageId && it.versionCode == resource.versionCode && it.locationType == resource.locationType }
        resources.add(resource)
        emit()
    }

    override suspend fun upsertActivation(activation: ResourceActivationEntity) {
        activations.removeAll { it.packageId == activation.packageId }
        activations.add(activation)
        emit()
    }

    override suspend fun selectEdition(packageId: String) {
        preference = preference?.copy(
            activeEditionPackageId = packageId,
            contentGeneration = (preference?.contentGeneration ?: 0) + 1,
        ) ?: ResourcePreferenceEntity(id = 0, activeEditionPackageId = packageId, contentGeneration = 1)
        emit()
    }

    override suspend fun setEnabled(packageId: String, enabled: Boolean) {
        val index = activations.indexOfFirst { it.packageId == packageId }
        if (index >= 0) {
            activations[index] = activations[index].copy(enabled = enabled)
            emit()
        }
    }

    override suspend fun advanceGeneration() {
        preference = preference?.copy(
            contentGeneration = (preference?.contentGeneration ?: 0) + 1,
        )
        emit()
    }

    override suspend fun clearChapterExerciseReferences(packageId: String) {}

    override suspend fun deleteResources(packageId: String) {
        resources.removeAll { it.packageId == packageId }
        emit()
    }

    override suspend fun deleteOtherDownloadedResources(packageId: String, currentVersionCode: Int) {
        resources.removeAll {
            it.packageId == packageId && it.locationType == ResourceLocationType.DOWNLOADED && it.versionCode != currentVersionCode
        }
        emit()
    }

    override suspend fun getBundledResource(packageId: String): InstalledResourceEntity? {
        return resources.firstOrNull { it.packageId == packageId && it.locationType == ResourceLocationType.BUNDLED }
    }

    override suspend fun deleteDownloadedResources(packageId: String) {
        resources.removeAll { it.packageId == packageId && it.locationType == ResourceLocationType.DOWNLOADED }
        emit()
    }

    override suspend fun getDownloadedResources(): List<InstalledResourceEntity> {
        return resources.filter { it.locationType == ResourceLocationType.DOWNLOADED }
    }

    override suspend fun getActivations(): List<ResourceActivationEntity> {
        return activations.toList()
    }

    override suspend fun deleteDownloadedResourceVersion(packageId: String, versionCode: Int) {
        resources.removeAll {
            it.packageId == packageId && it.versionCode == versionCode && it.locationType == ResourceLocationType.DOWNLOADED
        }
        emit()
    }

    override suspend fun deleteActivation(packageId: String) {
        activations.removeAll { it.packageId == packageId }
        emit()
    }

    override suspend fun clearActiveEditionIfSelected(packageId: String) {
        if (preference?.activeEditionPackageId == packageId) {
            preference = preference?.copy(
                activeEditionPackageId = "",
                contentGeneration = (preference?.contentGeneration ?: 0) + 1,
            )
            emit()
        }
    }

    override suspend fun insertChapterExerciseReferences(refs: List<ChapterExerciseCrossRefEntity>) {}
}

class ResourceUpdateAndCleanupTest {

    private lateinit var dao: FakeResourceDao

    @Before
    fun setUp() {
        dao = FakeResourceDao()
    }

    @Test
    fun testInstallAndActivateCleansUpOldDownloadedVersion() = runBlocking {
        // Given version 1 is installed
        val v1 = InstalledResourceEntity(
            packageId = "edition.custom",
            kind = ResourceKind.EDITION,
            name = "测试译注",
            versionCode = 1,
            versionName = "1.0",
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = "/packages/edition.custom/1",
        )
        dao.upsertResource(v1)
        dao.upsertActivation(
            ResourceActivationEntity(
                packageId = "edition.custom",
                activeVersionCode = 1,
                activeLocationType = ResourceLocationType.DOWNLOADED,
                enabled = true,
            )
        )

        assertEquals(1, dao.resources.size)

        // When updating to version 2
        val v2 = InstalledResourceEntity(
            packageId = "edition.custom",
            kind = ResourceKind.EDITION,
            name = "测试译注",
            versionCode = 2,
            versionName = "2.0",
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = "/packages/edition.custom/2",
        )
        dao.installAndActivate(
            v2,
            ResourceActivationEntity(
                packageId = "edition.custom",
                activeVersionCode = 2,
                activeLocationType = ResourceLocationType.DOWNLOADED,
                enabled = true,
            )
        )

        // Then only version 2 exists in DB, version 1 is removed
        assertEquals(1, dao.resources.size)
        val current = dao.resources.first()
        assertEquals(2, current.versionCode)
        assertEquals("2.0", current.versionName)

        // And activation points to version 2
        val act = dao.activations.first { it.packageId == "edition.custom" }
        assertEquals(2, act.activeVersionCode)
        assertEquals("edition.custom", dao.preference?.activeEditionPackageId)
    }

    @Test
    fun testRemoveDownloadedPackageWithNoBundledVersion() = runBlocking {
        val downloaded = InstalledResourceEntity(
            packageId = "exercise.custom",
            kind = ResourceKind.EXERCISE,
            name = "自定义题库",
            versionCode = 1,
            versionName = "1.0",
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = "/packages/exercise.custom/1",
        )
        dao.installAndActivate(
            downloaded,
            ResourceActivationEntity(
                packageId = "exercise.custom",
                activeVersionCode = 1,
                activeLocationType = ResourceLocationType.DOWNLOADED,
                enabled = true,
            )
        )

        assertEquals(1, dao.resources.size)
        assertEquals(1, dao.activations.size)

        // When removing downloaded package
        dao.removeDownloadedPackage("exercise.custom")

        // Then both resource and activation are deleted
        assertTrue(dao.resources.isEmpty())
        assertTrue(dao.activations.isEmpty())
    }

    @Test
    fun testRemoveDownloadedPackageWithBundledVersionRevertsToBundled() = runBlocking {
        // Given bundled version 0 exists
        val bundled = InstalledResourceEntity(
            packageId = "edition.yangbojun",
            kind = ResourceKind.EDITION,
            name = "杨伯峻《论语译注》",
            versionCode = 0,
            versionName = "内置",
            locationType = ResourceLocationType.BUNDLED,
            rootPath = "",
        )
        dao.upsertResource(bundled)

        // And user updated to downloaded version 1
        val downloaded = InstalledResourceEntity(
            packageId = "edition.yangbojun",
            kind = ResourceKind.EDITION,
            name = "杨伯峻《论语译注》",
            versionCode = 1,
            versionName = "1.0",
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = "/packages/edition.yangbojun/1",
        )
        dao.installAndActivate(
            downloaded,
            ResourceActivationEntity(
                packageId = "edition.yangbojun",
                activeVersionCode = 1,
                activeLocationType = ResourceLocationType.DOWNLOADED,
                enabled = true,
            )
        )

        // Both bundled and downloaded exist in DB
        assertEquals(2, dao.resources.size)
        assertEquals(1, dao.activations.first { it.packageId == "edition.yangbojun" }.activeVersionCode)
        assertEquals("edition.yangbojun", dao.preference?.activeEditionPackageId)

        // When removing the downloaded package
        dao.removeDownloadedPackage("edition.yangbojun")

        // Then bundled version remains in DB
        assertEquals(1, dao.resources.size)
        val remaining = dao.resources.first()
        assertEquals(ResourceLocationType.BUNDLED, remaining.locationType)
        assertEquals(0, remaining.versionCode)

        // And activation is restored to bundled version!
        val act = dao.activations.first { it.packageId == "edition.yangbojun" }
        assertEquals(0, act.activeVersionCode)
        assertEquals(ResourceLocationType.BUNDLED, act.activeLocationType)
        assertTrue(act.enabled)

        // Active edition is preserved
        assertEquals("edition.yangbojun", dao.preference?.activeEditionPackageId)
    }

    @Test
    fun testRepresentativeSelectionLogic() {
        // Test UI deduplication: when a package has both bundled and downloaded versions
        val bundled = InstalledResourceEntity(
            packageId = "edition.yangbojun",
            kind = ResourceKind.EDITION,
            name = "杨伯峻《论语译注》",
            versionCode = 0,
            versionName = "内置",
            locationType = ResourceLocationType.BUNDLED,
            rootPath = "",
        )
        val downloaded = InstalledResourceEntity(
            packageId = "edition.yangbojun",
            kind = ResourceKind.EDITION,
            name = "杨伯峻《论语译注》",
            versionCode = 2,
            versionName = "2.0",
            locationType = ResourceLocationType.DOWNLOADED,
            rootPath = "/packages/edition.yangbojun/2",
        )
        val allVersions = listOf(bundled, downloaded)

        // When downloaded is active
        val activeEdition = downloaded
        val selectedActive = allVersions.firstOrNull {
            it.versionCode == activeEdition.versionCode && it.locationType == activeEdition.locationType
        }
        assertNotNull(selectedActive)
        assertEquals(2, selectedActive!!.versionCode)
        assertEquals(ResourceLocationType.DOWNLOADED, selectedActive.locationType)

        // When none is active, picks highest downloaded version
        val selectedDefault = allVersions.maxWithOrNull(
            compareBy<InstalledResourceEntity> { it.locationType == ResourceLocationType.DOWNLOADED }
                .thenBy { it.versionCode }
        )
        assertNotNull(selectedDefault)
        assertEquals(2, selectedDefault!!.versionCode)
    }
}
