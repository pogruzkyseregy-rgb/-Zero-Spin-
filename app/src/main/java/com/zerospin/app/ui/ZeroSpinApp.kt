package com.zerospin.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import com.zerospin.app.data.AppSettings
import com.zerospin.app.data.ZsException
import com.zerospin.app.data.ZsServer
import com.zerospin.app.data.ZsSession
import com.zerospin.app.data.ZsPerson
import com.zerospin.app.data.ZsFriends
import com.zerospin.app.data.AURORA_LOGIN
import com.zerospin.app.data.scaledJpeg
import com.zerospin.app.ui.screens.LocalPeople
import com.zerospin.app.ui.screens.LocalZs
import com.zerospin.app.ui.screens.PeopleScreen
import com.zerospin.app.ui.screens.UserProfileScreen
import com.zerospin.app.ui.screens.ZsCtx
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.zerospin.app.data.AttachKind
import com.zerospin.app.data.squareAvatarJpeg
import com.zerospin.app.ui.screens.AuthScreen
import com.zerospin.app.ui.screens.AuroraProfileScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zerospin.app.ui.screens.UnlockScreen
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
private const val APP_VERSION = "v0.2 · Квант"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZeroSpinApp() {
    val context = LocalContext.current
    val lifecycleOwnerForSync = LocalLifecycleOwner.current
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
        // Непрочитанным считается, если чат не на экране (другой чат или другая вкладка)
        if (openChat != chatId || tab != 1) updateChat(chatId) { it.copy(unread = it.unread + 1) }
    }

    fun setSettings(change: (AppSettings) -> AppSettings) {
        store.settings.value = change(store.settings.value)
    }

    var showAuth by rememberSaveable { mutableStateOf(false) }
    var showAuroraProfile by rememberSaveable { mutableStateOf(false) }

    /** Токен аккаунта. Без входа Аврора в поддержке не отвечает: просим войти по логину и паролю. */
    fun ensureAccount(): String? {
        val s = store.settings.value
        if (s.serverToken.isNotBlank()) return s.serverToken
        deliverIncoming(SUPPORT_CHAT_ID, "Чтобы я ответила, войди в аккаунт: логин и пароль. Окно входа уже открыто, или найдёшь его в Настройках → Сервер.")
        showAuth = true
        return null
    }

    // ---------- друзья и личка через сервер ----------
    val people = remember { mutableStateMapOf<String, ZsPerson>() }
    var showPeople by rememberSaveable { mutableStateOf(false) }
    var profileLogin by rememberSaveable { mutableStateOf<String?>(null) }
    val syncLock = remember { Mutex() }
    var syncKick by remember { mutableIntStateOf(0) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    fun peerChatId(login: String): Int? = store.chats.firstOrNull { it.peer.equals(login, ignoreCase = true) }?.id

    fun ensurePeerChat(login: String, name: String): Int {
        peerChatId(login)?.let { return it }
        val id = nextId(store.chats.map { it.id }.filter { it != SAVED_CHAT_ID })
        store.chats.add(Chat(id, name.ifBlank { login }, peer = login))
        store.messages[id] = emptyList()
        return id
    }

    /** Убрать с телефона все личные чаты (выход, другой аккаунт). На сервере они остаются и вернутся при входе. */
    fun dropPeerChats() {
        store.chats.filter { it.peer.isNotBlank() }.forEach { c -> store.messages.remove(c.id) }
        store.chats.removeAll { it.peer.isNotBlank() }
        if (openChat >= 0 && store.chats.none { it.id == openChat }) openChat = NO_CHAT
        people.clear()
    }

    fun applyFriends(f: ZsFriends) {
        (f.friends + f.incoming + f.outgoing).forEach { people[it.login.lowercase()] = it }
        f.friends.forEach { p ->
            val n = p.name.ifBlank { p.login }
            peerChatId(p.login)?.let { id -> updateChat(id) { c -> if (c.name == n) c else c.copy(name = n) } }
        }
        setSettings { if (it.friendRequests == f.incoming.size) it else it.copy(friendRequests = f.incoming.size) }
        val logins = f.friends.map { it.login }
        if (store.profile.value.friends != logins) store.profile.value = store.profile.value.copy(friends = logins)
    }

    suspend fun refreshFriends() {
        val s = store.settings.value
        if (s.serverToken.isBlank()) return
        runCatching { ZsServer.friends(s.serverUrl, s.serverToken) }.onSuccess { applyFriends(it) }
        runCatching { ZsServer.profile(s.serverUrl, AURORA_LOGIN) }.onSuccess { a ->
            people[AURORA_LOGIN.lowercase()] = ZsPerson(a.login, a.name, a.status, a.bio, a.role, a.banned, a.hasAvatar, a.avatarV)
        }
    }

    fun openPeer(p: ZsPerson) {
        people[p.login.lowercase()] = p
        val id = ensurePeerChat(p.login, p.name)
        showPeople = false
        profileLogin = null
        selectTab(1)
        openChatById(id)
    }

    /** Забрать новое с сервера. Под замком с отправкой: своё сообщение не появится дважды. */
    suspend fun syncOnce() = syncLock.withLock {
        val s = store.settings.value
        if (s.serverToken.isBlank()) return@withLock
        var after = s.syncAfter
        repeat(20) {
            val r = try {
                ZsServer.sync(s.serverUrl, s.serverToken, after)
            } catch (e: ZsException) {
                if (e.code == 401) setSettings { it.copy(serverToken = "", serverRole = "") }
                return@withLock
            }
            for (m in r.messages) {
                val mine = m.from.equals(s.serverNick, ignoreCase = true)
                val peer = if (mine) m.to else m.from
                val chatId = ensurePeerChat(peer, people[peer.lowercase()]?.name ?: peer)
                if (store.messages[chatId].orEmpty().any { it.remoteId == m.id }) continue
                val atts = if (m.hasImage) {
                    val f = File(context.filesDir, "dm/${m.id}.jpg")
                    if (f.exists() || ZsServer.download(s.serverUrl, s.serverToken, "/api/dm-image/${m.id}", f)) {
                        listOf(Attachment(AttachKind.IMAGE, android.net.Uri.fromFile(f).toString(), "фото", "image/jpeg"))
                    } else emptyList()
                } else emptyList()
                val list = store.messages[chatId].orEmpty()
                store.messages[chatId] = list + ChatMessage(
                    id = nextId(list.map { it.id }),
                    fromMe = mine,
                    text = m.text,
                    time = timeFmt.format(Date(m.at)),
                    attachments = atts,
                    createdAt = m.at,
                    remoteId = m.id,
                )
                if (!mine && (openChat != chatId || tab != 1)) updateChat(chatId) { it.copy(unread = it.unread + 1) }
            }
            after = r.lastId
            setSettings { if (it.syncAfter == after && it.friendRequests == r.incoming) it else it.copy(syncAfter = after, friendRequests = r.incoming) }
            if (!r.more) return@withLock
        }
    }

    /** Отправить в личку одно сообщение: текст и не больше одного фото. */
    fun sendToPeer(chatId: Int, localId: Int, peer: String, text: String, photo: Attachment?) {
        scope.launch {
            val s = store.settings.value
            if (s.serverToken.isBlank()) {
                updateMessage(chatId, localId) { it.copy(failed = true) }
                showAuth = true
                return@launch
            }
            syncLock.withLock {
                try {
                    val jpeg = photo?.let { scaledJpeg(context, it.uri) }
                    if (photo != null && jpeg == null && text.isBlank()) throw ZsException(400, "не получилось прочитать фото")
                    val sent = ZsServer.sendDm(s.serverUrl, s.serverToken, peer, text, jpeg)
                    updateMessage(chatId, localId) { it.copy(remoteId = sent.id, failed = false) }
                } catch (e: ZsException) {
                    updateMessage(chatId, localId) { it.copy(failed = true) }
                    Toast.makeText(context, e.message ?: "не отправлено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun retry(chatId: Int, messageId: Int) {
        val chat = store.chats.firstOrNull { it.id == chatId } ?: return
        val m = store.messages[chatId].orEmpty().firstOrNull { it.id == messageId } ?: return
        if (chat.peer.isBlank() || !m.failed) return
        updateMessage(chatId, messageId) { it.copy(failed = false) }
        sendToPeer(chatId, messageId, chat.peer, m.text, m.attachments.firstOrNull { it.kind == AttachKind.IMAGE })
    }

    /** Имя, статус и описание из профиля на телефоне уходят на сервер: так тебя видят друзья. */
    fun pushProfile(withAvatar: Boolean) {
        val s = store.settings.value
        if (s.serverToken.isBlank()) return
        val p = store.profile.value
        scope.launch {
            runCatching { ZsServer.updateMe(s.serverUrl, s.serverToken, p.name, p.status, p.bio) }
            if (withAvatar) {
                p.avatarUri?.let { uri ->
                    squareAvatarJpeg(context, uri)?.let { jpeg -> runCatching { ZsServer.uploadMyAvatar(s.serverUrl, s.serverToken, jpeg) } }
                }
            }
        }
    }

    fun applySession(z: ZsSession) {
        // другой аккаунт: личка прежнего с телефона убирается, новая придёт с сервера
        if (!store.settings.value.serverNick.equals(z.login, ignoreCase = true)) {
            dropPeerChats()
            setSettings { it.copy(syncAfter = 0, friendRequests = 0) }
        }
        setSettings {
            it.copy(serverToken = z.token, serverNick = z.login, serverRole = z.role, authSkipped = false,
                zsWarnings = 0, zsBanned = false, zsBanReason = "")
        }
        pushProfile(withAvatar = true)
        showAuth = false
        // бан и предупреждения этого аккаунта сразу берём с сервера
        scope.launch {
            val s = store.settings.value
            runCatching { ZsServer.me(s.serverUrl, s.serverToken) }.onSuccess { m ->
                setSettings { it.copy(zsWarnings = m.warnings, zsBanned = m.banned, zsBanReason = m.banReason, serverNick = m.nick, serverRole = m.role) }
            }
        }
    }

    fun logout() {
        val s = store.settings.value
        if (s.serverToken.isNotBlank()) scope.launch { runCatching { ZsServer.logout(s.serverUrl, s.serverToken) } }
        dropPeerChats()
        setSettings {
            it.copy(serverToken = "", serverNick = "", serverRole = "", zsWarnings = 0, zsBanned = false, zsBanReason = "",
                syncAfter = 0, friendRequests = 0)
        }
    }

    /** «!аватар» + фото в чате с Авророй: только с аккаунта админа. Фото сразу становится её аватаром. */
    suspend fun setAuroraAvatar(attachments: List<Attachment>) {
        val s = store.settings.value
        val token = ensureAccount() ?: return
        if (s.serverRole != "admin") {
            deliverIncoming(SUPPORT_CHAT_ID, "«!аватар» работает только с аккаунта админа.")
            return
        }
        val photo = attachments.firstOrNull { it.kind == AttachKind.IMAGE }
        if (photo == null) {
            deliverIncoming(SUPPORT_CHAT_ID, "Прикрепи фото к «!аватар», тогда поставлю.")
            return
        }
        typingChat = SUPPORT_CHAT_ID
        try {
            val jpeg = squareAvatarJpeg(context, photo.uri)
            if (jpeg == null) {
                deliverIncoming(SUPPORT_CHAT_ID, "Не смогла прочитать это фото. Попробуй другое.")
                return
            }
            ZsServer.setAuroraAvatar(s.serverUrl, token, jpeg)
            deliverIncoming(SUPPORT_CHAT_ID, "✓ Новый аватар стоит. Посмотреть: нажми на шапку чата.")
        } catch (e: ZsException) {
            if (e.code == 401) {
                setSettings { it.copy(serverToken = "", serverRole = "") }
                showAuth = true
            }
            deliverIncoming(SUPPORT_CHAT_ID, "Аватар не сменился: ${e.message}")
        } finally {
            typingChat = null
        }
    }

    /** Живая Аврора: ответ, предупреждения и бан решает сервер, приложение только показывает. */
    suspend fun askSupport(text: String) {
        val s0 = store.settings.value
        if (s0.zsBanned) return
        if (text.isBlank()) {
            deliverIncoming(SUPPORT_CHAT_ID, "Вложения я пока не вижу. Напиши словами.")
            return
        }
        typingChat = SUPPORT_CHAT_ID
        try {
            val token = ensureAccount() ?: return
            val r = try {
                ZsServer.support(store.settings.value.serverUrl, token, text)
            } catch (e: ZsException) {
                when (e.code) {
                    403 -> {
                        setSettings { it.copy(zsBanned = true) }
                        deliverIncoming(SUPPORT_CHAT_ID, "⛔ Аврора больше не отвечает тебе.")
                    }
                    401 -> {
                        setSettings { it.copy(serverToken = "", serverRole = "") }
                        deliverIncoming(SUPPORT_CHAT_ID, "Сессия закончилась. Войди в аккаунт снова.")
                        showAuth = true
                    }
                    0 -> deliverIncoming(SUPPORT_CHAT_ID, "Нет связи с сервером. Пока подсказка из самого приложения:\n" + auroraReply(text))
                    else -> deliverIncoming(SUPPORT_CHAT_ID, e.message ?: "Ошибка сервера")
                }
                return
            }
            deliverIncoming(SUPPORT_CHAT_ID, r.reply)
            setSettings {
                it.copy(zsWarnings = r.warnings, zsBanned = r.banned, zsBanReason = if (r.banned) r.reason else it.zsBanReason)
            }
            when {
                r.banned -> deliverIncoming(SUPPORT_CHAT_ID, "⛔ БАН. Второе предупреждение: ${r.reason}. Аврора больше не отвечает.")
                r.warned -> deliverIncoming(SUPPORT_CHAT_ID, "⚠ ПРЕДУПРЕЖДЕНИЕ ${r.warnings} из ${r.warnLimit}: ${r.reason}. Следующее — бан.")
            }
        } finally {
            typingChat = null
        }
    }

    // При запуске сверяем бан и предупреждения с сервером: их мог снять админ
    LaunchedEffect(Unit) {
        val s = store.settings.value
        if (s.serverToken.isNotBlank()) {
            runCatching { ZsServer.me(s.serverUrl, s.serverToken) }
                .onSuccess { m ->
                    setSettings { it.copy(zsWarnings = m.warnings, zsBanned = m.banned, zsBanReason = m.banReason, serverNick = m.nick, serverRole = m.role) }
                }
                .onFailure { e -> if (e is ZsException && e.code == 401) setSettings { it.copy(serverToken = "", serverRole = "") } }
        } else if (!s.authSkipped) {
            showAuth = true
        }
    }

    // Вернулись на вкладку с открытым чатом — он прочитан
    LaunchedEffect(tab, openChat) {
        if (tab == 1 && openChat >= 0) updateChat(openChat) { if (it.unread == 0) it else it.copy(unread = 0) }
    }

    fun sendMessage(chatId: Int, text: String, attachments: List<Attachment>, replyTo: Int?) {
        val peer = store.chats.firstOrNull { it.id == chatId }?.peer.orEmpty()
        if (peer.isNotBlank()) {
            // личка: на сервер уходят текст и фото, по одному фото в сообщении
            val photos = attachments.filter { it.kind == AttachKind.IMAGE }
            if (photos.size < attachments.size) {
                Toast.makeText(context, "В личку пока уходят только текст и фото", Toast.LENGTH_SHORT).show()
            }
            if (text.isBlank() && photos.isEmpty()) return
            val parts = if (photos.isEmpty()) listOf(text to null) else photos.mapIndexed { i, ph -> (if (i == 0) text else "") to ph }
            parts.forEachIndexed { i, (t, ph) ->
                val l = store.messages[chatId].orEmpty()
                val id = nextId(l.map { it.id })
                store.messages[chatId] = l + ChatMessage(
                    id = id, fromMe = true, text = t, time = nowTime(),
                    attachments = listOfNotNull(ph), replyTo = if (i == 0) replyTo else null,
                )
                sendToPeer(chatId, id, peer, t, ph)
            }
            return
        }
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
            if (text.trim().lowercase().startsWith("!аватар")) scope.launch { setAuroraAvatar(attachments) }
            else scope.launch { askSupport(text) }
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
        val isPeer = store.chats.firstOrNull { it.id == openChat }?.peer?.isNotBlank() == true
        if (openChat >= 0 && !isPeer && store.messages[openChat].orEmpty().any { it.fromMe && !it.read }) {
            delay(1000)
            store.messages[openChat] = store.messages[openChat].orEmpty().map {
                if (it.fromMe) it.copy(read = true) else it
            }
        }
    }

    // Личка и заявки: часто, пока открыт чат с другом, и реже в остальное время. Только когда приложение на экране.
    LaunchedEffect(settings.serverToken) {
        if (settings.serverToken.isBlank()) return@LaunchedEffect
        refreshFriends()
        var lastIncoming = store.settings.value.friendRequests
        var round = 0
        while (true) {
            if (lifecycleOwnerForSync.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                syncOnce()
                val inc = store.settings.value.friendRequests
                if (inc != lastIncoming || round % 6 == 5) {
                    lastIncoming = inc
                    refreshFriends()
                }
                round++
            }
            val peerOpen = tab == 1 && store.chats.firstOrNull { it.id == openChat }?.peer?.isNotBlank() == true
            val seen = syncKick
            val until = System.currentTimeMillis() + if (peerOpen) 3_000 else 12_000
            while (System.currentTimeMillis() < until && syncKick == seen) delay(250)
        }
    }

    // Блокировка PIN-кодом: при запуске и после минуты в фоне
    var unlocked by rememberSaveable { mutableStateOf(store.settings.value.pinHash.isEmpty()) }
    var stoppedAt by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> stoppedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START ->
                    if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 60_000) unlocked = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (settings.pinHash.isNotEmpty() && !unlocked) {
        UnlockScreen(pinHash = settings.pinHash, animated = settings.animatedBackground, onUnlocked = { unlocked = true })
        return
    }

    BackHandler(enabled = section != NO_SECTION) { section = NO_SECTION }

    val zsCtx = ZsCtx(settings.serverUrl, settings.serverToken, settings.serverNick)
    CompositionLocalProvider(
        LocalAvatarSetter provides { uri ->
            store.profile.value = store.profile.value.copy(avatarUri = uri)
            pushProfile(withAvatar = true)
        },
        LocalZs provides zsCtx,
        LocalPeople provides people,
    ) {
    val unreadTotal = store.chats.filter { !it.muted && !it.archived }.sumOf { it.unread }

    HudBackground(animated = settings.animatedBackground) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // В открытом чате своя шапка: общая только отнимала бы место
                if (!(tab == 1 && openChat >= 0)) HudTopBar(
                    title = if (tab == 2 && section != NO_SECTION) section.uppercase() else tabs[tab].title,
                    status = if (typingChat != null) "АВРОРА ПЕЧАТАЕТ…" else "КАНАЛ СТАБИЛЕН · КРУГ",
                    onSettings = { showSettings = true },
                    glitch = settings.glitch,
                )
            },
            bottomBar = {
                // В открытом чате нижняя панель прячется: больше места для переписки и клавиатуры
                if (!(tab == 1 && openChat >= 0)) {
                    HudTabBar(
                        tabs = tabs,
                        selected = tab,
                        onSelect = { selectTab(it) },
                        badges = listOf(0, unreadTotal + settings.friendRequests, 0, 0),
                    )
                }
            },
        ) { padding ->
            // Отступ под клавиатуру: экран сжимается над ней, поле ввода остаётся видно.
            // consumeWindowInsets не даёт посчитать нижнюю панель дважды.
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize(),
            ) {
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
                        onOpenChat = {
                            openChatById(it)
                            if (store.chats.firstOrNull { c -> c.id == it }?.peer?.isNotBlank() == true) syncKick++
                        },
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
                        onOpenAuroraProfile = { showAuroraProfile = true },
                        onOpenPeople = { if (settings.serverToken.isBlank()) showAuth = true else showPeople = true },
                        onOpenProfile = { profileLogin = it },
                        onRetry = { chatId, id -> retry(chatId, id) },
                        friendRequests = settings.friendRequests,
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
                            onOpenSaved = {
                                openChatById(SAVED_CHAT_ID)
                                selectTab(1)
                            },
                        )
                    }

                    3 -> ProfileScreen(
                        profile = store.profile.value,
                        banned = settings.zsBanned,
                        admin = settings.serverRole == "admin" && settings.serverToken.isNotBlank(),
                        postCount = store.posts.count { it.author == myName },
                        chatCount = store.chats.count { it.id != SUPPORT_CHAT_ID && it.id != SAVED_CHAT_ID },
                        onSave = {
                            store.profile.value = it
                            pushProfile(withAvatar = false)
                        },
                        onAvatar = { uri ->
                            store.profile.value = store.profile.value.copy(avatarUri = uri)
                            pushProfile(withAvatar = true)
                        },
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
            onLogin = { showAuth = true },
            onLogout = { logout() },
        )
    }

    if (showAuroraProfile) {
        AuroraProfileScreen(serverUrl = settings.serverUrl, onBack = { showAuroraProfile = false })
    }

    CompositionLocalProvider(LocalZs provides zsCtx, LocalPeople provides people) {
        if (showPeople) {
            PeopleScreen(
                onDismiss = { showPeople = false },
                onWrite = { openPeer(it) },
                onOpenProfile = { login ->
                    if (login.equals(AURORA_LOGIN, ignoreCase = true)) showAuroraProfile = true else profileLogin = login
                },
                onChanged = { applyFriends(it) },
            )
        }
        profileLogin?.let { login ->
            UserProfileScreen(
                login = login,
                onBack = { profileLogin = null },
                onWrite = { openPeer(it) },
                onChanged = { scope.launch { refreshFriends() } },
            )
        }
    }

    if (showAuth) {
        AuthScreen(
            serverUrl = settings.serverUrl,
            onDone = { applySession(it) },
            onSkip = {
                showAuth = false
                setSettings { it.copy(authSkipped = true) }
            },
        )
    }
}
