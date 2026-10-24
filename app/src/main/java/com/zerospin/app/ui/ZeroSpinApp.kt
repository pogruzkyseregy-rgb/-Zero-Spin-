package com.zerospin.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.zerospin.app.data.AppStore
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.Event
import com.zerospin.app.data.Note
import com.zerospin.app.data.Poll
import com.zerospin.app.data.Post
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.zerospin.app.data.Comment
import com.zerospin.app.data.SAVED_CHAT_ID
import com.zerospin.app.data.Story
import com.zerospin.app.data.STORY_LIFETIME_MS
import com.zerospin.app.data.SUPPORT_CHAT_ID
import com.zerospin.app.data.auroraReply
import kotlinx.coroutines.launch
import com.zerospin.app.data.nextId
import com.zerospin.app.data.nowMillis
import com.zerospin.app.data.nowTime
import androidx.compose.runtime.CompositionLocalProvider
import com.zerospin.app.ui.components.LocalAvatarSetter
import com.zerospin.app.ui.hud.HudBackground
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudTab
import com.zerospin.app.ui.hud.HudTabBar
import com.zerospin.app.ui.hud.HudTopBar
import com.zerospin.app.ui.screens.AppsScreen
import com.zerospin.app.ui.screens.ChatsScreen
import com.zerospin.app.ui.screens.CoinScreen
import com.zerospin.app.ui.screens.EventsScreen
import com.zerospin.app.ui.screens.FeedScreen
import com.zerospin.app.ui.screens.NotesScreen
import com.zerospin.app.ui.screens.PollsScreen
import com.zerospin.app.ui.screens.ProfileBuilderScreen
import com.zerospin.app.ui.screens.ProfileScreen
import com.zerospin.app.ui.screens.SECTION_COIN
import com.zerospin.app.ui.screens.SECTION_EVENTS
import com.zerospin.app.ui.screens.SECTION_NOTES
import com.zerospin.app.ui.screens.SECTION_POLLS
import com.zerospin.app.ui.screens.SettingsScreen
import kotlinx.coroutines.delay

private const val NO_SECTION = ""
private const val NO_CHAT = -1
private const val APP_VERSION = "v0.1"

