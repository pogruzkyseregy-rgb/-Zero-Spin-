package com.zerospin.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Event
import com.zerospin.app.data.nextId
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

private val DateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val TimeRegex = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

@Composable
fun EventsScreen(
    events: List<Event>,
    onAdd: (Event) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val sorted = events.sortedWith(compareBy({ it.date }, { it.time }))

    if (adding) {
        EventForm(
            nextId = nextId(events.map { it.id }),
            onSave = { onAdd(it); adding = false },
            onCancel = { adding = false },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HudButton(
                text = "+ Новое событие",
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
            )
        }
        if (sorted.isEmpty()) {
            item { HudEmptyState("СОБЫТИЙ НЕТ", "Отметьте встречу круга") }
        }
        items(sorted, key = { it.id }) { event ->
            Row(
                Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOf(event.date, event.time).filter { it.isNotBlank() }.joinToString(" · "),
                        color = Violet,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(event.title.uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (event.place.isNotBlank()) {
                        Text(event.place, color = TextMain, fontSize = 12.sp)
                    }
                    if (event.desc.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(event.desc, color = Muted, fontSize = 12.sp)
                    }
                }
                Text(
                    "×",
                    color = Red,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { onDelete(event.id) }.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EventForm(nextId: Int, onSave: (Event) -> Unit, onCancel: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }

    val titleError = if (title.isBlank()) "Назовите событие" else null
    val dateError = when {
        date.isBlank() -> "Укажите дату"
        !DateRegex.matches(date) -> "Формат: 2026-11-07"
        else -> null
    }
    val timeError = when {
        time.isBlank() -> null
        !TimeRegex.matches(time) -> "Формат: 19:30"
        else -> null
    }
    val canSave = titleError == null && dateError == null && timeError == null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        HudTextField(
            value = title, onValueChange = { title = it }, label = "Название",
            placeholder = "Встреча, день рождения…", error = titleError, maxLength = 60,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HudTextField(
                value = date, onValueChange = { date = it }, label = "Дата",
                placeholder = "ГГГГ-ММ-ДД", error = dateError, maxLength = 10,
                modifier = Modifier.weight(1.4f),
            )
            HudTextField(
                value = time, onValueChange = { time = it }, label = "Время",
                placeholder = "ЧЧ:ММ", hint = "Необязательно", error = timeError, maxLength = 5,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        HudTextField(
            value = place, onValueChange = { place = it }, label = "Место",
            placeholder = "Адрес или название", maxLength = 80, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        HudTextField(
            value = desc, onValueChange = { desc = it }, label = "Описание",
            placeholder = "Что взять, как добраться…", singleLine = false, minLines = 3,
            maxLength = 500, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(text = "Назад", onClick = onCancel, modifier = Modifier.weight(1f))
            HudButton(
                text = "Создать",
                onClick = {
                    if (canSave) {
                        onSave(Event(nextId, title.trim(), date.trim(), time.trim(), place.trim(), desc.trim()))
                    }
                },
                modifier = Modifier.weight(1f),
                primary = true,
            )
        }
    }
}
