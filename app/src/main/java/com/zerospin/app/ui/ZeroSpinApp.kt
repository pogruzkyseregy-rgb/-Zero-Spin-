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
import com.zerospin.app.data.AppSettings
import com.zerospin.app.data.AppStore
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.Event
import com.zerospin.app.data.Note
import com.zerospin.app.data.Poll
import com.zerospin.app.data.Post
import com.zerospin.app.data.Profile
import com.zerospin.app.data.SUPPORT_CHAT_ID
import com.zerospin.app.data.auroraReply
import com.zerospin.app.data.nextId
import com.zerospin.app.data.nowMillis
import com.zerospin.app.data.nowTime
import com.zerospin.app.ui.hud.HudBackground
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudTab
import com.zerospin.app.ui.hud.HudTabBar
import com.zerospin.app.ui.hud.HudTopBar
import com.zerospin.app.ui.screens.AppsScreen
import com.zerospin.app.ui.screens.ChatsScreen
import com.zerospin.app.ui.screens.CoinScreen
import com.zerospin.app.ui.screens.EventsScreen
import com.zerospin.app.ui.screens.SECTION_COIN
import com.zerospin.app.ui.screens.FeedScreen
import com.zerospin.app.ui.screens.NotesScreen
import com.zerospin.app.ui.screens.PollsScreen
import com.zerospin.app.ui.screens.ProfileScreen
import com.zerospin.app.ui.screens.SECTION_EVENTS
import com.zerospin.app.ui.screens.SECTION_NOTES
import com.zerospin.app.ui.screens.SECTION_POLLS
import com.zerospin.app.ui.screens.SettingsDialog

private const val NO_SECTION = ""
private const val NO_CHAT = -1

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

    val myName = store.profile.value.name

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
        val votes = poll.votes.toMutableList()
        poll.myVote?.let { votes[it] = votes[it] - 1 }
        val newVote = if (poll.myVote == option) null else option
        if (newVote != null) votes[newVote] = votes[newVote] + 1
        store.polls[i] = poll.copy(votes = votes, myVote = newVote)
    }

    BackHandler(enabled = section != NO_SECTION) { section = NO_SECTION }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HudTopBar(
                    title = if (tab == 2 && section != NO_SECTION) section.uppercase() else tabs[tab].title,
                    status = "● ONLINE · КРУГ",
                    onSettings = { showSettings = true },
                )
            },
            bottomBar = {
                HudTabBar(tabs = tabs, selected = tab, onSelect = { selectTab(it) })
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> FeedScreen(
                        posts = store.posts,
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
                    )

                    1 -> ChatsScreen(
                        chats = store.chats,
                        messages = store.messages,
                        openChatId = openChat,
                        onOpenChat = { openChat = it },
                        onCreateChat = {
                            val id = nextId(store.chats.map { it.id })
                            store.chats.add(Chat(id, "Канал $id"))
                            store.messages[id] = emptyList()
                        },
                        onSend = { chatId, text, attachments ->
                            store.messages[chatId] = store.messages[chatId].orEmpty() +
                                ChatMessage(fromMe = true, text = text, time = nowTime(), attachments = attachments)
                            if (chatId == SUPPORT_CHAT_ID) {
                                store.messages[chatId] = store.messages[chatId].orEmpty() +
                                    ChatMessage(fromMe = false, text = auroraReply(text), time = nowTime())
                            }
                        },
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
                        )
                        SECTION_COIN -> CoinScreen()
                        else -> AppsScreen(
                            onOpen = { section = it },
                            onOpenSupport = {
                                openChat = SUPPORT_CHAT_ID
                                selectTab(1)
                            },
                        )
                    }

                    3 -> ProfileScreen(
                        profile = store.profile.value,
                        postCount = store.posts.count { it.author == myName },
                        chatCount = store.chats.count { it.id != SUPPORT_CHAT_ID },
                        onSave = { store.profile.value = it },
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = store.settings.value,
            onChange = { store.settings.value = it },
            onDismiss = { showSettings = false },
        )
    }
}
