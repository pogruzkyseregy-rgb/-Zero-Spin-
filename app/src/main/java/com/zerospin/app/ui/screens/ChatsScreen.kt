package com.zerospin.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.SUPPORT_CHAT_ID
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.rememberAttachPicker
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
    openChatId: Int,
    onOpenChat: (Int) -> Unit,
    onCreateChat: () -> Unit,
    onSend: (Int, String, List<Attachment>) -> Unit,
) {
    val open = chats.firstOrNull { it.id == openChatId }

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
                )
            }
            if (chats.isEmpty()) {
                item { HudEmptyState("НЕТ КАНАЛОВ", "Создайте первый канал круга") }
            }
            items(chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    lastText = messages[chat.id].orEmpty().lastOrNull()?.text ?: "ожидание сигнала…",
                    onClick = { onOpenChat(chat.id) },
                )
            }
        }
    } else {
        Conversation(
            chat = open,
            messages = messages[open.id].orEmpty(),
            onBack = { onOpenChat(-1) },
            onSend = { text, attachments -> onSend(open.id, text, attachments) },
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
    onSend: (String, List<Attachment>) -> Unit,
) {
    BackHandler(onBack = onBack)

    var draft by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    val pick = rememberAttachPicker { pending = pending + it }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

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
            Column {
                Text(
                    chat.name.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                if (chat.id == SUPPORT_CHAT_ID) {
                    Text("поддержка · отвечает Аврора", color = Violet, fontSize = 10.sp)
                }
            }
        }

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                HudEmptyState("КАНАЛ ПУСТ", "Передайте первый сигнал")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { message -> MessageBubble(message) }
            }
        }

        if (pending.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            AttachmentList(pending, onRemove = { i -> pending = pending.filterIndexed { idx, _ -> idx != i } })
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .hudPanel(cut = 12.dp)
                    .clickable(onClick = pick),
                contentAlignment = Alignment.Center,
            ) {
                HudIcon(HudIconKind.PLUS, Cyan, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Сигнал…", color = Muted) },
                maxLines = 3,
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
                        if (draft.isNotBlank() || pending.isNotEmpty()) {
                            onSend(draft.trim(), pending)
                            draft = ""
                            pending = emptyList()
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
            if (message.text.isNotEmpty()) {
                Text(message.text, color = TextMain, fontSize = 14.sp)
            }
            if (message.attachments.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AttachmentList(message.attachments)
            }
            Text(message.time, color = Muted, fontSize = 10.sp)
        }
    }
}
