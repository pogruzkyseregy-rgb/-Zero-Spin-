package com.zerospin.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.Poll
import com.zerospin.app.data.nextId
import com.zerospin.app.ui.components.AttachmentList
import com.zerospin.app.ui.components.rememberCoverPicker
import com.zerospin.app.ui.components.rememberFilePicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.components.rememberThumb
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlin.math.roundToInt

@Composable
fun PollsScreen(
    polls: List<Poll>,
    onAdd: (Poll) -> Unit,
    onVote: (pollId: Int, option: Int) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }

    if (adding) {
        PollForm(
            nextId = nextId(polls.map { it.id }),
            onSave = { onAdd(it); adding = false },
            onCancel = { adding = false },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HudButton(
                text = "+ Новый опрос",
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
            )
        }
        if (polls.isEmpty()) {
            item { HudEmptyState("ОПРОСОВ НЕТ", "Спросите круг о чём-нибудь") }
        }
        items(polls, key = { it.id }) { poll ->
            PollCard(poll = poll, onVote = { option -> onVote(poll.id, option) })
        }
    }
}

/** Один блок: фото на фоне, вопрос, варианты-полоски с процентами внутри и вложения. */
@Composable
private fun PollCard(poll: Poll, onVote: (Int) -> Unit) {
    val total = poll.votes.sum()
    val cover = poll.cover?.let { rememberThumb(it.uri, 1080) }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().hudPanel(cut = 14.dp)) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // Затемнение сверху вниз, чтобы текст читался поверх фото
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(listOf(Bg.copy(alpha = 0.45f), Bg.copy(alpha = 0.94f)))),
                )
            }
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("ОПРОС", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(poll.question, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                poll.options.forEachIndexed { index, option ->
                    OptionBar(
                        option = option,
                        count = poll.votes.getOrElse(index) { 0 },
                        total = total,
                        mine = poll.myVote == index,
                        onClick = { onVote(index) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "голосов: $total · нажмите вариант, чтобы проголосовать или снять голос",
                    color = Muted,
                    fontSize = 10.sp,
                )
            }
        }
        if (poll.attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            AttachmentList(poll.attachments)
        }
    }
}

@Composable
private fun OptionBar(option: String, count: Int, total: Int, mine: Boolean, onClick: () -> Unit) {
    val fraction = if (total == 0) 0f else count.toFloat() / total
    val percent = (fraction * 100).roundToInt()
    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .hudPanel(line = if (mine) Cyan else Muted, fill = Bg.copy(alpha = 0.7f), cut = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(if (mine) Cyan.copy(alpha = 0.4f) else Cyan.copy(alpha = 0.14f)),
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (mine) "▲ $option" else option,
                color = if (mine) Cyan else TextMain,
                fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("$percent%  ·  $count", color = if (mine) Cyan else Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PollForm(nextId: Int, onSave: (Poll) -> Unit, onCancel: () -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    var options by rememberSaveable { mutableStateOf("") }
    var cover by remember { mutableStateOf<Attachment?>(null) }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }

    val pickCover = rememberCoverPicker { cover = it }
    val pickPhoto = rememberPhotoPicker { attachments = attachments + it }
    val pickFile = rememberFilePicker { attachments = attachments + it }
    val coverPreview = cover?.let { rememberThumb(it.uri, 400) }

    val list = options.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val questionError = if (question.isBlank()) "Задайте вопрос" else null
    val optionsError = if (list.size < 2) "Нужно минимум два варианта" else null
    val canSave = questionError == null && optionsError == null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        HudTextField(
            value = question, onValueChange = { question = it }, label = "Вопрос",
            placeholder = "Во сколько встречаемся?", error = questionError,
            singleLine = false, minLines = 2, maxLength = 140, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        HudTextField(
            value = options, onValueChange = { options = it }, label = "Варианты",
            placeholder = "18:00; 19:30; 21:00",
            hint = "Варианты разделяйте знаком «;»",
            error = optionsError, singleLine = false, minLines = 2, maxLength = 300,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        // Фото на фон: превью и кнопка
        Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(12.dp)) {
            Text("ФОТО НА ФОН", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            if (coverPreview != null) {
                Box(Modifier.fillMaxWidth().height(140.dp)) {
                    Image(
                        bitmap = coverPreview,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        "×",
                        color = Red,
                        fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clickable { cover = null },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            HudButton(
                text = if (cover == null) "Выбрать фото на фон" else "Заменить фото",
                onClick = pickCover,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("ВЛОЖЕНИЯ", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        if (attachments.isNotEmpty()) {
            AttachmentList(attachments, onRemove = { i -> attachments = attachments.filterIndexed { idx, _ -> idx != i } })
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(text = "+ Фото", onClick = pickPhoto, modifier = Modifier.weight(1f))
            HudButton(text = "+ Файл", onClick = pickFile, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(text = "Назад", onClick = onCancel, modifier = Modifier.weight(1f))
            HudButton(
                text = "Создать",
                onClick = {
                    if (canSave) {
                        onSave(
                            Poll(
                                id = nextId,
                                question = question.trim(),
                                options = list,
                                votes = List(list.size) { 0 },
                                myVote = null,
                                cover = cover,
                                attachments = attachments,
                            ),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                primary = true,
            )
        }
    }
}
