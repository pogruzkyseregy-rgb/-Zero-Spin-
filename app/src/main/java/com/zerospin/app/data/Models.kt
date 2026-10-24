package com.zerospin.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Идентификатор встроенного канала поддержки «Аврора». */
const val SUPPORT_CHAT_ID = 0

/** Реакции, которые можно поставить на сообщение. */
val QuickReactions = listOf("❤️", "🔥", "😂", "👍", "😮", "😢")

enum class AttachKind { IMAGE, VIDEO, FILE }

/** Вложение: ссылка на файл на телефоне (content:// uri), сам файл не копируется. */
data class Attachment(
    val kind: AttachKind,
    val uri: String,
    val name: String,
    val mime: String,
)

data class Post(
    val id: Int,
    val author: String,
    val time: String,
    val text: String,
    val attachments: List<Attachment>,
    val likes: Int,
    val liked: Boolean,
    val reposts: Int = 0,
    val reposted: Boolean = false,
    /** id исходной записи, если это репост */
    val repostOf: Int? = null,
)

data class Chat(
    val id: Int,
    val name: String,
    val pinned: Boolean = false,
    val muted: Boolean = false,
)

data class ChatMessage(
    val id: Int,
    val fromMe: Boolean,
    val text: String,
    val time: String,
    val attachments: List<Attachment> = emptyList(),
    /** id сообщения, на которое это ответ, или null */
    val replyTo: Int? = null,
    /** Реакция, которую поставил я */
    val myReaction: String? = null,
    val reactionCount: Int = 0,
    val edited: Boolean = false,
    /** Прочитано собеседником (для галочек у моих сообщений) */
    val read: Boolean = false,
    val createdAt: Long = nowMillis(),
)

data class Note(
    val id: Int,
    val title: String,
    val body: String,
    val updated: Long,
)

/** date — yyyy-MM-dd, time — HH:mm или пусто */
data class Event(
    val id: Int,
    val title: String,
    val date: String,
    val time: String,
    val place: String,
    val desc: String,
)

data class Poll(
    val id: Int,
    val question: String,
    val options: List<String>,
    val votes: List<Int>,
    /** Индексы вариантов, за которые проголосовал я (один или несколько) */
    val myVotes: List<Int> = emptyList(),
    /** Можно выбрать несколько вариантов */
    val multiple: Boolean = false,
    /** Опрос закрыт: голосовать больше нельзя */
    val closed: Boolean = false,
    /** Фото на фон опроса */
    val cover: Attachment? = null,
    /** Прикреплённые фото и файлы */
    val attachments: List<Attachment> = emptyList(),
)

/** Виды блоков профиля. Текст берётся из полей профиля или из самого блока. */
enum class BlockKind(val title: String, val hint: String, val hasText: Boolean) {
    ABOUT("О СЕБЕ", "Текст из поля «О себе»", false),
    STATS("СТАТИСТИКА", "Записи и каналы с прогресс-полосами", false),
    BIRTHDAY("ДАТА РОЖДЕНИЯ", "Из поля «Дата рождения»", false),
    QUOTE("ЦИТАТА", "Ваша фраза или девиз", true),
    LOCATION("ГОРОД", "Где вы сейчас или откуда", true),
    LINK("ССЫЛКА", "Канал, сайт или контакт", true),
}

/** Блок профиля: вид и свой текст (только для видов с hasText). */
data class ProfileBlock(
    val kind: BlockKind,
    val text: String = "",
)

/** Готовые наборы блоков: одно нажатие — и профиль перестроен. */
val BlockTemplates: List<Pair<String, List<ProfileBlock>>> = listOf(
    "Минимал" to listOf(
        ProfileBlock(BlockKind.ABOUT),
        ProfileBlock(BlockKind.BIRTHDAY),
    ),
    "Аврора" to listOf(
        ProfileBlock(BlockKind.QUOTE, "Тишина тоже сигнал"),
        ProfileBlock(BlockKind.STATS),
        ProfileBlock(BlockKind.ABOUT),
        ProfileBlock(BlockKind.BIRTHDAY),
    ),
    "Полный" to listOf(
        ProfileBlock(BlockKind.QUOTE, "Тишина тоже сигнал"),
        ProfileBlock(BlockKind.ABOUT),
        ProfileBlock(BlockKind.STATS),
        ProfileBlock(BlockKind.BIRTHDAY),
        ProfileBlock(BlockKind.LOCATION),
        ProfileBlock(BlockKind.LINK),
    ),
)

/** birthday — yyyy-MM-dd или пусто */
data class Profile(
    val name: String,
    val nick: String,
    val status: String,
    val bio: String,
    val birthday: String,
    /** Фото профиля (content:// uri) или null */
    val avatarUri: String? = null,
    /** Блоки профиля в порядке показа сверху вниз */
    val blocks: List<ProfileBlock> = BlockTemplates[1].second,
)

data class AppSettings(
    val notifications: Boolean = true,
    val sounds: Boolean = true,
    val vibration: Boolean = true,
    val showOnline: Boolean = true,
    val readReceipts: Boolean = true,
    val openToAll: Boolean = true,
    val glitch: Boolean = true,
)

fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

fun nowMillis(): Long = System.currentTimeMillis()
