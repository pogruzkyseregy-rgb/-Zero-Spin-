package com.zerospin.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Event
import com.zerospin.app.data.nextId
import com.zerospin.app.ui.components.MutedLabel
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

private val DateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val TimeRegex = Regex("""^(\d{2}:\d{2})?$""")

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
            )
        }
        if (sorted.isEmpty()) {
            item { HudEmptyState("СОБЫТИЙ НЕТ", "Отметьте встречу круга") }
        }
        items(sorted, key = { it.id }) { event ->
            Row(
                Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Top,
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

    val dateOk = DateRegex.matches(date.trim())
    val timeOk = TimeRegex.matches(time.trim())
    val canSave = title.isNotBlank() && dateOk && timeOk

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        MutedLabel("НАЗВАНИЕ")
        OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        MutedLabel("ДАТА · ГГГГ-ММ-ДД")
        OutlinedTextField(value = date, onValueChange = { date = it }, singleLine = true,
            isError = date.isNotBlank() && !dateOk,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        MutedLabel("ВРЕМЯ · ЧЧ:ММ, НЕОБЯЗАТЕЛЬНО")
        OutlinedTextField(value = time, onValueChange = { time = it }, singleLine = true,
            isError = time.isNotBlank() && !timeOk,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        MutedLabel("МЕСТО")
        OutlinedTextField(value = place, onValueChange = { place = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        MutedLabel("ОПИСАНИЕ")
        OutlinedTextField(value = desc, onValueChange = { desc = it }, minLines = 3,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(16.dp))
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
            )
        }
    }
}
