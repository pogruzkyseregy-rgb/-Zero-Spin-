package com.zerospin.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Post
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.rememberFilePicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.hud.HudAction
import com.zerospin.app.ui.hud.HudActionSheet
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    currentAuthor: String,
    onPublish: (String, List<Attachment>) -> Unit,
    onLike: (Int) -> Unit,
    onRepost: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var menuPost by remember { mutableStateOf<Post?>(null) }
    val pickPhoto = rememberPhotoPicker { pending = pending + it }
    val pickFile = rememberFilePicker { pending = pending + it }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(12.dp)) {
                HudTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = "Новая запись",
                    placeholder = "Что нового в круге?",
                    singleLine = false,
                    minLines = 2,
                    maxLength = 1000,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pending.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    AttachmentList(pending, onRemove = { i -> pending = pending.filterIndexed { idx, _ -> idx != i } })
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudButton(text = "+ Фото", onClick = pickPhoto, modifier = Modifier.weight(1f))
                    HudButton(text = "+ Файл", onClick = pickFile, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                HudButton(
                    text = "Опубликовать",
                    onClick = {
                        if (draft.isNotBlank() || pending.isNotEmpty()) {
                            onPublish(draft.trim(), pending)
                            draft = ""
                            pending = emptyList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                )
            }
        }
        if (posts.isEmpty()) {
            item { HudEmptyState("ЛЕНТА ПУСТА", "Напишите первую запись круга") }
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                original = post.repostOf?.let { id -> posts.firstOrNull { it.id == id } },
                onLike = { onLike(post.id) },
                onRepost = { onRepost(post.repostOf ?: post.id) },
                onLongClick = { menuPost = post },
            )
        }
    }

    menuPost?.let { post ->
        val actions = buildList {
            add(HudAction(if (post.liked) "Убрать лайк" else "Поставить лайк") { onLike(post.id) })
            add(HudAction(if (post.reposted) "Отменить репост" else "Репост") { onRepost(post.id) })
            if (post.author == currentAuthor) {
                add(HudAction("Удалить запись", danger = true) { onDelete(post.id) })
            }
        }
        HudActionSheet(title = "Запись", actions = actions, onDismiss = { menuPost = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostCard(
    post: Post,
    original: Post?,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onLongClick: () -> Unit,
) {
    val isRepost = post.repostOf != null
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .hudPanel(cut = 12.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (isRepost) Red else Cyan))
        Column(Modifier.weight(1f).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(post.time, color = Muted, fontSize = 10.sp)
            }
            if (isRepost) {
                Text("⟲ РЕПОСТ", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            if (post.text.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(post.text, color = TextMain, fontSize = 15.sp)
            }
            if (post.attachments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                AttachmentList(post.attachments)
            }
            if (isRepost) {
                Spacer(Modifier.height(8.dp))
                OriginalCard(original)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionChip(
                    label = "${post.likes}",
                    icon = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    active = post.liked,
                    onClick = onLike,
                )
                Spacer(Modifier.width(18.dp))
                if (!isRepost) {
                    ActionChip(
                        label = "${post.reposts}",
                        icon = Icons.Filled.Repeat,
                        active = post.reposted,
                        onClick = onRepost,
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalCard(original: Post?) {
    Column(Modifier.fillMaxWidth().hudPanel(line = Muted, cut = 8.dp).padding(10.dp)) {
        if (original == null) {
            Text("Исходная запись удалена", color = Muted, fontSize = 12.sp)
            return@Column
        }
        Text(original.author.uppercase(), color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
        if (original.text.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(original.text, color = TextMain, fontSize = 13.sp)
        }
        if (original.attachments.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            AttachmentList(original.attachments)
        }
    }
}

/** Кнопка под записью: иконка и число. Активная окрашивается в циан. */
@Composable
private fun ActionChip(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) Cyan else Muted
    Row(
        Modifier.clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = tint,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
        )
    }
}
