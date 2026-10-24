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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Poll
import com.zerospin.app.data.nextId
import com.zerospin.app.ui.components.MutedLabel
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudEmptyState
import com.zerospin.app.ui.hud.SegmentedBar
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HudButton(
                text = "+ Новый опрос",
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun PollCard(poll: Poll, onVote: (Int) -> Unit) {
    val total = poll.votes.sum().coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
        Text(poll.question, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        poll.options.forEachIndexed { index, option ->
            val count = poll.votes.getOrElse(index) { 0 }
            val mine = poll.myVote == index
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onVote(index) }
                    .padding(vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (mine) "▲ $option" else "△ $option",
                        color = if (mine) Cyan else TextMain,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$count", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                SegmentedBar(progress = count.toFloat() / total)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("голосов: ${poll.votes.sum()} · нажмите вариант, чтобы проголосовать или снять голос", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PollForm(nextId: Int, onSave: (Poll) -> Unit, onCancel: () -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    var options by rememberSaveable { mutableStateOf("") }

    val list = options.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val canSave = question.isNotBlank() && list.size >= 2

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        MutedLabel("ВОПРОС")
        OutlinedTextField(value = question, onValueChange = { question = it }, minLines = 2,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        MutedLabel("ВАРИАНТЫ ЧЕРЕЗ «;» · МИНИМУМ ДВА")
        OutlinedTextField(value = options, onValueChange = { options = it }, minLines = 2,
            modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.height(16.dp))
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
                            ),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
