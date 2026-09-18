package com.ilunyu.lunyu

import com.ilunyu.lunyu.data.db.ItemTagCrossRef
import com.ilunyu.lunyu.data.db.TagDao
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TargetType
import com.ilunyu.lunyu.data.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTagDao : TagDao {
    private val tags = mutableListOf<TagEntity>()
    private val itemTags = mutableListOf<ItemTagCrossRef>()

    private val tagsFlow = MutableStateFlow<List<TagEntity>>(emptyList())
    private val itemTagsFlow = MutableStateFlow<List<ItemTagCrossRef>>(emptyList())

    private fun emit() {
        tagsFlow.value = tags.toList()
        itemTagsFlow.value = itemTags.toList()
    }

    override fun getAllTagsFlow(): Flow<List<TagEntity>> = tagsFlow

    override suspend fun getTagById(id: String): TagEntity? = tags.find { it.id == id }

    override suspend fun getTagByName(name: String): TagEntity? = tags.find { it.name == name }

    override suspend fun insertTag(tag: TagEntity): Long {
        tags.add(tag)
        emit()
        return tags.size.toLong()
    }

    override suspend fun updateTag(tag: TagEntity) {
        val index = tags.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            tags[index] = tag
            emit()
        }
    }

    override suspend fun deleteTagEntity(tagId: String): Int {
        val removed = tags.removeAll { it.id == tagId }
        emit()
        return if (removed) 1 else 0
    }

    override suspend fun deleteItemTagsByTagId(tagId: String): Int {
        val count = itemTags.count { it.tagId == tagId }
        itemTags.removeAll { it.tagId == tagId }
        emit()
        return count
    }

    override fun getTagsForItemFlow(targetType: String, targetId: String): Flow<List<TagEntity>> {
        return itemTagsFlow.map { list ->
            val tagIds = list.filter { it.targetType == targetType && it.targetId == targetId }.map { it.tagId }.toSet()
            tags.filter { tagIds.contains(it.id) }
        }
    }

    override fun getAllItemTagsFlow(): Flow<List<ItemTagCrossRef>> = itemTagsFlow

    override suspend fun insertItemTag(crossRef: ItemTagCrossRef) {
        if (itemTags.none { it.tagId == crossRef.tagId && it.targetType == crossRef.targetType && it.targetId == crossRef.targetId }) {
            itemTags.add(crossRef)
            emit()
        }
    }

    override suspend fun deleteItemTag(tagId: String, targetType: String, targetId: String): Int {
        val removed = itemTags.removeAll { it.tagId == tagId && it.targetType == targetType && it.targetId == targetId }
        emit()
        return if (removed) 1 else 0
    }

    override fun getTargetIdsForTagFlow(tagId: String, targetType: String): Flow<List<String>> {
        return itemTagsFlow.map { list ->
            list.filter { it.tagId == tagId && it.targetType == targetType }.map { it.targetId }
        }
    }

    override suspend fun hasItemTag(tagId: String, targetType: String, targetId: String): Boolean {
        return itemTags.any { it.tagId == tagId && it.targetType == targetType && it.targetId == targetId }
    }

    override fun countItemsForTagFlow(tagId: String, targetType: String): Flow<Int> {
        return itemTagsFlow.map { list ->
            list.count { it.tagId == tagId && it.targetType == targetType }
        }
    }
}

class TagRepositoryTest {

    private lateinit var fakeDao: FakeTagDao
    private lateinit var repository: TagRepository

    @Before
    fun setUp() {
        fakeDao = FakeTagDao()
        repository = TagRepository(fakeDao)
    }

    @Test
    fun testCreateTagSuccessAndDuplicateCheck() = runBlocking {
        val createResult = repository.createTag("中庸")
        assertTrue(createResult.isSuccess)
        val tag = createResult.getOrThrow()
        assertEquals("中庸", tag.name)
        assertNotNull(tag.colorHex)

        // Duplicate check
        val duplicateResult = repository.createTag("中庸")
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is IllegalStateException)

        // Blank check
        val blankResult = repository.createTag("   ")
        assertTrue(blankResult.isFailure)
    }

    @Test
    fun testUpdateTag() = runBlocking {
        val tag1 = repository.createTag("修身").getOrThrow()
        val tag2 = repository.createTag("齐家").getOrThrow()

        // Rename tag1
        val updateSuccess = repository.updateTag(tag1.id, "格物", "#C04851")
        assertTrue(updateSuccess.isSuccess)
        val fetched = repository.getTagById(tag1.id)
        assertNotNull(fetched)
        assertEquals("格物", fetched!!.name)
        assertEquals("#C04851", fetched.colorHex)

        // Cannot rename to tag2's name (duplicate)
        val duplicateRename = repository.updateTag(tag1.id, "齐家", "#C04851")
        assertTrue(duplicateRename.isFailure)
    }

    @Test
    fun testToggleItemTagAndCounts() = runBlocking {
        val tag = repository.createTag("孝悌").getOrThrow()

        // Tag chapter 1
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        // Tag exercise 101
        repository.toggleItemTag(tag.id, TargetType.EXERCISE, "101")

        val tagsWithCounts = repository.allTagsWithCountsFlow.first()
        assertEquals(1, tagsWithCounts.size)
        val tagWithCount = tagsWithCounts.first()
        assertEquals("孝悌", tagWithCount.tag.name)
        assertEquals(1, tagWithCount.chapterCount)
        assertEquals(1, tagWithCount.exerciseCount)

        // Untag chapter 1
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        val updatedCounts = repository.allTagsWithCountsFlow.first()
        assertEquals(0, updatedCounts.first().chapterCount)
        assertEquals(1, updatedCounts.first().exerciseCount)
    }

    @Test
    fun testCascadeDeleteTag() = runBlocking {
        val tag = repository.createTag("治国").getOrThrow()
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        repository.toggleItemTag(tag.id, TargetType.EXERCISE, "201")

        // Delete tag
        repository.deleteTag(tag.id)

        assertNull(repository.getTagById(tag.id))
        val chapterIds = repository.getChapterIdsForTagFlow(tag.id).first()
        assertTrue(chapterIds.isEmpty())
        val exerciseIds = repository.getExerciseIdsForTagFlow(tag.id).first()
        assertTrue(exerciseIds.isEmpty())
    }
}
