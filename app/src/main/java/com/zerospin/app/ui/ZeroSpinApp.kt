package com.zerospin.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.R
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.Post
import com.zerospin.app.data.SampleChats
import com.zerospin.app.data.SampleMessages
import com.zerospin.app.data.SamplePosts
import com.zerospin.app.ui.screens.ChatsScreen
import com.zerospin.app.ui.screens.FeedScreen
import com.zerospin.app.ui.screens.GalleryScreen
import com.zerospin.app.ui.screens.ProfileScreen
import com.zerospin.app.ui.screens.SettingsDialog
import com.zerospin.app.ui.theme.AuroraBrush
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted

private data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun ZeroSpinApp() {
    val tabs = listOf(
        TabItem("ЛЕНТА", Icons.Default.Home),
        TabItem("ЧАТ", Icons.Default.Email),
        TabItem("ГАЛЕРЕЯ", Icons.Default.Image),
        TabItem("ПРОФИЛЬ", Icons.Default.Person),
    )

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Общие данные приложения (пока в памяти, без сервера)
    val posts = remember { mutableStateListOf<Post>().apply { addAll(SamplePosts) } }
    val chatMessages = remember {
        mutableStateMapOf<Int, List<ChatMessage>>().apply { putAll(SampleMessages) }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            AppTopBar(
                title = tabs[tab].title,
                onSettings = { showSettings = true },
            )
        },
        bottomBar = {
            Column {
                Box(Modifier.fillMaxWidth().height(2.dp).background(AuroraBrush))
                NavigationBar(containerColor = Bg) {
                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 10.sp, letterSpacing = 0.5.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan,
                                selectedTextColor = Cyan,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = Cyan.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxWidth()) {
            when (tab) {
                0 -> FeedScreen(posts = posts)
                1 -> ChatsScreen(chats = SampleChats, messages = chatMessages)
                2 -> GalleryScreen()
                3 -> ProfileScreen(
                    postCount = posts.count { it.author == "Адонис" },
                    chatCount = SampleChats.size,
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun AppTopBar(title: String, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().background(Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_av),
                contentDescription = "AV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 38.dp, height = 27.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Muted)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(AuroraBrush))
    }
}
