package com.zerospin.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Всё состояние приложения в одном месте.
 * Изменения видны интерфейсу сразу, а сохранение в SharedPreferences делает ZeroSpinApp при каждом изменении.
 */
class AppStore(context: Context) {

    private val prefs = context.getSharedPreferences("zerospin_store", Context.MODE_PRIVATE)

    val profile = mutableStateOf(Profile("Адонис", "adonis", "В сети", "", ""))
    val settings = mutableStateOf(AppSettings(notifications = true, sounds = true, showOnline = true, openToAll = true))
    val posts = mutableStateListOf<Post>()
    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateMapOf<Int, List<ChatMessage>>()
    val notes = mutableStateListOf<Note>()
    val events = mutableStateListOf<Event>()
    val polls = mutableStateListOf<Poll>()

    init {
        prefs.getString("all", null)?.let { load(it) }
        if (chats.isEmpty()) addSupportChat()
    }

    private fun addSupportChat() {
        chats.add(0, Chat(SUPPORT_CHAT_ID, "Аврора · поддержка"))
        messages[SUPPORT_CHAT_ID] = listOf(
            ChatMessage(false, "Привет, я Аврора. Спросите, как пользоваться Zero Spin: профиль, чаты, заметки, события, опросы.", nowTime()),
        )
    }

    /** Полный снимок состояния в JSON. Читает все состояния, поэтому snapshotFlow реагирует на любые изменения. */
    fun toJsonString(): String {
        val p = profile.value
        val s = settings.value
        val root = JSONObject()
        root.put("profile", JSONObject()
            .put("name", p.name).put("nick", p.nick).put("status", p.status)
            .put("bio", p.bio).put("birthday", p.birthday))
        root.put("settings", JSONObject()
            .put("notifications", s.notifications).put("sounds", s.sounds)
            .put("showOnline", s.showOnline).put("openToAll", s.openToAll))
        root.put("posts", JSONArray().apply {
            posts.forEach { post ->
                put(JSONObject().put("id", post.id).put("author", post.author).put("time", post.time)
                    .put("text", post.text).put("attachments", attachmentsToJson(post.attachments))
                    .put("likes", post.likes).put("liked", post.liked))
            }
        })
        root.put("chats", JSONArray().apply {
            chats.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
        })
        root.put("messages", JSONObject().apply {
            messages.forEach { (chatId, list) ->
                put(chatId.toString(), JSONArray().apply {
                    list.forEach { m ->
                        put(JSONObject().put("fromMe", m.fromMe).put("text", m.text).put("time", m.time)
                            .put("attachments", attachmentsToJson(m.attachments)))
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
                    .put("myVote", poll.myVote ?: JSONObject.NULL))
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
            )
        }
        root.optJSONObject("settings")?.let { o ->
            settings.value = AppSettings(
                notifications = o.optBoolean("notifications", true),
                sounds = o.optBoolean("sounds", true),
                showOnline = o.optBoolean("showOnline", true),
                openToAll = o.optBoolean("openToAll", true),
            )
        }
        root.optJSONArray("posts")?.objects()?.forEach { o ->
            posts.add(Post(
                id = o.getInt("id"), author = o.getString("author"), time = o.getString("time"),
                text = o.getString("text"), attachments = attachmentsFromJson(o.optJSONArray("attachments")),
                likes = o.optInt("likes"), liked = o.optBoolean("liked"),
            ))
        }
        root.optJSONArray("chats")?.objects()?.forEach { o ->
            chats.add(Chat(o.getInt("id"), o.getString("name")))
        }
        root.optJSONObject("messages")?.let { obj ->
            obj.keys().forEach { key ->
                val list = obj.getJSONArray(key).objects().map { o ->
                    ChatMessage(
                        fromMe = o.optBoolean("fromMe"), text = o.getString("text"), time = o.optString("time"),
                        attachments = attachmentsFromJson(o.optJSONArray("attachments")),
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
                myVote = if (o.isNull("myVote")) null else o.getInt("myVote"),
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
