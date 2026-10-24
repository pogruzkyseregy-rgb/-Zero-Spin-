package com.zerospin.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

@Composable
fun ChatsScreen(
    chats: List<Chat>,
    messages: Map<Int, List<ChatMessage>>,
    onCreateChat: () -> Unit,
    onSend: (Int, String) -> Unit,
) {
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    val open = chats.firstOrNull { it.id == openId }

    // Системная кнопка «назад» закрывает открытый канал, а не приложение
    BackHandler(enabled = open != null) { openId = null }

    if (open == null) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HudButton(
                    text = "+ Новый канал",
                    onClick = onCreateChat,
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                )
            }
            if (chats.isEmpty()) {
                item { HudEmptyState("НЕТ КАНАЛОВ", "Создайте первый канал круга") }
            }
            items(chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    lastText = messages[chat.id].orEmpty().lastOrNull()?.text ?: "ожидание сигнала…",
                    onClick = { openId = chat.id },
                )
            }
        }
    } else {
        Conversation(
            chat = open,
            messages = messages[open.id].orEmpty(),
            onBack = { openId = null },
            onSend = { text -> onSend(open.id, text) },
        )
    }
}

@Composable
private fun ChatRow(chat: Chat, lastText: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(cut = 12.dp)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TargetRing(Modifier.size(44.dp)) {
            Text(chat.name.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                chat.name.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = lastText,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Conversation(
    chat: Chat,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .hudPanel(cut = 9.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                chat.name.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                HudEmptyState("КАНАЛ ПУСТ", "Передайте первый сигнал")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { message -> MessageBubble(message) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Сигнал…", color = Muted) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = hudFieldColors(),
                shape = RoundedCornerShape(6.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .hudPanel(line = Cyan, fill = Cyan, cut = 12.dp)
                    .clickable {
                        if (draft.isNotBlank()) {
                            onSend(draft.trim())
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.SEND, Bg, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .hudPanel(
                    line = if (message.fromMe) Cyan else Violet,
                    fill = if (message.fromMe) Color(0xFF0B2A36) else Panel,
                    cut = 10.dp,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(message.text, color = TextMain, fontSize = 14.sp)
            Text(message.time, color = Muted, fontSize = 10.sp)
        }
    }
}
