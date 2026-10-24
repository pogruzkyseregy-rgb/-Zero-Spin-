package com.zerospin.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.zerospin.app.data.AttachKind
import com.zerospin.app.ui.components.VoiceRecorder
import com.zerospin.app.ui.components.formatDuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zerospin.app.data.AppSettings
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.QuickReactions
import com.zerospin.app.data.SAVED_CHAT_ID
import com.zerospin.app.data.SUPPORT_CHAT_ID
import com.zerospin.app.data.TtlOptions
import com.zerospin.app.data.ttlLabel
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.firstUrl
import com.zerospin.app.ui.components.formatMessage
import com.zerospin.app.ui.components.plainText
import com.zerospin.app.ui.components.rememberFilePicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.hud.HudAction
import com.zerospin.app.ui.hud.HudActionSheet
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudMono
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.PulseDot
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Magenta
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.PanelHi
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** Эмодзи для панели в поле ввода. */
private val EmojiPanel = listOf(
    "😀", "😂", "🥲", "😍", "😘", "😎", "🤔", "😴", "😭", "😡",
    "👍", "👎", "👏", "🙏", "💪", "🫡", "👀", "🔥", "✨", "⚡",
    "💜", "💙", "💚", "🖤", "🦊", "🌌", "🧬", "⚛️", "🔮", "💎",
    "🌙", "⭐", "🎧", "🎮", "☕", "🍕", "🎉", "✅", "❌", "❓",
)

@Composable
fun ChatsScreen(
    chats: List<Chat>,
    messages: Map<Int, List<ChatMessage>>,
    settings: AppSettings,
    openChatId: Int,
    typingChatId: Int?,
    onOpenChat: (Int) -> Unit,
    onCreateChat: (String) -> Unit,
    onSend: (chatId: Int, text: String, attachments: List<Attachment>, replyTo: Int?) -> Unit,
    onEdit: (chatId: Int, messageId: Int, text: String) -> Unit,
    onDelete: (chatId: Int, messageId: Int) -> Unit,
    onReact: (chatId: Int, messageId: Int, emoji: String) -> Unit,
    onForward: (fromChat: Int, messageId: Int, toChat: Int) -> Unit,
    onPinMessage: (chatId: Int, messageId: Int?) -> Unit,
    onDraft: (chatId: Int, text: String) -> Unit,
    onTogglePin: (chatId: Int) -> Unit,
    onToggleMute: (chatId: Int) -> Unit,
    onToggleArchive: (chatId: Int) -> Unit,
    onSetTtl: (chatId: Int, seconds: Int) -> Unit,
    onDeleteChat: (chatId: Int) -> Unit,
) {
    val open = chats.firstOrNull { it.id == openChatId }
    if (open == null) {
        ChatList(
            chats = chats,
            messages = messages,
            typingChatId = typingChatId,
            onOpenChat = onOpenChat,
            onCreateChat = onCreateChat,
            onTogglePin = onTogglePin,
            onToggleMute = onToggleMute,
            onToggleArchive = onToggleArchive,
            onSetTtl = onSetTtl,
            onDeleteChat = onDeleteChat,
        )
    } else {
        Conversation(
            chat = open,
            allChats = chats,
            messages = messages[open.id].orEmpty(),
            settings = settings,
            typing = typingChatId == open.id,
            onBack = { onOpenChat(-1) },
            onSend = { text, attachments, replyTo -> onSend(open.id, text, attachments, replyTo) },
            onEdit = { id, text -> onEdit(open.id, id, text) },
            onDelete = { id -> onDelete(open.id, id) },
            onReact = { id, emoji -> onReact(open.id, id, emoji) },
            onForward = { id, to -> onForward(open.id, id, to) },
            onPinMessage = { id -> onPinMessage(open.id, id) },
            onDraft = { text -> onDraft(open.id, text) },
            onSetTtl = { seconds -> onSetTtl(open.id, seconds) },
        )
    }
}

// ---------------------------------------------------------------- Список чатов

