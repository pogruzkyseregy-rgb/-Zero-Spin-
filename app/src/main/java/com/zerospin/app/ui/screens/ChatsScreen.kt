package com.zerospin.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.AppSettings
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.QuickReactions
import com.zerospin.app.data.SUPPORT_CHAT_ID
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.rememberFilePicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.hud.HudAction
import com.zerospin.app.ui.hud.HudActionSheet
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(
    chats: List<Chat>,
    messages: Map<Int, List<ChatMessage>>,
    settings: AppSettings,
    openChatId: Int,
    onOpenChat: (Int) -> Unit,
    onCreateChat: () -> Unit,
    onSend: (chatId: Int, text: String, attachments: List<Attachment>, replyTo: Int?) -> Unit,
    onEdit: (chatId: Int, messageId: Int, text: String) -> Unit,
    onDelete: (chatId: Int, messageId: Int) -> Unit,
    onReact: (chatId: Int, messageId: Int, emoji: String) -> Unit,
    onTogglePin: (chatId: Int) -> Unit,
    onToggleMute: (chatId: Int) -> Unit,
    onDeleteChat: (chatId: Int) -> Unit,
) {
    val open = chats.firstOrNull { it.id == openChatId }
    if (open == null) {
        ChatList(
            chats = chats,
            messages = messages,
            onOpenChat = onOpenChat,
            onCreateChat = onCreateChat,
            onTogglePin = onTogglePin,
            onToggleMute = onToggleMute,
            onDeleteChat = onDeleteChat,
        )
    } else {
        Conversation(
            chat = open,
            messages = messages[open.id].orEmpty(),
            settings = settings,
            onBack = { onOpenChat(-1) },
            onSend = { text, attachments, replyTo -> onSend(open.id, text, attachments, replyTo) },
            onEdit = { id, text -> onEdit(open.id, id, text) },
            onDelete = { id -> onDelete(open.id, id) },
            onReact = { id, emoji -> onReact(open.id, id, emoji) },
        )
    }
}

