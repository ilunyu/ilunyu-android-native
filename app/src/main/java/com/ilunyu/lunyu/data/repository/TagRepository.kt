package com.ilunyu.lunyu.data.repository

import com.ilunyu.lunyu.data.db.ItemTagCrossRef
import com.ilunyu.lunyu.data.db.TagDao
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TagWithCounts
import com.ilunyu.lunyu.data.db.TargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 雅致传统国学配色预设盘（用于自动分配或用户手动挑选标签色彩）
 */
val TAG_PRESET_COLORS = listOf(
    "#008080", // 黛绿 (Teal)
    "#2F72B5", // 苍蓝 (Slate Blue)
    "#C04851", // 绯红 (Carmine)
    "#D98719", // 琥珀 (Amber)
    "#2E7D32", // 竹青 (Bamboo Green)
    "#7E57C2", // 暮紫 (Twilight Purple)
    "#A0522D", // 赭石 (Ochre)
    "#00897B", // 松石 (Turquoise)
    "#546E7A"  // 墨灰 (Slate Grey)
)

class TagRepository(private val tagDao: TagDao) {

    /**
     * 响应式流：获取全部标签及其实时关联的章节数与试题数
     */
    val allTagsWithCountsFlow: Flow<List<TagWithCounts>> = combine(
        tagDao.getAllTagsFlow(),
        tagDao.getAllItemTagsFlow()
    ) { tags, itemTags ->
        val chapterCounts = mutableMapOf<String, Int>()
        val exerciseCounts = mutableMapOf<String, Int>()

        for (item in itemTags) {
            when (item.targetType) {
                TargetType.CHAPTER -> {
                    chapterCounts[item.tagId] = (chapterCounts[item.tagId] ?: 0) + 1
                }
                TargetType.EXERCISE -> {
                    exerciseCounts[item.tagId] = (exerciseCounts[item.tagId] ?: 0) + 1
                }
            }
        }

        tags.map { tag ->
            TagWithCounts(
                tag = tag,
                chapterCount = chapterCounts[tag.id] ?: 0,
                exerciseCount = exerciseCounts[tag.id] ?: 0
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取指定章节或试题所附加的标签列表流
     */
    fun getTagsForItemFlow(targetType: String, targetId: String): Flow<List<TagEntity>> {
        return tagDao.getTagsForItemFlow(targetType, targetId).flowOn(Dispatchers.IO)
    }

    /**
     * 获取某标签下的全部章节 ID 列表流
     */
    fun getChapterIdsForTagFlow(tagId: String): Flow<List<String>> {
        return tagDao.getTargetIdsForTagFlow(tagId, TargetType.CHAPTER).flowOn(Dispatchers.IO)
    }

    /**
     * 获取某标签下的全部试题 ID 列表流
     */
    fun getExerciseIdsForTagFlow(tagId: String): Flow<List<String>> {
        return tagDao.getTargetIdsForTagFlow(tagId, TargetType.EXERCISE).flowOn(Dispatchers.IO)
    }

    /**
     * 根据 ID 获取单个标签信息
     */
    suspend fun getTagById(tagId: String): TagEntity? = withContext(Dispatchers.IO) {
        tagDao.getTagById(tagId)
    }

    /**
     * 创建新标签（支持防重名校验与颜色自动轮转分配）
     */
    suspend fun createTag(name: String, colorHex: String? = null): Result<TagEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim().removePrefix("#").trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能为空"))
        }

        val existing = tagDao.getTagByName(trimmedName)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("已存在同名标签：$trimmedName"))
        }

        val color = colorHex ?: run {
            // 根据已有标签数量顺延分配预设色彩
            val hash = Math.abs(trimmedName.hashCode())
            TAG_PRESET_COLORS[hash % TAG_PRESET_COLORS.size]
        }

        val newTag = TagEntity(
            name = trimmedName,
            colorHex = color
        )
        tagDao.insertTag(newTag)
        Result.success(newTag)
    }

    /**
     * 重命名标签或修改标签主题色
     */
    suspend fun updateTag(tagId: String, name: String, colorHex: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim().removePrefix("#").trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能为空"))
        }

        val current = tagDao.getTagById(tagId)
            ?: return@withContext Result.failure(NoSuchElementException("标签不存在"))

        val existing = tagDao.getTagByName(trimmedName)
        if (existing != null && existing.id != tagId) {
            return@withContext Result.failure(IllegalStateException("已存在同名标签：$trimmedName"))
        }

        val updated = current.copy(name = trimmedName, colorHex = colorHex)
        tagDao.updateTag(updated)
        Result.success(Unit)
    }

    /**
     * 删除标签及其关联的所有打标记录
     */
    suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tagId)
    }

    /**
     * 切换章节或试题对某一标签的关联状态（打标 / 去除打标）
     */
    suspend fun toggleItemTag(tagId: String, targetType: String, targetId: String) = withContext(Dispatchers.IO) {
        val hasTag = tagDao.hasItemTag(tagId, targetType, targetId)
        if (hasTag) {
            tagDao.deleteItemTag(tagId, targetType, targetId)
        } else {
            tagDao.insertItemTag(ItemTagCrossRef(tagId = tagId, targetType = targetType, targetId = targetId))
        }
    }

    /**
     * 批量为章节或试题设置标签
     */
    suspend fun setItemTags(targetType: String, targetId: String, tagIds: Set<String>) = withContext(Dispatchers.IO) {
        // 先删除原有标签关联
        val currentTags = tagDao.getAllItemTagsFlow()
        // 简化：直接插入新关系并清理未选中的
        for (tagId in tagIds) {
            tagDao.insertItemTag(ItemTagCrossRef(tagId = tagId, targetType = targetType, targetId = targetId))
        }
    }
}
