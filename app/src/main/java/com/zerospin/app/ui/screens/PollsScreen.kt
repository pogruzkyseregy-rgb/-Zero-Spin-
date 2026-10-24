package com.zerospin.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
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

private const val MAX_OPTIONS = 10
private const val MIN_OPTIONS = 2

@Composable
fun PollsScreen(
    polls: List<Poll>,
    onAdd: (Poll) -> Unit,
    onVote: (pollId: Int, option: Int) -> Unit,
    onToggleClosed: (pollId: Int, closed: Boolean) -> Unit,
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
            PollCard(
                poll = poll,
                onVote = { option -> onVote(poll.id, option) },
                onToggleClosed = { onToggleClosed(poll.id, !poll.closed) },
            )
        }
    }
}

/**
 * Карточка опроса в духе Telegram и WhatsApp: до голоса видны только варианты с кружками
 * (или квадратами для нескольких ответов). После голоса появляются проценты и полоски.
 */
@Composable
private fun PollCard(poll: Poll, onVote: (Int) -> Unit, onToggleClosed: () -> Unit) {
    val total = poll.votes.sum()
    val cover = poll.cover?.let { rememberThumb(it.uri, 1080) }
    val voted = poll.myVotes.isNotEmpty()
    val showResults = voted || poll.closed
    val maxVotes = poll.votes.maxOrNull() ?: 0
    val leaders = if (maxVotes > 0) poll.votes.indices.filter { poll.votes[it] == maxVotes }.toSet() else emptySet()

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().hudPanel(cut = 16.dp)) {
            if (cover != null) {
                // Обложка в собственной рамке с отступом: не заходит на срезанные углы панели
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .height(130.dp)
                        .border(1.dp, Muted.copy(alpha = 0.5f), RectangleShape),
                )
            }
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ОПРОС", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    if (poll.closed) {
                        Tag("ЗАКРЫТ", Red)
                    } else {
                        Tag(if (poll.multiple) "НЕСКОЛЬКО" else "ОДИН ОТВЕТ", Muted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(poll.question, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(14.dp))

                poll.options.forEachIndexed { index, option ->
                    OptionRow(
                        option = option,
                        count = poll.votes.getOrElse(index) { 0 },
                        total = total,
                        selected = index in poll.myVotes,
                        leading = index in leaders && total > 0,
                        showResults = showResults,
                        multiple = poll.multiple,
                        enabled = !poll.closed,
                        onClick = { onVote(index) },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            poll.closed -> "Голосование завершено · ${total} гол."
                            voted -> "Ваш выбор учтён · ${total} гол."
                            poll.multiple -> "Отметьте один или несколько · ${total} гол."
                            else -> "Выберите вариант · ${total} гол."
                        },
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (poll.closed) "ОТКРЫТЬ" else "ЗАКРЫТЬ",
                        color = Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable(onClick = onToggleClosed).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
            }
        }
        if (poll.attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            AttachmentList(poll.attachments)
        }
    }
}

@Composable
private fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.6f), RectangleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Вариант: индикатор выбора слева, после голоса — полоска с процентом внутри. */
@Composable
private fun OptionRow(
    option: String,
    count: Int,
    total: Int,
    selected: Boolean,
    leading: Boolean,
    showResults: Boolean,
    multiple: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val fraction = if (total == 0) 0f else count.toFloat() / total
    val percent = (fraction * 100).roundToInt()
    val accent = if (selected || leading) Cyan else Muted
    val indicatorShape = if (multiple) RoundedCornerShape(4.dp) else CircleShape

    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .hudPanel(line = accent, fill = Bg.copy(alpha = 0.7f), cut = 8.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (showResults) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (selected) Cyan.copy(alpha = 0.32f) else Cyan.copy(alpha = 0.10f)),
            )
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .border(1.5.dp, accent, indicatorShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(Modifier.size(10.dp).background(Cyan, indicatorShape))
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(
                option,
                color = if (selected || leading) Cyan else TextMain,
                fontWeight = if (selected || leading) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showResults) {
                Text(
                    "$percent%",
                    color = if (selected) Cyan else Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun PollForm(nextId: Int, onSave: (Poll) -> Unit, onCancel: () -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    // Варианты — отдельные поля, их можно добавлять и удалять по одному
    var options by remember { mutableStateOf(listOf("", "")) }
    var multiple by rememberSaveable { mutableStateOf(false) }
    var cover by remember { mutableStateOf<Attachment?>(null) }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }

    val pickCover = rememberCoverPicker { cover = it }
    val pickPhoto = rememberPhotoPicker { attachments = attachments + it }
    val pickFile = rememberFilePicker { attachments = attachments + it }
    val coverPreview = cover?.let { rememberThumb(it.uri, 400) }

    val cleanOptions = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val questionError = if (question.isBlank()) "Задайте вопрос" else null
    val optionsError = if (cleanOptions.size < MIN_OPTIONS) "Заполните минимум два разных варианта" else null
    val canSave = questionError == null && optionsError == null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        Text("НОВЫЙ ОПРОС", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        HudTextField(
            value = question, onValueChange = { question = it }, label = "Вопрос",
            placeholder = "Во сколько встречаемся?", error = questionError,
            singleLine = false, minLines = 2, maxLength = 140, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))

        Text("ТИП ОТВЕТА", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(
                text = "Один ответ",
                onClick = { multiple = false },
                modifier = Modifier.weight(1f),
                primary = !multiple,
            )
            HudButton(
                text = "Несколько",
                onClick = { multiple = true },
                modifier = Modifier.weight(1f),
                primary = multiple,
            )
        }
        Spacer(Modifier.height(14.dp))

        Text("ВАРИАНТЫ", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        options.forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                HudTextField(
                    value = value,
                    onValueChange = { text -> options = options.toMutableList().also { it[index] = text } },
                    label = "Вариант ${index + 1}",
                    placeholder = if (index == 0) "18:00" else "Ещё вариант",
                    maxLength = 60,
                    modifier = Modifier.weight(1f),
                )
                if (options.size > MIN_OPTIONS) {
                    Text(
                        "×",
                        color = Red,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(start = 10.dp, top = 20.dp)
                            .clickable { options = options.filterIndexed { i, _ -> i != index } },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (options.size < MAX_OPTIONS) {
            HudButton(
                text = "+ Добавить вариант",
                onClick = { options = options + "" },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (optionsError != null && options.any { it.isNotBlank() }) {
            Spacer(Modifier.height(6.dp))
            Text(optionsError, color = Red, fontSize = 10.sp)
        }

        Spacer(Modifier.height(16.dp))

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
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Bg.copy(alpha = 0.7f))
                            .clickable { cover = null }
                            .padding(horizontal = 6.dp),
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
                                options = cleanOptions,
                                votes = List(cleanOptions.size) { 0 },
                                multiple = multiple,
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