// ---------------------------------------------------------------- Список чатов

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatList(
    chats: List<Chat>,
    messages: Map<Int, List<ChatMessage>>,
    onOpenChat: (Int) -> Unit,
    onCreateChat: () -> Unit,
    onTogglePin: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onDeleteChat: (Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuChat by remember { mutableStateOf<Chat?>(null) }

    val visible = chats
        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        .sortedByDescending { it.pinned } // закреплённые выше, порядок остальных сохраняется

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SearchField(query) { query = it } }
        item {
            HudButton(text = "+ Новый канал", onClick = onCreateChat, modifier = Modifier.fillMaxWidth())
        }
        if (visible.isEmpty()) {
            item {
                if (query.isBlank()) HudEmptyState("НЕТ КАНАЛОВ", "Создайте первый канал круга")
                else HudEmptyState("НИЧЕГО НЕ НАЙДЕНО", "Попробуйте другое название")
            }
        }
        items(visible, key = { it.id }) { chat ->
            val last = messages[chat.id].orEmpty().lastOrNull()
            ChatRow(
                chat = chat,
                last = last,
                onClick = { onOpenChat(chat.id) },
                onLongClick = { menuChat = chat },
            )
        }
    }

    menuChat?.let { chat ->
        val actions = buildList {
            add(HudAction(if (chat.pinned) "Открепить" else "Закрепить") { onTogglePin(chat.id) })
            add(HudAction(if (chat.muted) "Включить уведомления" else "Выключить уведомления") { onToggleMute(chat.id) })
            if (chat.id != SUPPORT_CHAT_ID) {
                add(HudAction("Удалить канал", danger = true) { onDeleteChat(chat.id) })
            }
        }
        HudActionSheet(title = chat.name, actions = actions, onDismiss = { menuChat = null })
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(line = Cyan.copy(alpha = 0.5f), fill = Bg, cut = 8.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudIcon(HudIconKind.PLUS, Muted, Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Поиск по чатам", color = Muted, fontSize = 14.sp)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 14.sp),
                cursorBrush = SolidColor(Cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Text("×", color = Muted, fontSize = 18.sp, modifier = Modifier.clickable { onChange("") }.padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(chat: Chat, last: ChatMessage?, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(cut = 12.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TargetRing(Modifier.size(44.dp)) {
            Text(chat.name.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.name.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.pinned) Text("◆", color = Violet, fontSize = 10.sp)
                if (chat.muted) Text("  ⊘", color = Muted, fontSize = 11.sp)
            }
            Text(
                text = previewOf(last),
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (last != null) {
            Spacer(Modifier.width(8.dp))
            Text(last.time, color = Muted, fontSize = 10.sp)
        }
    }
}

private fun previewOf(last: ChatMessage?): String {
    if (last == null) return "ожидание сигнала…"
    val body = last.text.ifBlank { "вложение" }
    return if (last.fromMe) "Вы: $body" else body
}

// ---------------------------------------------------------------- Переписка

private sealed class ListRow {
    data class Day(val label: String) : ListRow()
    data class Msg(val message: ChatMessage) : ListRow()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Conversation(
    chat: Chat,
    messages: List<ChatMessage>,
    settings: AppSettings,
    onBack: () -> Unit,
    onSend: (String, List<Attachment>, Int?) -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onReact: (Int, String) -> Unit,
) {
    BackHandler(onBack = onBack)

    val clipboard = LocalClipboardManager.current
    var draft by rememberSaveable(chat.id) { mutableStateOf("") }
    var pending by remember(chat.id) { mutableStateOf<List<Attachment>>(emptyList()) }
    var replyTo by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var editing by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var actionFor by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var reactionFor by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var showAttach by remember(chat.id) { mutableStateOf(false) }
    val pickPhoto = rememberPhotoPicker { pending = pending + it }
    val pickFile = rememberFilePicker { pending = pending + it }
    val listState = rememberLazyListState()

    val rows = remember(messages) { buildRows(messages) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(rows.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).hudPanel(cut = 9.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            TargetRing(Modifier.size(36.dp)) {
                Text(chat.name.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(chat.name.uppercase(), color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
                Text(
                    when {
                        chat.id == SUPPORT_CHAT_ID -> "поддержка · отвечает Аврора"
                        settings.showOnline -> "в сети"
                        else -> ""
                    },
                    color = Violet,
                    fontSize = 10.sp,
                )
            }
        }

        if (rows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                HudEmptyState("КАНАЛ ПУСТ", "Передайте первый сигнал")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { r ->
                    when (r) {
                        is ListRow.Day -> "day-${r.label}"
                        is ListRow.Msg -> "msg-${r.message.id}"
                    }
                }) { r ->
                    when (r) {
                        is ListRow.Day -> DayChip(r.label)
                        is ListRow.Msg -> Bubble(
                            message = r.message,
                            quoted = r.message.replyTo?.let { id -> messages.firstOrNull { it.id == id } },
                            showRead = settings.readReceipts,
                            onLongClick = { actionFor = r.message },
                            onReactionClick = { onReact(r.message.id, it) },
                        )
                    }
                }
            }
        }

        // Режим ответа или правки
        val mode = editing?.let { "ИЗМЕНЕНИЕ" to it.text } ?: replyTo?.let { "ОТВЕТ" to it.text.ifBlank { "вложение" } }
        if (mode != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).hudPanel(line = Violet, fill = Panel, cut = 8.dp).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(mode.first, color = Violet, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(mode.second, color = TextMain, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "×",
                    color = Red,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable {
                        editing = null
                        replyTo = null
                        draft = ""
                    }.padding(start = 8.dp),
                )
            }
        }

        if (pending.isNotEmpty()) {
            AttachmentList(pending, onRemove = { i -> pending = pending.filterIndexed { idx, _ -> idx != i } })
            Spacer(Modifier.height(6.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(46.dp).hudPanel(cut = 10.dp).clickable { showAttach = true },
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.PLUS, Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .hudPanel(line = Cyan.copy(alpha = 0.5f), fill = Bg, cut = 10.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (draft.isEmpty()) Text("Сообщение", color = Muted, fontSize = 14.sp)
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(Cyan),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .hudPanel(line = Cyan, fill = Cyan, cut = 10.dp)
                    .clickable {
                        val text = draft.trim()
                        val edit = editing
                        if (edit != null) {
                            if (text.isNotEmpty()) onEdit(edit.id, text)
                            editing = null
                        } else if (text.isNotEmpty() || pending.isNotEmpty()) {
                            onSend(text, pending, replyTo?.id)
                            pending = emptyList()
                            replyTo = null
                        } else {
                            return@clickable
                        }
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.SEND, Bg, Modifier.size(18.dp))
            }
        }
    }

    // Меню сообщения: ответ, копирование, реакция, правка и удаление
    actionFor?.let { m ->
        val actions = buildList {
            add(HudAction("Ответить") {
                replyTo = m
                editing = null
            })
            if (m.text.isNotEmpty()) {
                add(HudAction("Копировать") { clipboard.setText(AnnotatedString(m.text)) })
            }
            add(HudAction("Реакция") { reactionFor = m })
            if (m.fromMe) {
                add(HudAction("Изменить") {
                    editing = m
                    replyTo = null
                    draft = m.text
                })
                add(HudAction("Удалить", danger = true) { onDelete(m.id) })
            }
        }
        HudActionSheet(title = "Сообщение", actions = actions, onDismiss = { actionFor = null })
    }

    // Вложения: фото и файлы выбираются отдельно
    if (showAttach) {
        HudActionSheet(
            title = "Вложить",
            actions = listOf(
                HudAction("Фото и видео") { pickPhoto() },
                HudAction("Файл") { pickFile() },
            ),
            onDismiss = { showAttach = false },
        )
    }

    reactionFor?.let { m ->
        HudActionSheet(
            title = "Реакция",
            actions = QuickReactions.map { emoji -> HudAction(emoji) { onReact(m.id, emoji) } },
            onDismiss = { reactionFor = null },
        )
    }
}

private fun buildRows(messages: List<ChatMessage>): List<ListRow> {
    val out = mutableListOf<ListRow>()
    var lastDay = ""
    for (m in messages) {
        val day = dayKey(m.createdAt)
        if (day != lastDay) {
            out += ListRow.Day(dayLabel(m.createdAt))
            lastDay = day
        }
        out += ListRow.Msg(m)
    }
    return out
}

private fun dayKey(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
}

private fun dayLabel(millis: Long): String {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) "Сегодня" else SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru")).format(millis)
}

@Composable
private fun DayChip(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        Text(label.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    message: ChatMessage,
    quoted: ChatMessage?,
    showRead: Boolean,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
) {
    val mine = message.fromMe
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 290.dp)
                .hudPanel(
                    line = if (mine) Cyan else Violet,
                    fill = if (mine) Color(0xFF0B2A36) else Panel,
                    cut = 10.dp,
                )
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (quoted != null) {
                Text(
                    "↪ " + quoted.text.ifBlank { "вложение" },
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (message.text.isNotEmpty()) {
                Text(message.text, color = TextMain, fontSize = 14.sp)
            }
            if (message.attachments.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AttachmentList(message.attachments)
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    if (message.edited) "${message.time} · изм." else message.time,
                    color = Muted,
                    fontSize = 10.sp,
                )
                if (mine) {
                    val read = showRead && message.read
                    Text(
                        if (read) " ✓✓" else " ✓",
                        color = if (read) Cyan else Muted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        if (message.reactionCount > 0) {
            Row(
                Modifier
                    .padding(top = 2.dp)
                    .hudPanel(line = Violet, fill = Bg, cut = 6.dp)
                    .clickable { onReactionClick(message.myReaction ?: QuickReactions.first()) }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("${message.myReaction ?: "•"} ${message.reactionCount}", color = TextMain, fontSize = 12.sp)
            }
        }
    }
}