private const val FILTER_ALL = 0
private const val FILTER_UNREAD = 1
private const val FILTER_ARCHIVE = 2

@Composable
private fun ChatList(
    chats: List<Chat>,
    messages: Map<Int, List<ChatMessage>>,
    typingChatId: Int?,
    onOpenChat: (Int) -> Unit,
    onCreateChat: (String) -> Unit,
    onTogglePin: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onToggleArchive: (Int) -> Unit,
    onSetTtl: (Int, Int) -> Unit,
    onDeleteChat: (Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(FILTER_ALL) }
    var menuChat by remember { mutableStateOf<Chat?>(null) }
    var ttlChat by remember { mutableStateOf<Chat?>(null) }
    var creating by remember { mutableStateOf(false) }

    val q = query.trim()
    val archivedCount = chats.count { it.archived }
    val visible = chats
        .filter { if (filter == FILTER_ARCHIVE) it.archived else !it.archived }
        .filter { filter != FILTER_UNREAD || it.unread > 0 }
        .filter { chat ->
            // Поиск по названию и по тексту сообщений
            q.isEmpty() || chat.name.contains(q, ignoreCase = true) ||
                messages[chat.id].orEmpty().any { plainText(it.text).contains(q, ignoreCase = true) }
        }
        .sortedByDescending { it.pinned } // закреплённые выше, порядок остальных сохраняется

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SearchField(query, "Поиск по чатам и сообщениям") { query = it } }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("ВСЕ", filter == FILTER_ALL) { filter = FILTER_ALL }
                FilterChip("НЕПРОЧИТАННЫЕ", filter == FILTER_UNREAD) { filter = FILTER_UNREAD }
                FilterChip("АРХИВ · $archivedCount", filter == FILTER_ARCHIVE) { filter = FILTER_ARCHIVE }
                FilterChip("+ КАНАЛ", false) { creating = true }
            }
        }
        if (visible.isEmpty()) {
            item {
                when {
                    q.isNotEmpty() -> HudEmptyState("НИЧЕГО НЕ НАЙДЕНО", "Попробуйте другое слово")
                    filter == FILTER_ARCHIVE -> HudEmptyState("АРХИВ ПУСТ", "Долгое нажатие на чат → В архив")
                    filter == FILTER_UNREAD -> HudEmptyState("ВСЁ ПРОЧИТАНО", "Новых сигналов нет")
                    else -> HudEmptyState("НЕТ КАНАЛОВ", "Создайте первый канал круга")
                }
            }
        }
        items(visible, key = { it.id }) { chat ->
            val last = messages[chat.id].orEmpty().lastOrNull()
            ChatRow(
                chat = chat,
                last = last,
                typing = typingChatId == chat.id,
                onClick = { onOpenChat(chat.id) },
                onLongClick = { menuChat = chat },
            )
        }
    }

    menuChat?.let { chat ->
        val builtIn = chat.id == SUPPORT_CHAT_ID || chat.id == SAVED_CHAT_ID
        val actions = buildList {
            add(HudAction(if (chat.pinned) "Открепить" else "Закрепить") { onTogglePin(chat.id) })
            add(HudAction(if (chat.muted) "Включить уведомления" else "Без звука") { onToggleMute(chat.id) })
            add(HudAction(if (chat.archived) "Вернуть из архива" else "В архив") { onToggleArchive(chat.id) })
            add(HudAction("Исчезающие сообщения · ${ttlLabel(chat.ttlSeconds)}") { ttlChat = chat })
            if (!builtIn) {
                add(HudAction("Удалить канал", danger = true) { onDeleteChat(chat.id) })
            }
        }
        HudActionSheet(title = chat.name, actions = actions, onDismiss = { menuChat = null })
    }

    ttlChat?.let { chat ->
        TtlSheet(chat, onSet = { onSetTtl(chat.id, it) }, onDismiss = { ttlChat = null })
    }

    if (creating) {
        NewChatDialog(onCreate = { onCreateChat(it) }, onDismiss = { creating = false })
    }
}

