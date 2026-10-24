package com.zerospin.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Comment
import com.zerospin.app.data.Post
import com.zerospin.app.data.Story
import com.zerospin.app.data.nowMillis
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.formatMessage
import com.zerospin.app.ui.components.rememberCoverPicker
import com.zerospin.app.ui.components.rememberFilePicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.components.rememberThumb
import com.zerospin.app.ui.hud.HudAction
import com.zerospin.app.ui.hud.HudActionSheet
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudMono
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.FluoriteColors
import com.zerospin.app.ui.theme.Magenta
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import com.zerospin.app.ui.theme.fluoriteBrush

private const val FEED_ALL = 0
private const val FEED_BOOKMARKS = 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    stories: List<Story>,
    currentAuthor: String,
    onPublish: (String, List<Attachment>) -> Unit,
    onLike: (Int) -> Unit,
    onRepost: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onComment: (postId: Int, text: String) -> Unit,
    onDeleteComment: (postId: Int, commentId: Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onAddStory: (uri: String, caption: String) -> Unit,
    onStorySeen: (Int) -> Unit,
    onDeleteStory: (Int) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var menuPost by remember { mutableStateOf<Post?>(null) }
    var filter by rememberSaveable { mutableIntStateOf(FEED_ALL) }
    var openComments by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var storyDraft by remember { mutableStateOf<Attachment?>(null) }
    var viewingStory by remember { mutableStateOf<Int?>(null) }

    val pickPhoto = rememberPhotoPicker { pending = pending + it }
    val pickFile = rememberFilePicker { pending = pending + it }
    val pickStory = rememberCoverPicker { storyDraft = it }

    // Непросмотренные истории первыми, внутри — новые раньше
    val orderedStories = stories.sortedWith(compareBy<Story> { it.seen }.thenByDescending { it.createdAt })
    val visible = if (filter == FEED_BOOKMARKS) posts.filter { it.bookmarked } else posts

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StoriesRow(
                stories = orderedStories,
                onAdd = pickStory,
                onOpen = { index -> viewingStory = index },
            )
        }
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
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudButton(text = "+ Фото", onClick = pickPhoto, modifier = Modifier.weight(1f))
                    HudButton(text = "+ Файл", onClick = pickFile, modifier = Modifier.weight(1f))
                    HudButton(
                        text = "Пуск",
                        onClick = {
                            if (draft.isNotBlank() || pending.isNotEmpty()) {
                                onPublish(draft.trim(), pending)
                                draft = ""
                                pending = emptyList()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedTab("ВСЕ · ${posts.size}", filter == FEED_ALL) { filter = FEED_ALL }
                FeedTab("ЗАКЛАДКИ · ${posts.count { it.bookmarked }}", filter == FEED_BOOKMARKS) { filter = FEED_BOOKMARKS }
            }
        }
        if (visible.isEmpty()) {
            item {
                if (filter == FEED_BOOKMARKS) {
                    HudEmptyState("ЗАКЛАДОК НЕТ", "Нажмите флажок под записью")
                } else {
                    HudEmptyState("ЛЕНТА ПУСТА", "Напишите первую запись круга")
                }
            }
        }
        items(visible, key = { it.id }) { post ->
            PostCard(
                post = post,
                original = post.repostOf?.let { id -> posts.firstOrNull { it.id == id } },
                me = currentAuthor,
                commentsOpen = post.id in openComments,
                onLike = { onLike(post.id) },
                onRepost = { onRepost(post.repostOf ?: post.id) },
                onBookmark = { onBookmark(post.id) },
                onToggleComments = {
                    openComments = if (post.id in openComments) openComments - post.id else openComments + post.id
                },
                onComment = { text -> onComment(post.id, text) },
                onDeleteComment = { id -> onDeleteComment(post.id, id) },
                onLongClick = { menuPost = post },
            )
        }
    }

    menuPost?.let { post ->
        val actions = buildList {
            add(HudAction(if (post.liked) "Убрать лайк" else "Поставить лайк") { onLike(post.id) })
            add(HudAction(if (post.reposted) "Отменить репост" else "Репост") { onRepost(post.id) })
            add(HudAction(if (post.bookmarked) "Убрать из закладок" else "В закладки") { onBookmark(post.id) })
            if (post.author == currentAuthor) {
                add(HudAction("Удалить запись", danger = true) { onDelete(post.id) })
            }
        }
        HudActionSheet(title = "Запись", actions = actions, onDismiss = { menuPost = null })
    }

    storyDraft?.let { photo ->
        StoryComposer(
            photo = photo,
            onPublish = { caption ->
                onAddStory(photo.uri, caption)
                storyDraft = null
            },
            onDismiss = { storyDraft = null },
        )
    }

    viewingStory?.let { start ->
        StoryViewer(
            stories = orderedStories,
            startIndex = start,
            me = currentAuthor,
            onSeen = onStorySeen,
            onDelete = onDeleteStory,
            onDismiss = { viewingStory = null },
        )
    }
}

