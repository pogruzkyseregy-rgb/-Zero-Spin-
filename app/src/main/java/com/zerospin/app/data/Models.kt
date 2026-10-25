package com.zerospin.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Идентификатор встроенного канала поддержки «Аврора». */
const val SUPPORT_CHAT_ID = 0

/** Чат «Избранное»: сюда пересылаются сохранённые сообщения. */
const val SAVED_CHAT_ID = 999_999

/** Варианты таймера исчезающих сообщений: секунды и подпись. 0 — выключено. */
val TtlOptions = listOf(
    0 to "Выключено",
    3600 to "1 час",
    86_400 to "1 день",
    604_800 to "1 неделя",
)

fun ttlLabel(seconds: Int): String = TtlOptions.firstOrNull { it.first == seconds }?.second ?: "${seconds / 3600} ч"

/** Реакции, которые можно поставить на сообщение. */
val QuickReactions = listOf("❤️", "🔥", "😂", "👍", "😮", "😢")

enum class AttachKind { IMAGE, VIDEO, FILE, AUDIO }

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
    val comments: List<Comment> = emptyList(),
    /** Сохранена в закладки */
    val bookmarked: Boolean = false,
)

data class Comment(
    val id: Int,
    val author: String,
    val text: String,
    val time: String,
)

/** История («момент»): фото с подписью, живёт сутки. */
data class Story(
    val id: Int,
    val author: String,
    val uri: String,
    val caption: String,
    val createdAt: Long,
    val seen: Boolean = false,
)

/** Сколько живёт история. */
const val STORY_LIFETIME_MS = 24L * 60 * 60 * 1000

data class Chat(
    val id: Int,
    val name: String,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    /** Закреплённое сообщение внутри чата */
    val pinnedMessageId: Int? = null,
    /** Недописанный текст, сохраняется при выходе из чата */
    val draft: String = "",
    /** Непрочитанные входящие */
    val unread: Int = 0,
    /** Убран в архив: не мешает в общем списке */
    val archived: Boolean = false,
    /** Исчезающие сообщения: через сколько секунд удаляются, 0 — выключено */
    val ttlSeconds: Int = 0,
    /** Личка с другом через сервер: его логин. Пусто — чат живёт только на телефоне */
    val peer: String = "",
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
    /** Если сообщение переслано — от кого */
    val forwardedFrom: String? = null,
    /** Номер на сервере (личка). 0 — ещё не отправлено или чат только на телефоне */
    val remoteId: Int = 0,
    /** Не ушло на сервер */
    val failed: Boolean = false,
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
    COVER("ОБЛОЖКА", "Широкое фото вверху профиля", false),
    PHOTOS("ФОТО", "Ваши фото по кругу, листаются бесконечно", false),
    FRIENDS("ДРУЗЬЯ", "Список друзей по нику", false),
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
        ProfileBlock(BlockKind.COVER),
        ProfileBlock(BlockKind.QUOTE, "Тишина тоже сигнал"),
        ProfileBlock(BlockKind.ABOUT),
        ProfileBlock(BlockKind.STATS),
        ProfileBlock(BlockKind.PHOTOS),
        ProfileBlock(BlockKind.FRIENDS),
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
    /** Обложка профиля (широкое фото) или null */
    val coverUri: String? = null,
    /** Фото для карусели в профиле */
    val photos: List<Attachment> = emptyList(),
    /** Друзья: ники */
    val friends: List<String> = emptyList(),
    /** Украшение аватара (нимб, корона…), пусто — без него */
    val cosmetic: String = "",
)

data class AppSettings(
    val notifications: Boolean = true,
    val sounds: Boolean = true,
    val vibration: Boolean = true,
    val showOnline: Boolean = true,
    val readReceipts: Boolean = true,
    val openToAll: Boolean = true,
    val glitch: Boolean = true,
    /** Анимация квантового поля на фоне */
    val animatedBackground: Boolean = true,
    /** Хэш PIN-кода на вход, пусто — без блокировки */
    val pinHash: String = "",
    /** Сервер Zero Spin: адрес, ключ входа и ник, под которым зарегистрирован */
    val serverUrl: String = DEFAULT_SERVER,
    val serverToken: String = "",
    val serverNick: String = "",
    /** Предупреждения от Авроры и бан: так их видит сервер, приложение только показывает */
    val zsWarnings: Int = 0,
    val zsBanned: Boolean = false,
    val zsBanReason: String = "",
    /** Роль на сервере: user, admin или aurora */
    val serverRole: String = "",
    /** Человек выбрал «Позже» на экране входа: больше его не показываем, пока сам не откроет */
    val authSkipped: Boolean = false,
    /** Личка: номер последнего сообщения, которое телефон уже забрал с сервера */
    val syncAfter: Int = 0,
    /** Входящие заявки в друзья */
    val friendRequests: Int = 0,
)

/** Логин Авроры на сервере: её профиль и новости. */
const val AURORA_LOGIN = "Avrora_AV"

/** Сервер на машине Авроры: HTTPS через Caddy, адрес по IP. */
const val DEFAULT_SERVER = "https://37-220-86-132.sslip.io"

fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

fun nowMillis(): Long = System.currentTimeMillis()
