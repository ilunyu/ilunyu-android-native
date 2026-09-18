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

    override suspend fun countAllItemsForTag(tagId: String): Int {
        return itemTags.count { it.tagId == tagId }
    }

    override suspend fun deleteOrphanTags(): Int {
        val activeTagIds = itemTags.map { it.tagId }.toSet()
        val count = tags.count { !activeTagIds.contains(it.id) }
        tags.removeAll { !activeTagIds.contains(it.id) }
        emit()
        return count
    }

    override suspend fun getMaxSortOrderForItem(targetType: String, targetId: String): Int? {
        return itemTags.filter { it.targetType == targetType && it.targetId == targetId }.maxOfOrNull { it.sortOrder }
    }

    override suspend fun updateItemTagSortOrder(tagId: String, targetType: String, targetId: String, sortOrder: Int): Int {
        val index = itemTags.indexOfFirst { it.tagId == tagId && it.targetType == targetType && it.targetId == targetId }
        if (index >= 0) {
            itemTags[index] = itemTags[index].copy(sortOrder = sortOrder)
            emit()
            return 1
        }
        return 0
    }

    override suspend fun reorderItemTags(targetType: String, targetId: String, orderedTagIds: List<String>) {
        orderedTagIds.forEachIndexed { index, tagId ->
            updateItemTagSortOrder(tagId, targetType, targetId, index)
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
        // 规则 3：用户未设置颜色和图标时，均为 null（展示为最普通的纯文本 label）
        assertNull(tag.colorHex)
        assertNull(tag.icon)

        // Duplicate check
        val duplicateResult = repository.createTag("中庸")
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is IllegalStateException)

        // Blank check
        val blankResult = repository.createTag("   ")
        assertTrue(blankResult.isFailure)

        // Length > 10 check
        val longResult = repository.createTag("这是一段超过十个字符的标签名称")
        assertTrue(longResult.isFailure)

        // 规则 3：颜色与图标二选一（互斥）。若同时传入，优先保留图标，颜色置 null
        val iconResult = repository.createTag("仁者爱人", "#C04851", "star")
        assertTrue(iconResult.isSuccess)
        val iconTag = iconResult.getOrThrow()
        assertEquals("star", iconTag.icon)
        assertNull(iconTag.colorHex)

        // 仅设置颜色时，icon 为 null
        val colorResult = repository.createTag("克己复礼", "#2F72B5", null)
        assertTrue(colorResult.isSuccess)
        val colorTag = colorResult.getOrThrow()
        assertEquals("#2F72B5", colorTag.colorHex)
        assertNull(colorTag.icon)
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
        assertNull(fetched.icon)

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

        // Untag chapter 1 (still has exercise 101, so not deleted)
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        val updatedCounts = repository.allTagsWithCountsFlow.first()
        assertEquals(0, updatedCounts.first().chapterCount)
        assertEquals(1, updatedCounts.first().exerciseCount)

        // 规则 4：Untag exercise 101 (关联全无，标签自动删除)
        repository.toggleItemTag(tag.id, TargetType.EXERCISE, "101")
        assertNull(repository.getTagById(tag.id))
        val emptyList = repository.allTagsWithCountsFlow.first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun testOrphanTagAutoDeleted() = runBlocking {
        val tag = repository.createTag("学而").getOrThrow()
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        assertNotNull(repository.getTagById(tag.id))

        // 解绑后立即触发孤儿删除
        repository.toggleItemTag(tag.id, TargetType.CHAPTER, "1")
        assertNull(repository.getTagById(tag.id))
    }

    @Test
    fun testReorderItemTags() = runBlocking {
        val tagA = repository.createTag("标签A").getOrThrow()
        val tagB = repository.createTag("标签B").getOrThrow()
        val tagC = repository.createTag("标签C").getOrThrow()

        // 依次绑定到章节 1
        repository.toggleItemTag(tagA.id, TargetType.CHAPTER, "1")
        repository.toggleItemTag(tagB.id, TargetType.CHAPTER, "1")
        repository.toggleItemTag(tagC.id, TargetType.CHAPTER, "1")

        // 重排为 C, A, B
        repository.reorderItemTags(TargetType.CHAPTER, "1", listOf(tagC.id, tagA.id, tagB.id))

        // 验证 FakeTagDao 中排序位次
        val maxOrder = fakeDao.getMaxSortOrderForItem(TargetType.CHAPTER, "1")
        assertEquals(2, maxOrder)
    }
}
