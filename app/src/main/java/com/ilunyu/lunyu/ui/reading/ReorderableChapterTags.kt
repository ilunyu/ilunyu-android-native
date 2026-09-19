package com.ilunyu.lunyu.ui.reading

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.ui.tag.ReorderableItemTags

/** Compatibility wrapper around [ReorderableItemTags] for chapter screen */
@Composable
internal fun ReorderableChapterTags(
    chapterId: String,
    tags: List<TagEntity>,
    onAdd: () -> Unit,
    onNavigate: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    ReorderableItemTags(
        itemId = chapterId,
        tags = tags,
        onAdd = onAdd,
        onNavigate = onNavigate,
        onRemove = onRemove,
        onReorder = onReorder,
        modifier = modifier
    )
}
