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
     * 创建新标签：
     * - 名称去前后空格、去 # 前缀、校验空与长度（<=10）
     * - 颜色与图标二选一（互斥）；若用户均未设置，则均为 null（普通纯文本 label）
     */
    suspend fun createTag(
        name: String,
        colorHex: String? = null,
        icon: String? = null
    ): Result<TagEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim().removePrefix("#").trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能为空"))
        }
        if (trimmedName == "收藏" || trimmedName == "全部收藏") {
            return@withContext Result.failure(IllegalArgumentException("不能使用系统预留名称“$trimmedName”"))
        }
        if (trimmedName.length > 10) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能超过10个字符"))
        }

        val existing = tagDao.getTagByName(trimmedName)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("已存在同名标签：$trimmedName"))
        }

        // 颜色与图标只能二选一：若设置了图标，则颜色为 null；若设置了颜色，则图标为 null；若均未设置，则均为 null
        val finalIcon: String?
        val finalColorHex: String?
        if (!icon.isNullOrBlank()) {
            finalIcon = icon
            finalColorHex = null
        } else if (!colorHex.isNullOrBlank()) {
            finalIcon = null
            finalColorHex = colorHex
        } else {
            finalIcon = null
            finalColorHex = null
        }

        val newTag = TagEntity(
            name = trimmedName,
            colorHex = finalColorHex,
            icon = finalIcon
        )
        tagDao.insertTag(newTag)
        Result.success(newTag)
    }

    /**
     * 重命名标签或修改标签主题色与图标（同样严格保持颜色与图标二选一）
     */
    suspend fun updateTag(
        tagId: String,
        name: String,
        colorHex: String? = null,
        icon: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim().removePrefix("#").trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能为空"))
        }
        if (trimmedName == "收藏" || trimmedName == "全部收藏") {
            return@withContext Result.failure(IllegalArgumentException("不能使用系统预留名称“$trimmedName”"))
        }
        if (trimmedName.length > 10) {
            return@withContext Result.failure(IllegalArgumentException("标签名称不能超过10个字符"))
        }

        val current = tagDao.getTagById(tagId)
            ?: return@withContext Result.failure(NoSuchElementException("标签不存在"))

        val existing = tagDao.getTagByName(trimmedName)
        if (existing != null && existing.id != tagId) {
            return@withContext Result.failure(IllegalStateException("已存在同名标签：$trimmedName"))
        }

        val finalIcon: String?
        val finalColorHex: String?
        if (!icon.isNullOrBlank()) {
            finalIcon = icon
            finalColorHex = null
        } else if (!colorHex.isNullOrBlank()) {
            finalIcon = null
            finalColorHex = colorHex
        } else {
            finalIcon = null
            finalColorHex = null
        }

        val updated = current.copy(name = trimmedName, colorHex = finalColorHex, icon = finalIcon)
        tagDao.updateTag(updated)
        Result.success(Unit)
    }

    /**
     * 创建标签并立即与目标章节/试题关联（新标签排在最后）
     */
    suspend fun createAndAttachTag(
        name: String,
        colorHex: String? = null,
        icon: String? = null,
        targetType: String,
        targetId: String
    ): Result<TagEntity> = withContext(Dispatchers.IO) {
        val result = createTag(name, colorHex, icon)
        result.onSuccess { newTag ->
            val maxOrder = tagDao.getMaxSortOrderForItem(targetType, targetId) ?: -1
            tagDao.insertItemTag(
                ItemTagCrossRef(
                    tagId = newTag.id,
                    targetType = targetType,
                    targetId = targetId,
                    sortOrder = maxOrder + 1
                )
            )
        }
        result
    }

    /**
     * 删除标签及其关联的所有打标记录
     */
    suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tagId)
    }

    /**
     * 切换章节或试题对某一标签的关联状态（打标 / 去除打标）
     * 规则：
     * - 如果解绑后该标签所有关联的章节和试题都没了（计数为 0），则自动彻底删除该标签！
     * - 如果新增打标，默认排在末尾（sortOrder = maxOrder + 1）
     */
    suspend fun toggleItemTag(tagId: String, targetType: String, targetId: String) = withContext(Dispatchers.IO) {
        val hasTag = tagDao.hasItemTag(tagId, targetType, targetId)
        if (hasTag) {
            tagDao.deleteItemTag(tagId, targetType, targetId)
            val remainingCount = tagDao.countAllItemsForTag(tagId)
            if (remainingCount == 0) {
                tagDao.deleteTagEntity(tagId)
            }
        } else {
            val maxOrder = tagDao.getMaxSortOrderForItem(targetType, targetId) ?: -1
            tagDao.insertItemTag(
                ItemTagCrossRef(
                    tagId = tagId,
                    targetType = targetType,
                    targetId = targetId,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    /**
     * 为指定章节或试题下的标签重排顺序
     */
    suspend fun reorderItemTags(targetType: String, targetId: String, orderedTagIds: List<String>) = withContext(Dispatchers.IO) {
        tagDao.reorderItemTags(targetType, targetId, orderedTagIds)
    }

    /**
     * 清理所有无关联章节和试题的孤儿标签
     */
    suspend fun cleanupOrphanTags(): Int = withContext(Dispatchers.IO) {
        tagDao.deleteOrphanTags()
    }

    /**
     * 批量为章节或试题设置标签
     */
    suspend fun setItemTags(targetType: String, targetId: String, tagIds: Set<String>) = withContext(Dispatchers.IO) {
        var order = 0
        for (tagId in tagIds) {
            tagDao.insertItemTag(ItemTagCrossRef(tagId = tagId, targetType = targetType, targetId = targetId, sortOrder = order++))
        }
    }

    /**
     * 迁移旧版本试题规范 ID
     */
    suspend fun migrateLegacyExerciseIds() = withContext(Dispatchers.IO) {
        UserPreferencesRepository.LEGACY_EXERCISE_ID_MAP.forEach { (oldId, newId) ->
            tagDao.migrateExerciseId(oldId, newId)
        }
    }
}