@Composable
fun ZeroSpinApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }

    // Любое изменение данных сразу уходит в хранилище
    LaunchedEffect(store) {
        snapshotFlow { store.toJsonString() }.collect { store.save(it) }
    }

    val tabs = listOf(
        HudTab("ЛЕНТА", HudIconKind.FEED),
        HudTab("ЧАТ", HudIconKind.CHAT),
        HudTab("ПРИЛОЖЕНИЯ", HudIconKind.APPS),
        HudTab("ПРОФИЛЬ", HudIconKind.PROFILE),
    )

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var section by rememberSaveable { mutableStateOf(NO_SECTION) }
    var openChat by rememberSaveable { mutableIntStateOf(NO_CHAT) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showBuilder by rememberSaveable { mutableStateOf(false) }
    // Чат, в котором собеседник «печатает» (сейчас — Аврора)
    var typingChat by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    val myName = store.profile.value.name
    val settings = store.settings.value

    fun selectTab(index: Int) {
        tab = index
        section = NO_SECTION
    }

    fun upsertNote(note: Note) {
        val i = store.notes.indexOfFirst { it.id == note.id }
        if (i >= 0) store.notes[i] = note else store.notes.add(0, note)
    }

    fun vote(pollId: Int, option: Int) {
        val i = store.polls.indexOfFirst { it.id == pollId }
        if (i < 0) return
        val poll = store.polls[i]
        if (poll.closed) return
        val votes = poll.votes.toMutableList()
        val mine = poll.myVotes.toMutableList()
        if (option in mine) {
            // Повторное нажатие снимает голос
            mine.remove(option)
            votes[option] = (votes[option] - 1).coerceAtLeast(0)
        } else {
            // В режиме одного ответа новый выбор заменяет старый
            if (!poll.multiple) {
                mine.forEach { votes[it] = (votes[it] - 1).coerceAtLeast(0) }
                mine.clear()
            }
            mine.add(option)
            votes[option] = votes[option] + 1
        }
        store.polls[i] = poll.copy(votes = votes, myVotes = mine)
    }

    fun setPollClosed(pollId: Int, closed: Boolean) {
        val i = store.polls.indexOfFirst { it.id == pollId }
        if (i >= 0) store.polls[i] = store.polls[i].copy(closed = closed)
    }

    // Репост: счётчик на исходной записи и новая запись со ссылкой на неё. Повторно нажать — отменить.
    fun repost(id: Int) {
        val i = store.posts.indexOfFirst { it.id == id }
        if (i < 0) return
        val p = store.posts[i]
        if (p.reposted) {
            store.posts[i] = p.copy(reposted = false, reposts = (p.reposts - 1).coerceAtLeast(0))
            store.posts.removeAll { it.repostOf == id && it.author == myName }
        } else {
            store.posts[i] = p.copy(reposted = true, reposts = p.reposts + 1)
            store.posts.add(
                0,
                Post(
                    id = nextId(store.posts.map { it.id }),
                    author = myName,
                    time = nowTime(),
                    text = "",
                    attachments = emptyList(),
                    likes = 0,
                    liked = false,
                    repostOf = id,
                ),
            )
        }
    }

    fun updatePost(id: Int, change: (Post) -> Post) {
        val i = store.posts.indexOfFirst { it.id == id }
        if (i >= 0) store.posts[i] = change(store.posts[i])
    }

    fun updateMessage(chatId: Int, id: Int, change: (ChatMessage) -> ChatMessage) {
        store.messages[chatId] = store.messages[chatId].orEmpty().map { if (it.id == id) change(it) else it }
    }

    fun react(chatId: Int, id: Int, emoji: String) = updateMessage(chatId, id) { m ->
        when {
            m.myReaction == emoji -> m.copy(myReaction = null, reactionCount = (m.reactionCount - 1).coerceAtLeast(0))
            m.myReaction == null -> m.copy(myReaction = emoji, reactionCount = m.reactionCount + 1)
            else -> m.copy(myReaction = emoji)
        }
    }

    fun updateChat(id: Int, change: (Chat) -> Chat) {
        val i = store.chats.indexOfFirst { it.id == id }
        if (i >= 0) store.chats[i] = change(store.chats[i])
    }

    fun deleteChat(id: Int) {
        if (id == SUPPORT_CHAT_ID || id == SAVED_CHAT_ID) return
        store.chats.removeAll { it.id == id }
        store.messages.remove(id)
        if (openChat == id) openChat = NO_CHAT
    }

    fun openChatById(id: Int) {
        openChat = id
        if (id >= 0) updateChat(id) { it.copy(unread = 0) }
    }

    /** Входящее сообщение: если чат не открыт — растёт счётчик непрочитанных. */
    fun deliverIncoming(chatId: Int, text: String) {
        val list = store.messages[chatId].orEmpty()
        store.messages[chatId] = list + ChatMessage(
            id = nextId(list.map { it.id }),
            fromMe = false,
            text = text,
            time = nowTime(),
        )
        if (openChat != chatId) updateChat(chatId) { it.copy(unread = it.unread + 1) }
    }

    fun sendMessage(chatId: Int, text: String, attachments: List<Attachment>, replyTo: Int?) {
        val list = store.messages[chatId].orEmpty()
        store.messages[chatId] = list + ChatMessage(
            id = nextId(list.map { it.id }),
            fromMe = true,
            text = text,
            time = nowTime(),
            attachments = attachments,
            replyTo = replyTo,
        )
        if (chatId == SUPPORT_CHAT_ID) {
            // Аврора «печатает» и отвечает через пару секунд, как живой собеседник
            scope.launch {
                delay(500)
                typingChat = SUPPORT_CHAT_ID
                delay(900L + (text.length * 15L).coerceAtMost(1800L))
                typingChat = null
                deliverIncoming(SUPPORT_CHAT_ID, auroraReply(text))
            }
        }
    }

    /** Пересылка: копия сообщения в другой чат с пометкой, от кого. */
    fun forward(fromChat: Int, messageId: Int, toChat: Int) {
        val m = store.messages[fromChat].orEmpty().firstOrNull { it.id == messageId } ?: return
        val source = m.forwardedFrom
            ?: if (m.fromMe) myName else store.chats.firstOrNull { it.id == fromChat }?.name ?: "канал"
        val list = store.messages[toChat].orEmpty()
        store.messages[toChat] = list + ChatMessage(
            id = nextId(list.map { it.id }),
            fromMe = true,
            text = m.text,
            time = nowTime(),
            attachments = m.attachments,
            forwardedFrom = source,
        )
        Toast.makeText(
            context,
            if (toChat == SAVED_CHAT_ID) "Сохранено в Избранное" else "Переслано",
            Toast.LENGTH_SHORT,
        ).show()
    }

    // Исчезающие сообщения и истёкшие истории убираются раз в полминуты
    LaunchedEffect(store) {
        while (true) {
            val now = nowMillis()
            store.chats.filter { it.ttlSeconds > 0 }.forEach { chat ->
                val cutoff = now - chat.ttlSeconds * 1000L
                val list = store.messages[chat.id].orEmpty()
                if (list.any { it.createdAt < cutoff }) {
                    store.messages[chat.id] = list.filter { it.createdAt >= cutoff }
                }
            }
            store.stories.removeAll { now - it.createdAt > STORY_LIFETIME_MS }
            delay(30_000)
        }
    }

    // Галочки прочтения: через секунду после открытия чата отмечаем мои сообщения прочитанными
    LaunchedEffect(openChat, store.messages[openChat]?.size) {
        if (openChat >= 0 && store.messages[openChat].orEmpty().any { it.fromMe && !it.read }) {
            delay(1000)
            store.messages[openChat] = store.messages[openChat].orEmpty().map {
                if (it.fromMe) it.copy(read = true) else it
            }
        }
    }

    BackHandler(enabled = section != NO_SECTION) { section = NO_SECTION }

    CompositionLocalProvider(
        LocalAvatarSetter provides { uri -> store.profile.value = store.profile.value.copy(avatarUri = uri) },
    ) {
    val unreadTotal = store.chats.filter { !it.muted && !it.archived }.sumOf { it.unread }

    HudBackground(animated = settings.animatedBackground) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HudTopBar(
                    title = if (tab == 2 && section != NO_SECTION) section.uppercase() else tabs[tab].title,
                    status = if (typingChat != null) "АВРОРА ПЕЧАТАЕТ…" else "КАНАЛ СТАБИЛЕН · КРУГ",
                    onSettings = { showSettings = true },
                    glitch = settings.glitch,
                )
            },
            bottomBar = {
                HudTabBar(
                    tabs = tabs,
                    selected = tab,
                    onSelect = { selectTab(it) },
                    badges = listOf(0, unreadTotal, 0, 0),
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> FeedScreen(
                        posts = store.posts,
                        stories = store.stories,
                        currentAuthor = myName,
                        onComment = { postId, text ->
                            updatePost(postId) { p ->
                                p.copy(comments = p.comments + Comment(nextId(p.comments.map { it.id }), myName, text, nowTime()))
                            }
                        },
                        onDeleteComment = { postId, commentId ->
                            updatePost(postId) { p -> p.copy(comments = p.comments.filter { it.id != commentId }) }
                        },
                        onBookmark = { id -> updatePost(id) { it.copy(bookmarked = !it.bookmarked) } },
                        onAddStory = { uri, caption ->
                            store.stories.add(
                                0,
                                Story(
                                    id = nextId(store.stories.map { it.id }),
                                    author = myName,
                                    uri = uri,
                                    caption = caption,
                                    createdAt = nowMillis(),
                                ),
                            )
                        },
                        onStorySeen = { id ->
                            val i = store.stories.indexOfFirst { it.id == id }
                            if (i >= 0 && !store.stories[i].seen) store.stories[i] = store.stories[i].copy(seen = true)
                        },
                        onDeleteStory = { id -> store.stories.removeAll { it.id == id } },
                        onPublish = { text, attachments ->
                            store.posts.add(
                                0,
                                Post(
                                    id = nextId(store.posts.map { it.id }),
                                    author = myName,
                                    time = nowTime(),
                                    text = text,
                                    attachments = attachments,
                                    likes = 0,
                                    liked = false,
                                ),
                            )
                        },
                        onLike = { id ->
                            val index = store.posts.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val p = store.posts[index]
                                store.posts[index] = p.copy(
                                    liked = !p.liked,
                                    likes = if (p.liked) p.likes - 1 else p.likes + 1,
                                )
                            }
                        },
                        onRepost = { id -> repost(id) },
                        onDelete = { id -> store.posts.removeAll { it.id == id } },
                    )

                    1 -> ChatsScreen(
                        chats = store.chats,
                        messages = store.messages,
                        settings = settings,
                        openChatId = openChat,
                        typingChatId = typingChat,
                        onOpenChat = { openChatById(it) },
                        onCreateChat = { name ->
                            // id обычных каналов не пересекаются с Избранным
                            val id = nextId(store.chats.map { it.id }.filter { it != SAVED_CHAT_ID })
                            store.chats.add(Chat(id, name))
                            store.messages[id] = emptyList()
                        },
                        onSend = { chatId, text, attachments, replyTo -> sendMessage(chatId, text, attachments, replyTo) },
                        onEdit = { chatId, id, text -> updateMessage(chatId, id) { it.copy(text = text, edited = true) } },
                        onDelete = { chatId, id ->
                            store.messages[chatId] = store.messages[chatId].orEmpty().filter { it.id != id }
                            updateChat(chatId) { if (it.pinnedMessageId == id) it.copy(pinnedMessageId = null) else it }
                        },
                        onReact = { chatId, id, emoji -> react(chatId, id, emoji) },
                        onForward = { from, id, to -> forward(from, id, to) },
                        onPinMessage = { chatId, id -> updateChat(chatId) { it.copy(pinnedMessageId = id) } },
                        onDraft = { chatId, text -> updateChat(chatId) { if (it.draft == text) it else it.copy(draft = text) } },
                        onTogglePin = { id -> updateChat(id) { it.copy(pinned = !it.pinned) } },
                        onToggleMute = { id -> updateChat(id) { it.copy(muted = !it.muted) } },
                        onToggleArchive = { id -> updateChat(id) { it.copy(archived = !it.archived) } },
                        onSetTtl = { id, seconds -> updateChat(id) { it.copy(ttlSeconds = seconds) } },
                        onDeleteChat = { id -> deleteChat(id) },
                    )

                    2 -> when (section) {
                        SECTION_NOTES -> NotesScreen(
                            notes = store.notes,
                            onSave = { upsertNote(it) },
                            onDelete = { id -> store.notes.removeAll { it.id == id } },
                        )
                        SECTION_EVENTS -> EventsScreen(
                            events = store.events,
                            onAdd = { store.events.add(it) },
                            onDelete = { id -> store.events.removeAll { it.id == id } },
                        )
                        SECTION_POLLS -> PollsScreen(
                            polls = store.polls,
                            onAdd = { store.polls.add(it) },
                            onVote = { pollId, option -> vote(pollId, option) },
                            onToggleClosed = { pollId, closed -> setPollClosed(pollId, closed) },
                        )
                        SECTION_COIN -> CoinScreen()
                        else -> AppsScreen(
                            onOpen = { section = it },
                            onOpenSupport = {
                                openChatById(SUPPORT_CHAT_ID)
                                selectTab(1)
                            },
                        )
                    }

                    3 -> ProfileScreen(
                        profile = store.profile.value,
                        postCount = store.posts.count { it.author == myName },
                        chatCount = store.chats.count { it.id != SUPPORT_CHAT_ID && it.id != SAVED_CHAT_ID },
                        onSave = { store.profile.value = it },
                        onAvatar = { uri -> store.profile.value = store.profile.value.copy(avatarUri = uri) },
                        onOpenBuilder = { showBuilder = true },
                    )
                }
            }
        }
    }

    }

    if (showBuilder) {
        ProfileBuilderScreen(
            profile = store.profile.value,
            onProfile = { store.profile.value = it },
            onDismiss = { showBuilder = false },
        )
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            profile = store.profile.value,
            version = APP_VERSION,
            onChange = { store.settings.value = it },
            onOpenProfile = {
                showSettings = false
                selectTab(3)
            },
            onOpenBuilder = {
                showSettings = false
                showBuilder = true
            },
            onDismiss = { showSettings = false },
        )
    }
}
