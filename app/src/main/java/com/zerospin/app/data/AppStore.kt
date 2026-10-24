package com.zerospin.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Всё состояние приложения в одном месте.
 * Изменения сразу видны интерфейсу, а ZeroSpinApp сохраняет их в SharedPreferences.
 * Новые поля читаются со значениями по умолчанию, поэтому старые данные не ломаются.
 */
class AppStore(context: Context) {

    private val prefs = context.getSharedPreferences("zerospin_store", Context.MODE_PRIVATE)

    val profile = mutableStateOf(Profile("Адонис", "adonis", "В сети", "", ""))
    val settings = mutableStateOf(AppSettings())
    val posts = mutableStateListOf<Post>()
    val stories = mutableStateListOf<Story>()
    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateMapOf<Int, List<ChatMessage>>()
    val notes = mutableStateListOf<Note>()
    val events = mutableStateListOf<Event>()
    val polls = mutableStateListOf<Poll>()

    init {
        prefs.getString("all", null)?.let { load(it) }
        if (chats.none { it.id == SUPPORT_CHAT_ID }) addSupportChat()
        if (chats.none { it.id == SAVED_CHAT_ID }) {
            chats.add(1.coerceAtMost(chats.size), Chat(SAVED_CHAT_ID, "Избранное", pinned = true))
            messages[SAVED_CHAT_ID] = emptyList()
        }
    }

    private fun addSupportChat() {
        chats.add(0, Chat(SUPPORT_CHAT_ID, "Аврора · поддержка", pinned = true))
        messages[SUPPORT_CHAT_ID] = listOf(
            ChatMessage(
                id = 1,
                fromMe = false,
                text = "Привет, я Аврора. Спросите, как пользоваться Zero Spin: профиль, чаты, истории, заметки, опросы.",
                time = nowTime(),
            ),
        )
    }

