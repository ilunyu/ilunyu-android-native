package com.ilunyu.lunyu.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 标签实体：记录用户自定义标签名称、代表色彩及展示排序
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null, // 代表色彩：自选或空
    @ColumnInfo(name = "icon")
    val icon: String? = null, // 代表图标：star, person, lightbulb, question_answer
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 目标类型：章节 (CHAPTER) 或 试题 (EXERCISE)
 */
object TargetType {
    const val CHAPTER = "CHAPTER"
    const val EXERCISE = "EXERCISE"
}

/**
 * 章节/试题与标签的多对多关联关系表
 */
@Entity(
    tableName = "item_tags",
    primaryKeys = ["tag_id", "target_type", "target_id"],
    indices = [
        Index(value = ["tag_id"]),
        Index(value = ["target_type", "target_id"])
    ]
)
data class ItemTagCrossRef(
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    @ColumnInfo(name = "target_type")
    val targetType: String,
    @ColumnInfo(name = "target_id")
    val targetId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 标签及其关联的章节数与试题数聚合数据模型
 */
data class TagWithCounts(
    val tag: TagEntity,
    val chapterCount: Int,
    val exerciseCount: Int
) {
    val totalCount: Int get() = chapterCount + exerciseCount
}
