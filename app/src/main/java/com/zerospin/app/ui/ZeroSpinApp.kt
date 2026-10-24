package com.zerospin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.Post
import com.zerospin.app.data.nowTime
import com.zerospin.app.ui.hud.HudBackground
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudTab
import com.zerospin.app.ui.hud.HudTabBar
import com.zerospin.app.ui.hud.HudTopBar
import com.zerospin.app.ui.screens.AppsScreen
import com.zerospin.app.ui.screens.ChatsScreen
import com.zerospin.app.ui.screens.FeedScreen
import com.zerospin.app.ui.screens.ProfileScreen
import com.zerospin.app.ui.screens.SettingsDialog

private const val MY_NAME = "Адонис"

@Composable
fun ZeroSpinApp() {
    val tabs = listOf(
        HudTab("ЛЕНТА", HudIconKind.FEED),
        HudTab("ЧАТ", HudIconKind.CHAT),
        HudTab("ПРИЛОЖЕНИЯ", HudIconKind.APPS),
        HudTab("ПРОФИЛЬ", HudIconKind.PROFILE),
    )

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Данные приложения в памяти, сервера пока нет
    val posts = remember { mutableStateListOf<Post>() }
    val chats = remember { mutableStateListOf<Chat>() }
    val messages = remember { mutableStateMapOf<Int, List<ChatMessage>>() }

    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HudTopBar(
                    title = tabs[tab].title,
                    status = "● ONLINE · КРУГ",
                    onSettings = { showSettings = true },
                )
            },
            bottomBar = {
                HudTabBar(tabs = tabs, selected = tab, onSelect = { tab = it })
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> FeedScreen(
                        posts = posts,
                        onPublish = { text ->
                            posts.add(
                                0,
                                Post(
                                    id = (posts.maxOfOrNull { it.id } ?: 0) + 1,
                                    author = MY_NAME,
                                    time = nowTime(),
                                    text = text,
                                    likes = 0,
                                    liked = false,
                                ),
                            )
                        },
                        onLike = { id ->
                            val index = posts.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val p = posts[index]
                                posts[index] = p.copy(
                                    liked = !p.liked,
                                    likes = if (p.liked) p.likes - 1 else p.likes + 1,
                                )
                            }
                        },
                    )
                    1 -> ChatsScreen(
                        chats = chats,
                        messages = messages,
                        onCreateChat = {
                            val id = (chats.maxOfOrNull { it.id } ?: 0) + 1
                            chats.add(Chat(id, "Канал $id"))
                        },
                        onSend = { chatId, text ->
                            messages[chatId] = messages[chatId].orEmpty() +
                                ChatMessage(fromMe = true, text = text, time = nowTime())
                        },
                    )
                    2 -> AppsScreen()
                    3 -> ProfileScreen(
                        postCount = posts.count { it.author == MY_NAME },
                        chatCount = chats.size,
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
}