@Composable
private fun FeedTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (active) Bg else Cyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .hudPanel(line = Cyan, fill = if (active) Cyan else Panel, cut = 7.dp, glow = active)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------- Истории

/** Полоса историй: кнопка «+» и кружки. Непросмотренные — в кольце флюорита. */
@Composable
private fun StoriesRow(stories: List<Story>, onAdd: () -> Unit, onOpen: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    Modifier
                        .size(60.dp)
                        .border(1.5.dp, Brush.sweepGradient(FluoriteColors + FluoriteColors.first()), CircleShape)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("МОМЕНТ", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        items(stories.size) { index ->
            val story = stories[index]
            val thumb = rememberThumb(story.uri, 240)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    Modifier
                        .size(60.dp)
                        .border(
                            2.dp,
                            if (story.seen) Brush.linearGradient(listOf(Muted, Muted)) else Brush.sweepGradient(FluoriteColors + FluoriteColors.first()),
                            CircleShape,
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Panel)
                        .clickable { onOpen(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumb != null) {
                        Image(thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    ago(story.createdAt),
                    color = if (story.seen) Muted else Mint,
                    fontSize = 9.sp,
                    fontFamily = HudMono,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun ago(createdAt: Long): String {
    val minutes = ((nowMillis() - createdAt) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "сейчас"
        minutes < 60 -> "$minutes мин"
        else -> "${minutes / 60} ч"
    }
}

@Composable
private fun StoryComposer(photo: Attachment, onPublish: (String) -> Unit, onDismiss: () -> Unit) {
    var caption by remember { mutableStateOf("") }
    val preview = rememberThumb(photo.uri, 900)
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().hudPanel(cut = 14.dp, glow = true).padding(16.dp)) {
            Text("НОВЫЙ МОМЕНТ · 24 ЧАСА", color = Violet, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (preview != null) {
                Image(
                    preview,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp).border(1.dp, Cyan.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(10.dp))
            }
            HudTextField(
                value = caption,
                onValueChange = { caption = it },
                label = "Подпись",
                placeholder = "Необязательно",
                maxLength = 120,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.weight(1f))
                HudButton(text = "Опубликовать", onClick = { onPublish(caption.trim()) }, modifier = Modifier.weight(1f), primary = true)
            }
        }
    }
}

/**
 * Просмотр историй на весь экран: полоски прогресса сверху, автопереход через 5 секунд,
 * нажатие слева — назад, справа — вперёд.
 */
@Composable
private fun StoryViewer(
    stories: List<Story>,
    startIndex: Int,
    me: String,
    onSeen: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var index by remember { mutableIntStateOf(startIndex) }
    val progress = remember { Animatable(0f) }
    val story = stories.getOrNull(index)

    LaunchedEffect(index, story?.id) {
        if (story == null) {
            onDismiss()
            return@LaunchedEffect
        }
        onSeen(story.id)
        progress.snapTo(0f)
        progress.animateTo(1f, tween(5000, easing = LinearEasing))
        if (index < stories.size - 1) index += 1 else onDismiss()
    }

    if (story == null) return
    val image = rememberThumb(story.uri, 1600)
    val noRipple = remember { MutableInteractionSource() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            // Зоны нажатия: треть слева — назад, остальное — вперёд
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = noRipple, indication = null) {
                        if (index > 0) index -= 1
                    },
                )
                Box(
                    Modifier.weight(2f).fillMaxHeight().clickable(interactionSource = noRipple, indication = null) {
                        if (index < stories.size - 1) index += 1 else onDismiss()
                    },
                )
            }
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stories.indices.forEach { i ->
                        val fill = when {
                            i < index -> 1f
                            i == index -> progress.value
                            else -> 0f
                        }
                        Box(Modifier.weight(1f).height(3.dp).background(Muted.copy(alpha = 0.4f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(fluoriteBrush()))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(story.author.uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(ago(story.createdAt), color = Muted, fontSize = 11.sp, fontFamily = HudMono)
                    Spacer(Modifier.weight(1f))
                    if (story.author == me) {
                        Text(
                            "УДАЛИТЬ",
                            color = Red,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.clickable { onDelete(story.id) }.padding(8.dp),
                        )
                    }
                    Text("✕", color = TextMain, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                }
            }
            if (story.caption.isNotBlank()) {
                Text(
                    story.caption,
                    color = TextMain,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Bg)))
                        .padding(horizontal = 20.dp, vertical = 36.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Записи

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostCard(
    post: Post,
    original: Post?,
    me: String,
    commentsOpen: Boolean,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onBookmark: () -> Unit,
    onToggleComments: () -> Unit,
    onComment: (String) -> Unit,
    onDeleteComment: (Int) -> Unit,
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
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isRepost) Brush.verticalGradient(listOf(Magenta, Violet)) else Brush.verticalGradient(FluoriteColors)),
        )
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
                Text(post.time, color = Muted, fontSize = 10.sp, fontFamily = HudMono)
            }
            if (isRepost) {
                Text("⟲ РЕПОСТ", color = Magenta, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            if (post.text.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(formatMessage(post.text), color = TextMain, fontSize = 15.sp)
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
                    activeColor = Magenta,
                    onClick = onLike,
                )
                Spacer(Modifier.width(16.dp))
                ActionChip(
                    label = "${post.comments.size}",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    active = commentsOpen,
                    onClick = onToggleComments,
                )
                if (!isRepost) {
                    Spacer(Modifier.width(16.dp))
                    ActionChip(
                        label = "${post.reposts}",
                        icon = Icons.Filled.Repeat,
                        active = post.reposted,
                        activeColor = Mint,
                        onClick = onRepost,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (post.bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Закладка",
                    tint = if (post.bookmarked) Violet else Muted,
                    modifier = Modifier.size(22.dp).clickable(onClick = onBookmark),
                )
            }
            if (commentsOpen) {
                Spacer(Modifier.height(8.dp))
                CommentsBlock(post.comments, me, onComment, onDeleteComment)
            }
        }
    }
}

@Composable
private fun CommentsBlock(
    comments: List<Comment>,
    me: String,
    onComment: (String) -> Unit,
    onDeleteComment: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Bg.copy(alpha = 0.6f))
            .border(1.dp, Violet.copy(alpha = 0.35f))
            .padding(10.dp),
    ) {
        if (comments.isEmpty()) {
            Text("Комментариев пока нет", color = Muted, fontSize = 12.sp)
        }
        comments.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.author, color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text(c.time, color = Muted, fontSize = 9.sp, fontFamily = HudMono)
                    }
                    Text(formatMessage(c.text), color = TextMain, fontSize = 13.sp)
                }
                if (c.author == me) {
                    Text("×", color = Muted, fontSize = 16.sp, modifier = Modifier.clickable { onDeleteComment(c.id) }.padding(start = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .hudPanel(line = Cyan.copy(alpha = 0.5f), fill = Bg, cut = 6.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (text.isEmpty()) Text("Комментарий…", color = Muted, fontSize = 13.sp)
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    textStyle = TextStyle(color = TextMain, fontSize = 13.sp),
                    cursorBrush = SolidColor(Cyan),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "ОТПР.",
                color = if (text.isBlank()) Muted else Mint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable {
                    if (text.isNotBlank()) {
                        onComment(text.trim())
                        text = ""
                    }
                }.padding(6.dp),
            )
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
            Text(formatMessage(original.text), color = TextMain, fontSize = 13.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        if (original.attachments.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            AttachmentList(original.attachments)
        }
    }
}

/** Кнопка под записью: иконка и число. Активная окрашивается своим цветом. */
@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color = Cyan,
    onClick: () -> Unit,
) {
    val tint = if (active) activeColor else Muted
    Row(
        Modifier.clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = tint,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
        )
    }
}
