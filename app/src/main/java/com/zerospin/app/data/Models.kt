package com.zerospin.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Данные приложения. Пока без сервера: всё хранится в памяти, пустое до первого действия.

data class Post(
    val id: Int,
    val author: String,
    val time: String,
    val text: String,
    val likes: Int,
    val liked: Boolean,
)

data class Chat(
    val id: Int,
    val name: String,
)

data class ChatMessage(
    val fromMe: Boolean,
    val text: String,
    val time: String,
)

fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
