package com.zerospin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.zerospin.app.ui.components.rememberAttachPicker
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain

@Composable
fun FeedScreen(
    posts: List<Post>,
    onPublish: (String, List<Attachment>) -> Unit,
    onLike: (Int) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    val pick = rememberAttachPicker { pending = pending + it }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(12.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Новая запись…", color = Muted) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = hudFieldColors(),
                    shape = RoundedCornerShape(6.dp),
                )
                if (pending.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    AttachmentList(pending, onRemove = { i -> pending = pending.filterIndexed { idx, _ -> idx != i } })
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudButton(text = "+ Файл", onClick = pick, modifier = Modifier.weight(1f))
                    HudButton(
                        text = "Опубликовать",
                        onClick = {
                            if (draft.isNotBlank() || pending.isNotEmpty()) {
                                onPublish(draft.trim(), pending)
                                draft = ""
                                pending = emptyList()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (posts.isEmpty()) {
            item { HudEmptyState("ЛЕНТА ПУСТА", "Напишите первую запись круга") }
        }
        items(posts, key = { it.id }) { post ->
            PostCard(post = post, onLike = { onLike(post.id) })
        }
    }
}

@Composable
private fun PostCard(post: Post, onLike: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .hudPanel(cut = 12.dp),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Cyan))
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
            if (post.text.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(post.text, color = TextMain, fontSize = 15.sp)
            }
            if (post.attachments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                AttachmentList(post.attachments)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clickable(onClick = onLike).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (post.liked) "▲ ОТМЕЧЕНО · ${post.likes}" else "△ ОТМЕТИТЬ · ${post.likes}",
                    color = if (post.liked) Cyan else Muted,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}