    /** Полный снимок состояния в JSON. Чтение всех состояний нужно, чтобы snapshotFlow реагировал на любые изменения. */
    fun toJsonString(): String {
        val p = profile.value
        val s = settings.value
        val root = JSONObject()
        root.put("profile", JSONObject()
            .put("name", p.name).put("nick", p.nick).put("status", p.status)
            .put("bio", p.bio).put("birthday", p.birthday).put("avatar", p.avatarUri ?: "")
            .put("blocks", JSONArray().apply {
                p.blocks.forEach { put(JSONObject().put("k", it.kind.name).put("t", it.text)) }
            })
            .put("cover", p.coverUri ?: "")
            .put("photos", attachmentsToJson(p.photos))
            .put("friends", JSONArray(p.friends)))
        root.put("settings", JSONObject()
            .put("notifications", s.notifications).put("sounds", s.sounds).put("vibration", s.vibration)
            .put("showOnline", s.showOnline).put("readReceipts", s.readReceipts)
            .put("openToAll", s.openToAll).put("glitch", s.glitch)
            .put("animatedBackground", s.animatedBackground).put("pinHash", s.pinHash))
        root.put("posts", JSONArray().apply {
            posts.forEach { post ->
                put(JSONObject().put("id", post.id).put("author", post.author).put("time", post.time)
                    .put("text", post.text).put("attachments", attachmentsToJson(post.attachments))
                    .put("likes", post.likes).put("liked", post.liked)
                    .put("reposts", post.reposts).put("reposted", post.reposted)
                    .put("repostOf", post.repostOf ?: JSONObject.NULL)
                    .put("bookmarked", post.bookmarked)
                    .put("comments", JSONArray().apply {
                        post.comments.forEach { c ->
                            put(JSONObject().put("id", c.id).put("author", c.author).put("text", c.text).put("time", c.time))
                        }
                    }))
            }
        })
        root.put("stories", JSONArray().apply {
            stories.forEach { st ->
                put(JSONObject().put("id", st.id).put("author", st.author).put("uri", st.uri)
                    .put("caption", st.caption).put("createdAt", st.createdAt).put("seen", st.seen))
            }
        })
        root.put("chats", JSONArray().apply {
            chats.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name)
                    .put("pinned", it.pinned).put("muted", it.muted)
                    .put("pinnedMessageId", it.pinnedMessageId ?: JSONObject.NULL)
                    .put("draft", it.draft).put("unread", it.unread)
                    .put("archived", it.archived).put("ttl", it.ttlSeconds))
            }
        })
        root.put("messages", JSONObject().apply {
            messages.forEach { (chatId, list) ->
                put(chatId.toString(), JSONArray().apply {
                    list.forEach { m ->
                        put(JSONObject()
                            .put("id", m.id).put("fromMe", m.fromMe).put("text", m.text).put("time", m.time)
                            .put("attachments", attachmentsToJson(m.attachments))
                            .put("replyTo", m.replyTo ?: JSONObject.NULL)
                            .put("myReaction", m.myReaction ?: JSONObject.NULL)
                            .put("reactionCount", m.reactionCount)
                            .put("edited", m.edited).put("read", m.read)
                            .put("createdAt", m.createdAt)
                            .put("forwardedFrom", m.forwardedFrom ?: JSONObject.NULL))
                    }
                })
            }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("updated", it.updated)) }
        })
        root.put("events", JSONArray().apply {
            events.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title).put("date", it.date)
                    .put("time", it.time).put("place", it.place).put("desc", it.desc))
            }
        })
        root.put("polls", JSONArray().apply {
            polls.forEach { poll ->
                put(JSONObject().put("id", poll.id).put("question", poll.question)
                    .put("options", JSONArray(poll.options))
                    .put("votes", JSONArray(poll.votes))
                    .put("myVotes", JSONArray(poll.myVotes))
                    .put("multiple", poll.multiple)
                    .put("closed", poll.closed)
                    .put("cover", poll.cover?.let { attachmentsToJson(listOf(it)).getJSONObject(0) } ?: JSONObject.NULL)
                    .put("attachments", attachmentsToJson(poll.attachments)))
            }
        })
        return root.toString()
    }

    fun save(json: String) {
        prefs.edit().putString("all", json).apply()
    }

    private fun load(raw: String) {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return

        root.optJSONObject("profile")?.let { o ->
            profile.value = Profile(
                name = o.optString("name", "Адонис"),
                nick = o.optString("nick", "adonis"),
                status = o.optString("status", ""),
                bio = o.optString("bio", ""),
                birthday = o.optString("birthday", ""),
                avatarUri = o.optString("avatar", "").ifBlank { null },
                blocks = o.optJSONArray("blocks")?.objects()?.mapNotNull { b ->
                    runCatching { BlockKind.valueOf(b.getString("k")) }.getOrNull()
                        ?.let { ProfileBlock(it, b.optString("t")) }
                } ?: BlockTemplates[1].second,
                coverUri = o.optString("cover", "").ifBlank { null },
                photos = attachmentsFromJson(o.optJSONArray("photos")),
                friends = o.optJSONArray("friends")?.strings() ?: emptyList(),
            )
        }
        root.optJSONObject("settings")?.let { o ->
            val d = AppSettings()
            settings.value = AppSettings(
                notifications = o.optBoolean("notifications", d.notifications),
                sounds = o.optBoolean("sounds", d.sounds),
                vibration = o.optBoolean("vibration", d.vibration),
                showOnline = o.optBoolean("showOnline", d.showOnline),
                readReceipts = o.optBoolean("readReceipts", d.readReceipts),
                openToAll = o.optBoolean("openToAll", d.openToAll),
                glitch = o.optBoolean("glitch", d.glitch),
                animatedBackground = o.optBoolean("animatedBackground", d.animatedBackground),
                pinHash = o.optString("pinHash", d.pinHash),
            )
        }
        root.optJSONArray("posts")?.objects()?.forEach { o ->
            posts.add(Post(
                id = o.getInt("id"), author = o.getString("author"), time = o.getString("time"),
                text = o.getString("text"), attachments = attachmentsFromJson(o.optJSONArray("attachments")),
                likes = o.optInt("likes"), liked = o.optBoolean("liked"),
                reposts = o.optInt("reposts"), reposted = o.optBoolean("reposted"),
                repostOf = if (o.isNull("repostOf")) null else o.getInt("repostOf"),
                comments = o.optJSONArray("comments")?.objects()?.map { c ->
                    Comment(c.getInt("id"), c.getString("author"), c.getString("text"), c.optString("time"))
                } ?: emptyList(),
                bookmarked = o.optBoolean("bookmarked"),
            ))
        }
        root.optJSONArray("stories")?.objects()?.forEach { o ->
            stories.add(Story(
                id = o.getInt("id"), author = o.getString("author"), uri = o.getString("uri"),
                caption = o.optString("caption"), createdAt = o.getLong("createdAt"), seen = o.optBoolean("seen"),
            ))
        }
        root.optJSONArray("chats")?.objects()?.forEach { o ->
            chats.add(Chat(
                id = o.getInt("id"),
                name = o.getString("name"),
                pinned = o.optBoolean("pinned"),
                muted = o.optBoolean("muted"),
                pinnedMessageId = if (o.isNull("pinnedMessageId")) null else o.optInt("pinnedMessageId"),
                draft = o.optString("draft"),
                unread = o.optInt("unread"),
                archived = o.optBoolean("archived"),
                ttlSeconds = o.optInt("ttl"),
            ))
        }
        root.optJSONObject("messages")?.let { obj ->
            obj.keys().forEach { key ->
                val list = obj.getJSONArray(key).objects().mapIndexed { index, o ->
                    ChatMessage(
                        id = o.optInt("id", index + 1),
                        fromMe = o.optBoolean("fromMe"),
                        text = o.getString("text"),
                        time = o.optString("time"),
                        attachments = attachmentsFromJson(o.optJSONArray("attachments")),
                        replyTo = if (o.isNull("replyTo")) null else o.getInt("replyTo"),
                        myReaction = if (o.isNull("myReaction")) null else o.getString("myReaction"),
                        reactionCount = o.optInt("reactionCount"),
                        edited = o.optBoolean("edited"),
                        read = o.optBoolean("read"),
                        createdAt = o.optLong("createdAt", nowMillis()),
                        forwardedFrom = if (o.isNull("forwardedFrom")) null else o.optString("forwardedFrom"),
                    )
                }
                messages[key.toInt()] = list
            }
        }
        root.optJSONArray("notes")?.objects()?.forEach { o ->
            notes.add(Note(o.getInt("id"), o.getString("title"), o.getString("body"), o.getLong("updated")))
        }
        root.optJSONArray("events")?.objects()?.forEach { o ->
            events.add(Event(o.getInt("id"), o.getString("title"), o.getString("date"),
                o.optString("time"), o.optString("place"), o.optString("desc")))
        }
        root.optJSONArray("polls")?.objects()?.forEach { o ->
            polls.add(Poll(
                id = o.getInt("id"), question = o.getString("question"),
                options = o.getJSONArray("options").strings(),
                votes = o.getJSONArray("votes").ints(),
                myVotes = o.optJSONArray("myVotes")?.ints()
                    ?: if (o.isNull("myVote")) emptyList() else listOf(o.getInt("myVote")),
                multiple = o.optBoolean("multiple"),
                closed = o.optBoolean("closed"),
                cover = if (o.isNull("cover")) null else attachmentsFromJson(JSONArray().put(o.getJSONObject("cover"))).firstOrNull(),
                attachments = attachmentsFromJson(o.optJSONArray("attachments")),
            ))
        }
    }

    private fun attachmentsToJson(list: List<Attachment>): JSONArray = JSONArray().apply {
        list.forEach { put(JSONObject().put("k", it.kind.name).put("u", it.uri).put("n", it.name).put("m", it.mime)) }
    }

    private fun attachmentsFromJson(arr: JSONArray?): List<Attachment> =
        arr?.objects()?.map { o ->
            Attachment(AttachKind.valueOf(o.getString("k")), o.getString("u"), o.getString("n"), o.optString("m"))
        } ?: emptyList()

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }
}
