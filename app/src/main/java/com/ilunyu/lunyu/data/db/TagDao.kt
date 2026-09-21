package com.ilunyu.lunyu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY sort_order ASC, created_at DESC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagEntity(tagId: String): Int

    @Query("DELETE FROM item_tags WHERE tag_id = :tagId")
    suspend fun deleteItemTagsByTagId(tagId: String): Int

    @Transaction
    suspend fun deleteTag(tagId: String) {
        deleteItemTagsByTagId(tagId)
        deleteTagEntity(tagId)
    }

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tags it ON t.id = it.tag_id
        WHERE it.target_type = :targetType AND it.target_id = :targetId
        ORDER BY it.sort_order ASC, it.created_at ASC
    """)
    fun getTagsForItemFlow(targetType: String, targetId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM item_tags")
    fun getAllItemTagsFlow(): Flow<List<ItemTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTag(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tags WHERE tag_id = :tagId AND target_type = :targetType AND target_id = :targetId")
    suspend fun deleteItemTag(tagId: String, targetType: String, targetId: String): Int

    @Query("SELECT target_id FROM item_tags WHERE tag_id = :tagId AND target_type = :targetType ORDER BY created_at DESC")
    fun getTargetIdsForTagFlow(tagId: String, targetType: String): Flow<List<String>>

    @Query("UPDATE OR IGNORE item_tags SET target_id = :newTargetId WHERE target_type = 'EXERCISE' AND target_id = :oldTargetId")
    suspend fun migrateExerciseId(oldTargetId: String, newTargetId: String): Int

    @Query("SELECT COUNT(*) > 0 FROM item_tags WHERE tag_id = :tagId AND target_type = :targetType AND target_id = :targetId")
    suspend fun hasItemTag(tagId: String, targetType: String, targetId: String): Boolean

    @Query("SELECT COUNT(*) FROM item_tags WHERE tag_id = :tagId AND target_type = :targetType")
    fun countItemsForTagFlow(tagId: String, targetType: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM item_tags WHERE tag_id = :tagId")
    suspend fun countAllItemsForTag(tagId: String): Int

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tag_id FROM item_tags)")
    suspend fun deleteOrphanTags(): Int

    @Query("SELECT MAX(sort_order) FROM item_tags WHERE target_type = :targetType AND target_id = :targetId")
    suspend fun getMaxSortOrderForItem(targetType: String, targetId: String): Int?

    @Query("UPDATE item_tags SET sort_order = :sortOrder WHERE tag_id = :tagId AND target_type = :targetType AND target_id = :targetId")
    suspend fun updateItemTagSortOrder(tagId: String, targetType: String, targetId: String, sortOrder: Int): Int

    @Transaction
    suspend fun reorderItemTags(targetType: String, targetId: String, orderedTagIds: List<String>) {
        orderedTagIds.forEachIndexed { index, tagId ->
            updateItemTagSortOrder(tagId, targetType, targetId, index)
        }
    }
}
