package com.zerospin.app.data

import androidx.compose.ui.graphics.Color
import com.zerospin.app.R
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Pink
import com.zerospin.app.ui.theme.Violet

// Пока данные тестовые. Позже сюда подключится сервер.

data class Post(
    val id: Int,
    val author: String,
    val time: String,
    val text: String,
    val imageRes: Int?,
    val likes: Int,
    val liked: Boolean,
    val accent: Color,
)

data class Chat(
    val id: Int,
    val name: String,
    val accent: Color,
    val unread: Int,
)

data class ChatMessage(
    val fromMe: Boolean,
    val text: String,
    val time: String,
)

val SamplePosts = listOf(
    Post(1, "Аврора", "21 окт., 20:29",
        "Нашла: картинки на месте, звука ноль. Так и живу: не верю «работает», пока не увижу сама.",
        imageRes = R.drawable.hud_cyan_targets, likes = 1, liked = true, accent = Cyan),
    Post(2, "Адонис", "20 окт., 14:11",
        "мое любимое фото",
        imageRes = R.drawable.hud_purple_error, likes = 4, liked = false, accent = Violet),
    Post(3, "Лена", "19 окт., 09:40",
        "Кто на выходных? Хочу собраться на пару часов.",
        imageRes = null, likes = 2, liked = false, accent = Pink),
    Post(4, "Макс", "18 окт., 22:05",
        "Выложил новый трек, послушайте в галерее.",
        imageRes = null, likes = 0, liked = false, accent = Color(0xFFFFB84D)),
)

val GalleryImages: List<Int> = listOf(
    R.drawable.hud_cyan_targets,
    R.drawable.hud_gothic_ui,
    R.drawable.hud_cyan_frames,
    R.drawable.hud_future_ui,
    R.drawable.hud_white_frames,
    R.drawable.hud_bars,
    R.drawable.hud_lines,
    R.drawable.hud_purple_error,
    R.drawable.hud_red_attention,
)

val SampleChats = listOf(
    Chat(1, "Круг", Cyan, unread = 2),
    Chat(2, "Аврора", Violet, unread = 0),
    Chat(3, "Лена", Pink, unread = 1),
)

val SampleMessages: Map<Int, List<ChatMessage>> = mapOf(
    1 to listOf(
        ChatMessage(false, "Всем привет!", "20:10"),
        ChatMessage(true, "Привет, как дела?", "20:12"),
        ChatMessage(false, "Собираемся в субботу?", "20:15"),
    ),
    2 to listOf(
        ChatMessage(false, "Я проверила сборку. Звук всё ещё молчит.", "19:02"),
    ),
    3 to listOf(
        ChatMessage(false, "Скинь ссылку на APK", "18:30"),
    ),
)
