package com.zerospin.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Идентификатор встроенного канала поддержки «Аврора». */
const val SUPPORT_CHAT_ID = 0

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
)

data class Chat(
    val id: Int,
    val name: String,
)

data class ChatMessage(
    val fromMe: Boolean,
    val text: String,
    val time: String,
    val attachments: List<Attachment> = emptyList(),
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
    val myVote: Int?,
)

/** birthday — yyyy-MM-dd или пусто */
data class Profile(
    val name: String,
    val nick: String,
    val status: String,
    val bio: String,
    val birthday: String,
)

data class AppSettings(
    val notifications: Boolean,
    val sounds: Boolean,
    val showOnline: Boolean,
    val openToAll: Boolean,
)

fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

fun nowMillis(): Long = System.currentTimeMillis()
