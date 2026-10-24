package com.zerospin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Profile
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.SegmentedBar
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudFieldColors
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.components.MutedLabel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain

private val BirthdayRegex = Regex("""^(\d{4}-\d{2}-\d{2})?$""")

@Composable
fun ProfileScreen(
    profile: Profile,
    postCount: Int,
    chatCount: Int,
    onSave: (Profile) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var nick by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    fun startEdit() {
        name = profile.name
        nick = profile.nick
        status = profile.status
        birthday = profile.birthday
        bio = profile.bio
        editing = true
    }

    val birthdayValid = BirthdayRegex.matches(birthday.trim())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TargetRing(Modifier.size(120.dp)) {
            Text(
                profile.name.take(1).uppercase(),
                color = Cyan,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (!editing) {
            Text(
                profile.name.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 3.sp,
            )
            Text("@${profile.nick}", color = Muted, fontSize = 12.sp)
            if (profile.status.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(profile.status, color = Cyan.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.bio.ifBlank { "Пока без описания" },
                color = TextMain.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            StatBlock(label = "ЗАПИСЕЙ", value = postCount, goal = 10)
            Spacer(Modifier.height(12.dp))
            StatBlock(label = "КАНАЛОВ", value = chatCount, goal = 5)
            Spacer(Modifier.height(12.dp))
            InfoRow("ДАТА РОЖДЕНИЯ", profile.birthday.ifBlank { "не указана" })
            Spacer(Modifier.height(24.dp))

            HudButton(
                text = "Редактировать",
                onClick = { startEdit() },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
                FieldLabel("ИМЯ")
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel("НИК")
                OutlinedTextField(
                    value = nick, onValueChange = { nick = it.removePrefix("@") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel("СТАТУС")
                OutlinedTextField(
                    value = status, onValueChange = { status = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel("ДАТА РОЖДЕНИЯ · ГГГГ-ММ-ДД")
                OutlinedTextField(
                    value = birthday, onValueChange = { birthday = it }, singleLine = true,
                    isError = !birthdayValid,
                    modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(),
                )
                if (!birthdayValid) {
                    Text("Формат: 1995-03-14", color = Red, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                FieldLabel("О СЕБЕ")
                OutlinedTextField(
                    value = bio, onValueChange = { bio = it }, maxLines = 4,
                    modifier = Modifier.fillMaxWidth(), colors = hudFieldColors(),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudButton(
                        text = "Отмена",
                        onClick = { editing = false },
                        modifier = Modifier.weight(1f),
                    )
                    HudButton(
                        text = "Сохранить",
                        onClick = {
                            onSave(
                                Profile(
                                    name = name.trim().ifEmpty { profile.name },
                                    nick = nick.trim().ifEmpty { profile.nick },
                                    status = status.trim(),
                                    bio = bio.trim(),
                                    birthday = birthday.trim(),
                                ),
                            )
                            editing = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    MutedLabel(text)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = TextMain, fontSize = 13.sp)
    }
}

@Composable
private fun StatBlock(label: String, value: Int, goal: Int) {
    Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Text(value.toString(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        SegmentedBar(progress = value.toFloat() / goal)
    }
}
