package com.zerospin.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Profile
import com.zerospin.app.ui.components.rememberCoverPicker
import com.zerospin.app.ui.components.rememberThumb
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.SegmentedBar
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain

private val BirthdayRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val NickRegex = Regex("""^[\p{L}\p{N}_.]{3,20}$""")
private val StatusPresets = listOf("на связи", "на смене", "в дороге", "занят", "не беспокоить")

@Composable
fun ProfileScreen(
    profile: Profile,
    postCount: Int,
    chatCount: Int,
    onSave: (Profile) -> Unit,
    onAvatar: (String?) -> Unit,
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

    // Назад во время правки закрывает форму, а не выходит из приложения
    BackHandler(enabled = editing) { editing = false }

    val pickAvatar = rememberCoverPicker { onAvatar(it.uri) }

    val nickError = when {
        nick.isBlank() -> "Введите ник"
        !NickRegex.matches(nick) -> "От 3 до 20 символов: буквы, цифры, _ и ."
        else -> null
    }
    val birthdayError = when {
        birthday.isBlank() -> null
        !BirthdayRegex.matches(birthday) -> "Нужна дата в формате ГГГГ-ММ-ДД"
        else -> null
    }
    val nameError = if (name.isBlank()) "Имя не может быть пустым" else null
    val dirty = name.trim() != profile.name ||
        nick.trim() != profile.nick ||
        status.trim() != profile.status ||
        bio.trim() != profile.bio ||
        birthday.trim() != profile.birthday
    val canSave = nameError == null && nickError == null && birthdayError == null && dirty

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(profile = profile, onPick = { pickAvatar() })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (profile.avatarUri == null) "нажмите, чтобы добавить фото" else "нажмите, чтобы сменить фото",
                color = Muted,
                fontSize = 10.sp,
            )
            if (profile.avatarUri != null) {
                Spacer(Modifier.size(10.dp))
                Text("убрать", color = Red, fontSize = 10.sp, modifier = Modifier.clickable { onAvatar(null) })
            }
        }
        Spacer(Modifier.height(18.dp))

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
                text = "Редактировать профиль",
                onClick = { startEdit() },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
            )
        } else {
            Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
                HudTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Имя",
                    placeholder = "Как вас называть",
                    error = nameError,
                    maxLength = 30,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                HudTextField(
                    value = nick,
                    onValueChange = { nick = it.removePrefix("@") },
                    label = "Ник",
                    placeholder = "adonis",
                    hint = "Так вас будет находить круг. Буквы, цифры, _ и .",
                    error = nickError,
                    maxLength = 20,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                HudTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = "Статус",
                    placeholder = "Например: на смене",
                    maxLength = 40,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                QuickStatuses(onPick = { status = it })
                Spacer(Modifier.height(14.dp))
                HudTextField(
                    value = birthday,
                    onValueChange = { birthday = formatBirthday(it) },
                    label = "Дата рождения",
                    placeholder = "1995-03-14",
                    hint = "Необязательно. Цифры, дефисы появятся сами",
                    error = birthdayError,
                    maxLength = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                HudTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "О себе",
                    placeholder = "Пару слов о себе",
                    singleLine = false,
                    minLines = 3,
                    maxLength = 160,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (dirty) "Есть несохранённые изменения" else "Изменений пока нет",
                    color = if (dirty) Cyan else Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudButton(text = "Отмена", onClick = { editing = false }, modifier = Modifier.weight(1f))
                    HudButton(
                        text = "Сохранить",
                        onClick = {
                            if (canSave) {
                                onSave(
                                    profile.copy(
                                        name = name.trim(),
                                        nick = nick.trim(),
                                        status = status.trim(),
                                        bio = bio.trim(),
                                        birthday = birthday.trim(),
                                    ),
                                )
                                editing = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
            }
        }
    }
}

/** Круг с фото или инициалом. Нажатие выбирает новое фото. */
@Composable
private fun Avatar(profile: Profile, onPick: () -> Unit) {
    val photo = profile.avatarUri?.let { rememberThumb(it, 512) }
    TargetRing(Modifier.size(120.dp).clickable(onClick = onPick)) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
        } else {
            Text(
                profile.name.take(1).uppercase(),
                color = Cyan,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Быстрые статусы в одну строку: нажатие подставляет текст в поле статуса. */
@Composable
private fun QuickStatuses(onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusPresets.forEach { preset ->
            Text(
                preset,
                color = Cyan,
                fontSize = 11.sp,
                modifier = Modifier
                    .hudPanel(line = Muted, cut = 6.dp)
                    .clickable { onPick(preset) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Оставляет только цифры и сам ставит дефисы: 19950314 → 1995-03-14. */
private fun formatBirthday(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 4 || i == 6) append('-')
            append(c)
        }
    }
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