@Composable
private fun TtlSheet(chat: Chat, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    HudActionSheet(
        title = "Исчезающие сообщения",
        actions = TtlOptions.map { (seconds, label) ->
            HudAction(if (seconds == chat.ttlSeconds) "● $label" else label) { onSet(seconds) }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
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

@Composable
private fun NewChatDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().hudPanel(cut = 14.dp, glow = true).padding(16.dp)) {
            Text("НОВЫЙ КАНАЛ", color = Violet, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HudTextField(
                value = name,
                onValueChange = { name = it },
                label = "Название",
                placeholder = "Например: Ночной эфир",
                maxLength = 32,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.weight(1f))
                HudButton(
                    text = "Создать",
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name.trim())
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    primary = true,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, hint: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(line = Cyan.copy(alpha = 0.5f), fill = Bg, cut = 8.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudIcon(HudIconKind.SEARCH, Muted, Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, color = Muted, fontSize = 14.sp)
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

/** Значок канала: у Авроры — кольцо с лисьими ушками, у Избранного — звезда. */
@Composable
private fun ChatBadge(chat: Chat, size: Int) {
    TargetRing(
        Modifier.size(size.dp),
        color = if (chat.id == SUPPORT_CHAT_ID) Violet else Cyan,
        ears = chat.id == SUPPORT_CHAT_ID,
    ) {
        Text(
            if (chat.id == SAVED_CHAT_ID) "★" else chat.name.take(1).uppercase(),
            color = if (chat.id == SAVED_CHAT_ID) Mint else Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 3).sp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(chat: Chat, last: ChatMessage?, typing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(cut = 12.dp, line = if (chat.unread > 0) Mint else Cyan)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatBadge(chat, 44)
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
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (chat.pinned) Text("  ◆", color = Violet, fontSize = 10.sp)
                if (chat.muted) Text("  ⊘", color = Muted, fontSize = 11.sp)
                if (chat.ttlSeconds > 0) Text("  ⏱", color = Magenta, fontSize = 10.sp)
            }
            when {
                typing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(Mint, Modifier.size(8.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("печатает…", color = Mint, fontSize = 12.sp)
                }
                chat.draft.isNotBlank() -> Text(
                    "Черновик: ${chat.draft}",
                    color = Magenta,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                else -> Text(
                    text = previewOf(last),
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (last != null) Text(last.time, color = Muted, fontSize = 10.sp, fontFamily = HudMono)
            if (chat.unread > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    chat.unread.toString(),
                    color = Bg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(if (chat.muted) Muted else Mint)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}

private fun previewOf(last: ChatMessage?): String {
    if (last == null) return "ожидание сигнала…"
    val body = plainText(last.text).ifBlank {
        if (last.attachments.any { it.kind == AttachKind.AUDIO }) "🎙 голосовое" else "вложение"
    }
    val prefix = if (last.forwardedFrom != null) "↪ " else ""
    return prefix + if (last.fromMe) "Вы: $body" else body
}

// ---------------------------------------------------------------- Переписка

private sealed class ListRow {
    data class Day(val label: String) : ListRow()
    data class Msg(val message: ChatMessage) : ListRow()
}

@Composable
private fun Conversation(
    chat: Chat,
    allChats: List<Chat>,
    messages: List<ChatMessage>,
    settings: AppSettings,
    typing: Boolean,
    onBack: () -> Unit,
    onSend: (String, List<Attachment>, Int?) -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onReact: (Int, String) -> Unit,
    onForward: (Int, Int) -> Unit,
    onPinMessage: (Int?) -> Unit,
    onDraft: (String) -> Unit,
    onSetTtl: (Int) -> Unit,
) {
    BackHandler(onBack = onBack)

    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var draft by rememberSaveable(chat.id) { mutableStateOf(chat.draft) }
    var pending by remember(chat.id) { mutableStateOf<List<Attachment>>(emptyList()) }
    var replyTo by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var editing by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var actionFor by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var reactionFor by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var forwardFor by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }
    var showAttach by remember(chat.id) { mutableStateOf(false) }
    var showEmoji by remember(chat.id) { mutableStateOf(false) }
    var showTtl by remember(chat.id) { mutableStateOf(false) }
    var searching by remember(chat.id) { mutableStateOf(false) }
    var query by remember(chat.id) { mutableStateOf("") }
    var matchIndex by remember(chat.id) { mutableIntStateOf(0) }
    var flashId by remember(chat.id) { mutableStateOf<Int?>(null) }

    val pickPhoto = rememberPhotoPicker { pending = pending + it }
    val pickFile = rememberFilePicker { pending = pending + it }
    val listState = rememberLazyListState()

    val rows = remember(messages) { buildRows(messages) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    fun buzz() {
        if (settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Голосовые: запись, таймер и уровень звука
    val recorder = remember(chat.id) { VoiceRecorder(context) }
    var recording by remember(chat.id) { mutableStateOf(false) }
    var recElapsed by remember(chat.id) { mutableLongStateOf(0L) }
    var recLevel by remember(chat.id) { mutableFloatStateOf(0f) }
    fun startRecording() {
        if (recorder.start()) {
            recording = true
            buzz()
        } else {
            Toast.makeText(context, "Микрофон недоступен", Toast.LENGTH_SHORT).show()
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, "Нужен доступ к микрофону", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(recording) {
        while (recording) {
            recElapsed = recorder.elapsed()
            recLevel = recorder.level()
            delay(120)
        }
    }

    // Черновик сохраняется при выходе из чата (если не в режиме правки), незаконченная запись отменяется
    val latestDraft = rememberUpdatedState(if (editing == null) draft else "")
    DisposableEffect(chat.id) {
        onDispose {
            onDraft(latestDraft.value)
            recorder.cancel()
        }
    }

    fun jumpTo(id: Int) {
        val index = rows.indexOfFirst { it is ListRow.Msg && it.message.id == id }
        if (index >= 0) {
            scope.launch { listState.animateScrollToItem(index) }
            flashId = id
        }
    }

    LaunchedEffect(flashId) {
        if (flashId != null) {
            delay(1600)
            flashId = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !searching) listState.animateScrollToItem(rows.size - 1)
    }

    // Совпадения поиска внутри чата: от старых к новым
    val q = query.trim()
    val matches = remember(messages, q) {
        if (q.isEmpty()) emptyList() else messages.filter { plainText(it.text).contains(q, ignoreCase = true) }.map { it.id }
    }
    LaunchedEffect(matches) {
        if (matches.isNotEmpty()) {
            matchIndex = matches.size - 1
            jumpTo(matches[matchIndex])
        }
    }

    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 2
        }
    }

    // Открылась клавиатура — показываем последние сообщения над ней
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeOpen) {
        if (imeOpen && rows.isNotEmpty()) listState.scrollToItem(rows.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        // Шапка
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
            ChatBadge(chat, 36)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat.name.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (typing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseDot(Mint, Modifier.size(7.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("печатает…", color = Mint, fontSize = 10.sp)
                    }
                } else {
                    Text(
                        when {
                            chat.id == SUPPORT_CHAT_ID -> "квантовый ИИ · Аврора"
                            chat.id == SAVED_CHAT_ID -> "сохранённое для себя"
                            settings.showOnline -> "в сети"
                            else -> ""
                        } + if (chat.ttlSeconds > 0) " · ⏱ ${ttlLabel(chat.ttlSeconds)}" else "",
                        color = Violet,
                        fontSize = 10.sp,
                    )
                }
            }
            Box(
                Modifier.size(40.dp).hudPanel(cut = 9.dp, line = if (searching) Magenta else Cyan).clickable {
                    searching = !searching
                    if (!searching) query = ""
                },
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.SEARCH, if (searching) Magenta else Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).hudPanel(cut = 9.dp, line = if (chat.ttlSeconds > 0) Magenta else Cyan).clickable { showTtl = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("⏱", color = if (chat.ttlSeconds > 0) Magenta else Cyan, fontSize = 16.sp)
            }
        }

        // Поиск по переписке
        if (searching) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    SearchField(query, "Поиск в чате") { query = it }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (matches.isEmpty()) "0/0" else "${matchIndex + 1}/${matches.size}",
                    color = Muted,
                    fontSize = 11.sp,
                    fontFamily = HudMono,
                )
                Text(
                    "▲",
                    color = Cyan,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        if (matches.isNotEmpty()) {
                            matchIndex = (matchIndex - 1 + matches.size) % matches.size
                            jumpTo(matches[matchIndex])
                        }
                    }.padding(horizontal = 8.dp),
                )
                Text(
                    "▼",
                    color = Cyan,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        if (matches.isNotEmpty()) {
                            matchIndex = (matchIndex + 1) % matches.size
                            jumpTo(matches[matchIndex])
                        }
                    }.padding(start = 4.dp),
                )
            }
        }

        // Закреплённое сообщение
        val pinned = chat.pinnedMessageId?.let { id -> messages.firstOrNull { it.id == id } }
        if (pinned != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .hudPanel(line = Violet, fill = PanelHi, cut = 8.dp)
                    .clickable { jumpTo(pinned.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(2.dp).height(28.dp).background(Brush.verticalGradient(listOf(Violet, Cyan))))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("ЗАКРЕПЛЕНО", color = Violet, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text(
                        plainText(pinned.text).ifBlank { "вложение" },
                        color = TextMain,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("×", color = Muted, fontSize = 18.sp, modifier = Modifier.clickable { onPinMessage(null) }.padding(start = 8.dp))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (chat.id == SAVED_CHAT_ID) {
                        HudEmptyState("ИЗБРАННОЕ ПУСТО", "Сообщение → «В избранное»")
                    } else {
                        HudEmptyState("КАНАЛ ПУСТ", "Передайте первый сигнал")
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
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
                                highlight = if (searching) q else "",
                                flash = flashId == r.message.id,
                                pinned = chat.pinnedMessageId == r.message.id,
                                onLongClick = {
                                    buzz()
                                    actionFor = r.message
                                },
                                onReactionClick = { onReact(r.message.id, it) },
                                onDoubleTap = {
                                    buzz()
                                    onReact(r.message.id, "❤️")
                                },
                                onQuoteClick = { id -> jumpTo(id) },
                                onSwipeReply = {
                                    buzz()
                                    replyTo = r.message
                                    editing = null
                                },
                            )
                        }
                    }
                }
                if (!atBottom) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(42.dp)
                            .hudPanel(cut = 10.dp, glow = true)
                            .clickable { scope.launch { listState.animateScrollToItem(rows.size - 1) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↓", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Режим ответа или правки
        val mode = editing?.let { "ИЗМЕНЕНИЕ" to plainText(it.text) }
            ?: replyTo?.let { "ОТВЕТ" to plainText(it.text).ifBlank { "вложение" } }
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
                        if (editing != null) draft = ""
                        editing = null
                        replyTo = null
                    }.padding(start = 8.dp),
                )
            }
        }

        if (pending.isNotEmpty()) {
            AttachmentList(pending, onRemove = { i -> pending = pending.filterIndexed { idx, _ -> idx != i } })
            Spacer(Modifier.height(6.dp))
        }

        if (showEmoji) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(bottom = 6.dp)
                    .hudPanel(cut = 10.dp, fill = Panel)
                    .padding(6.dp),
            ) {
                items(EmojiPanel.size) { i ->
                    val emoji = EmojiPanel[i]
                    Box(
                        Modifier.size(40.dp).clickable { draft += emoji },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }

        // Быстрые вопросы Авроре
        if (chat.id == SUPPORT_CHAT_ID && draft.isEmpty() && !recording) {
            SuggestionChips(onPick = { onSend(it, emptyList(), null) })
        }

        if (recording) {
            RecordingBar(
                elapsed = recElapsed,
                level = recLevel,
                onCancel = {
                    recorder.cancel()
                    recording = false
                },
                onSend = {
                    val voice = recorder.stop()
                    recording = false
                    if (voice != null) {
                        onSend("", listOf(voice), replyTo?.id)
                        replyTo = null
                    } else {
                        Toast.makeText(context, "Слишком короткая запись", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        } else Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).hudPanel(cut = 10.dp).clickable { showAttach = true },
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.PLUS, Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(44.dp).hudPanel(cut = 10.dp, line = if (showEmoji) Mint else Cyan).clickable { showEmoji = !showEmoji },
                contentAlignment = Alignment.Center,
            ) {
                Text("☺", color = if (showEmoji) Mint else Cyan, fontSize = 20.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .weight(1f)
                    .hudPanel(line = Cyan.copy(alpha = 0.5f), fill = Bg, cut = 10.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (draft.isEmpty()) Text("Сообщение · **жирный** _курсив_", color = Muted, fontSize = 14.sp, maxLines = 1)
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(Cyan),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .hudPanel(line = Cyan, cut = 10.dp, glow = true, brush = Brush.linearGradient(listOf(Violet, Cyan, Mint)))
                    .clickable {
                        // Пустое поле — кнопка записывает голосовое
                        if (draft.isBlank() && pending.isEmpty() && editing == null) {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            return@clickable
                        }
                        val text = draft.trim()
                        val edit = editing
                        if (edit != null) {
                            if (text.isNotEmpty()) onEdit(edit.id, text)
                            editing = null
                        } else if (text.isNotEmpty() || pending.isNotEmpty()) {
                            onSend(text, pending, replyTo?.id)
                            pending = emptyList()
                            replyTo = null
                            showEmoji = false
                        } else {
                            return@clickable
                        }
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (draft.isBlank() && pending.isEmpty() && editing == null) {
                    Text("🎙", fontSize = 18.sp)
                } else {
                    HudIcon(HudIconKind.SEND, Bg, Modifier.size(18.dp))
                }
            }
        }
    }

    // Меню сообщения
    actionFor?.let { m ->
        val url = firstUrl(m.text)
        val actions = buildList {
            add(HudAction("Ответить") {
                replyTo = m
                editing = null
            })
            add(HudAction("Переслать") { forwardFor = m })
            if (chat.id != SAVED_CHAT_ID) {
                add(HudAction("В избранное") { onForward(m.id, SAVED_CHAT_ID) })
            }
            add(HudAction(if (chat.pinnedMessageId == m.id) "Открепить" else "Закрепить") {
                onPinMessage(if (chat.pinnedMessageId == m.id) null else m.id)
            })
            if (m.text.isNotEmpty()) {
                add(HudAction("Копировать") { clipboard.setText(AnnotatedString(plainText(m.text))) })
            }
            if (url != null) {
                add(HudAction("Открыть ссылку") { runCatching { uriHandler.openUri(url) } })
            }
            add(HudAction("Реакция") { reactionFor = m })
            if (m.fromMe && m.forwardedFrom == null) {
                add(HudAction("Изменить") {
                    editing = m
                    replyTo = null
                    draft = m.text
                })
            }
            add(HudAction("Удалить", danger = true) { onDelete(m.id) })
        }
        HudActionSheet(title = "Сообщение", actions = actions, onDismiss = { actionFor = null })
    }

    // Куда переслать
    forwardFor?.let { m ->
        HudActionSheet(
            title = "Переслать в…",
            actions = allChats.filter { it.id != chat.id }.map { target ->
                HudAction(target.name) { onForward(m.id, target.id) }
            },
            onDismiss = { forwardFor = null },
        )
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

    if (showTtl) {
        TtlSheet(chat, onSet = onSetTtl, onDismiss = { showTtl = false })
    }

    reactionFor?.let { m ->
        HudActionSheet(
            title = "Реакция",
            actions = QuickReactions.map { emoji -> HudAction(emoji) { onReact(m.id, emoji) } },
            onDismiss = { reactionFor = null },
        )
    }
}

/** Вопросы-подсказки в чате Авроры: одно нажатие отправляет вопрос. */
private val AuroraQuestions = listOf(
    "Как переслать?", "Что в архиве?", "Разметка текста", "Истории", "Исчезающие", "PIN-код", "Поиск в чате",
)

@Composable
private fun SuggestionChips(onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AuroraQuestions.forEach { q ->
            Text(
                q,
                color = Violet,
                fontSize = 12.sp,
                modifier = Modifier
                    .hudPanel(line = Violet, fill = Panel, cut = 6.dp)
                    .clickable { onPick(q) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

/** Полоса записи голосового: пульс, таймер, живой уровень звука, отмена и отправка. */
@Composable
private fun RecordingBar(elapsed: Long, level: Float, onCancel: () -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .hudPanel(line = Red, fill = Panel, cut = 10.dp, glow = true)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseDot(Red, Modifier.size(12.dp))
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(elapsed), color = TextMain, fontSize = 14.sp, fontFamily = HudMono)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(6.dp).background(Muted.copy(alpha = 0.3f))) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0.05f, 1f))
                    .background(Brush.horizontalGradient(listOf(Violet, Cyan, Mint))),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "ОТМЕНА",
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.clickable(onClick = onCancel).padding(6.dp),
        )
        Box(
            Modifier
                .size(40.dp)
                .hudPanel(line = Cyan, cut = 9.dp, glow = true, brush = Brush.linearGradient(listOf(Violet, Cyan, Mint)))
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            HudIcon(HudIconKind.SEND, Bg, Modifier.size(16.dp))
        }
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
        Text(
            "— ${label.uppercase()} —",
            color = Muted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontFamily = HudMono,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    message: ChatMessage,
    quoted: ChatMessage?,
    showRead: Boolean,
    highlight: String,
    flash: Boolean,
    pinned: Boolean,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onDoubleTap: () -> Unit,
    onQuoteClick: (Int) -> Unit,
    onSwipeReply: () -> Unit,
) {
    val mine = message.fromMe
    var dragX by remember(message.id) { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }

    Box(Modifier.fillMaxWidth()) {
        // Значок ответа, проявляется при свайпе вправо
        if (dragX > 0f) {
            Text(
                "↩",
                color = Cyan.copy(alpha = (dragX / threshold).coerceIn(0f, 1f)),
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragX.roundToInt(), 0) }
                .pointerInput(message.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX >= threshold) onSwipeReply()
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    ) { change, amount ->
                        dragX = (dragX + amount).coerceIn(0f, threshold * 1.4f)
                        change.consume()
                    }
                },
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            val line = when {
                flash -> Magenta
                mine -> Cyan
                else -> Violet
            }
            Column(
                Modifier
                    .widthIn(max = 290.dp)
                    .hudPanel(
                        line = line,
                        fill = PanelHi,
                        cut = 10.dp,
                        glow = flash,
                        brush = if (mine) Brush.linearGradient(listOf(Color(0xFF0A2733), Color(0xFF1B1140))) else null,
                    )
                    .combinedClickable(onClick = {}, onDoubleClick = onDoubleTap, onLongClick = onLongClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (message.forwardedFrom != null) {
                    Text("↪ Переслано от ${message.forwardedFrom}", color = Mint, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                }
                if (quoted != null) {
                    Row(
                        Modifier.clickable { onQuoteClick(quoted.id) }.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(2.dp).height(26.dp).background(Violet))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            plainText(quoted.text).ifBlank { "вложение" },
                            color = Muted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (message.text.isNotEmpty()) {
                    Text(formatMessage(message.text, highlight), color = TextMain, fontSize = 14.sp)
                }
                if (message.attachments.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    AttachmentList(message.attachments)
                }
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) Text("◆ ", color = Violet, fontSize = 9.sp)
                    Text(
                        if (message.edited) "${message.time} · изм." else message.time,
                        color = Muted,
                        fontSize = 10.sp,
                        fontFamily = HudMono,
                    )
                    if (mine) {
                        val read = showRead && message.read
                        Text(
                            if (read) " ✓✓" else " ✓",
                            color = if (read) Mint else Muted,
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
}
