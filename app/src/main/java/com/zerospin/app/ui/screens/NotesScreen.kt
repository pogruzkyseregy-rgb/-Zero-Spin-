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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Note
import com.zerospin.app.data.nextId
import com.zerospin.app.data.nowMillis
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    onSave: (Note) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf<Note?>(null) }

    val current = editing
    if (current != null) {
        NoteEditor(
            note = current,
            isExisting = notes.any { it.id == current.id },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(current.id); editing = null },
            onCancel = { editing = null },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HudButton(
                text = "+ Новая заметка",
                onClick = {
                    editing = Note(id = nextId(notes.map { it.id }), title = "", body = "", updated = nowMillis())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (notes.isEmpty()) {
            item { HudEmptyState("ЗАМЕТОК НЕТ", "Запишите то, что не должно потеряться") }
        }
        items(notes, key = { it.id }) { note ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .hudPanel(cut = 12.dp)
                    .clickable { editing = note }
                    .padding(14.dp),
            ) {
                Text(
                    note.title.ifBlank { "Без названия" }.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(note.body, color = TextMain, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Text(formatMillis(note.updated), color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun NoteEditor(
    note: Note,
    isExisting: Boolean,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Название", color = Muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = hudFieldColors(),
            shape = RoundedCornerShape(6.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            placeholder = { Text("Текст заметки…", color = Muted) },
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
            colors = hudFieldColors(),
            shape = RoundedCornerShape(6.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(text = "Назад", onClick = onCancel, modifier = Modifier.weight(1f))
            HudButton(
                text = "Сохранить",
                onClick = {
                    if (title.isNotBlank() || body.isNotBlank()) {
                        onSave(note.copy(title = title.trim(), body = body.trim(), updated = nowMillis()))
                    } else {
                        onCancel()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (isExisting) {
            Spacer(Modifier.height(10.dp))
            Text(
                "УДАЛИТЬ ЗАМЕТКУ",
                color = Red,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable(onClick = onDelete).padding(8.dp),
            )
        }
    }
}

internal fun formatMillis(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))
