package com.zerospin.app.ui.screens

import com.zerospin.app.ui.hud.BannedGlitchText
import com.zerospin.app.ui.hud.bannedGlitch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.BlockKind
import com.zerospin.app.data.Profile
import com.zerospin.app.data.ProfileBlock
import com.zerospin.app.ui.components.PhotoViewer
import com.zerospin.app.ui.components.imageAttachment
import com.zerospin.app.ui.components.rememberCoverPicker
import com.zerospin.app.ui.components.rememberThumb
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private val BirthdayRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val NickRegex = Regex("""^[\p{L}\p{N}_.]{3,20}$""")
private val StatusPresets = listOf("на связи", "на смене", "в дороге", "занят", "не беспокоить")

@Composable
fun ProfileScreen(
    profile: Profile,
    banned: Boolean = false,
    admin: Boolean = false,
    postCount: Int,
    chatCount: Int,
    onSave: (Profile) -> Unit,
    onAvatar: (String?) -> Unit,
    onOpenBuilder: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var nick by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    // Фото, открытое на весь экран (любое фото профиля)
    var viewing by remember { mutableStateOf<Attachment?>(null) }

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

    // Просмотр фото поверх экрана
    viewing?.let { PhotoViewer(item = it, onDismiss = { viewing = null }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!editing) {
            // Правка и конструктор спрятаны в иконки, чтобы не мешали
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Изменить профиль",
                    tint = Muted,
                    modifier = Modifier.size(22.dp).clickable { startEdit() },
                )
                Spacer(Modifier.width(18.dp))
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Конструктор",
                    tint = Muted,
                    modifier = Modifier.size(22.dp).clickable(onClick = onOpenBuilder),
                )
            }

            ProfileHeader(
                profile = profile,
                banned = banned,
                admin = admin,
                onAvatarTap = {
                    val uri = profile.avatarUri
                    if (uri != null) viewing = imageAttachment(uri, "аватар") else pickAvatar()
                },
                onCoverTap = { uri -> viewing = imageAttachment(uri, "обложка") },
            )

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    profile.name.uppercase(),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 3.sp,
                )
                if (banned) {
                    BannedGlitchText("@${profile.nick}", color = Red, fontSize = 12.sp)
                    Text("⛔ ЗАБЛОКИРОВАН АВРОРОЙ", color = Red, fontSize = 10.sp, letterSpacing = 2.sp)
                } else {
                    Text(
                        listOfNotNull("@${profile.nick}", profile.status.ifBlank { null }).joinToString(" · "),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))

                // Блоки в порядке, который задан в конструкторе; обложка уже в шапке
                profile.blocks.filter { it.kind != BlockKind.COVER }.forEach { block ->
                    BlockView(block, profile, postCount, chatCount, onOpen = { viewing = it })
                    Spacer(Modifier.height(10.dp))
                }
                if (profile.blocks.isEmpty()) {
                    Text("Профиль пуст. Добавьте блоки в конструкторе", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (profile.avatarUri == null) "ДОБАВИТЬ ФОТО" else "СМЕНИТЬ ФОТО",
                            color = Cyan,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.clickable { pickAvatar() }.padding(vertical = 8.dp),
                        )
                        if (profile.avatarUri != null) {
                            Text(
                                "УБРАТЬ ФОТО",
                                color = Red,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.clickable { onAvatar(null) }.padding(vertical = 8.dp),
                            )
                        }
                    }
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
}

/**
 * Шапка: обложка на всю ширину как фон, аватар поверх её нижнего края.
 * Нажатие на обложку и аватар открывает фото на весь экран.
 */
@Composable
private fun ProfileHeader(
    profile: Profile,
    banned: Boolean,
    admin: Boolean,
    onAvatarTap: () -> Unit,
    onCoverTap: (String) -> Unit,
) {
    val coverUri = profile.coverUri.takeIf { uri -> uri != null && profile.blocks.any { it.kind == BlockKind.COVER } }
    val cover = coverUri?.let { rememberThumb(it, 1080) }
    if (cover != null && coverUri != null) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable { onCoverTap(coverUri) },
            )
            // Мягкий переход к фону приложения, чтобы обложка не резала экран
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Bg))),
            )
            Avatar(
                profile = profile,
                onPick = onAvatarTap,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 52.dp),
                banned = banned,
                admin = admin,
            )
        }
        Spacer(Modifier.height(60.dp))
    } else {
        Spacer(Modifier.height(16.dp))
        Avatar(profile = profile, onPick = onAvatarTap, banned = banned, admin = admin)
        Spacer(Modifier.height(12.dp))
    }
}

/** Один блок профиля. Пустые блоки с текстом не показываются, чтобы не было пустых рамок. */
@Composable
private fun BlockView(
    block: ProfileBlock,
    profile: Profile,
    postCount: Int,
    chatCount: Int,
    onOpen: (Attachment) -> Unit,
) {
    when (block.kind) {
        BlockKind.ABOUT -> TextPanel(
            profile.bio.ifBlank { "Пока без описания" },
        )
        BlockKind.STATS -> StatsRow(postCount, chatCount, profile.friends.size)
        BlockKind.BIRTHDAY -> InfoRow("ДАТА РОЖДЕНИЯ", profile.birthday.ifBlank { "не указана" })
        BlockKind.QUOTE -> if (block.text.isNotBlank()) QuotePanel(block.text)
        BlockKind.LOCATION -> if (block.text.isNotBlank()) InfoRow("ГОРОД", block.text)
        BlockKind.LINK -> if (block.text.isNotBlank()) InfoRow("ССЫЛКА", block.text)
        // Обложка показана в шапке, здесь ничего не выводим
        BlockKind.COVER -> Unit
        BlockKind.PHOTOS -> if (profile.photos.isNotEmpty()) PhotoCarousel(profile.photos, onOpen)
        BlockKind.FRIENDS -> if (profile.friends.isNotEmpty()) FriendsPanel(profile.friends)
    }
}

/**
 * Карусель фото по кругу. Листается сама каждые несколько секунд,
 * пауза, пока палец на экране или идёт прокрутка. Нажатие открывает фото на весь экран.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCarousel(photos: List<Attachment>, onOpen: (Attachment) -> Unit) {
    val count = photos.size
    // Начинаем из середины огромного диапазона, чтобы можно было листать в обе стороны
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % count
    val pager = rememberPagerState(initialPage = start) { Int.MAX_VALUE }

    LaunchedEffect(pager, count) {
        if (count > 1) {
            while (true) {
                delay(4000)
                if (!pager.isScrollInProgress) {
                    pager.animateScrollToPage(pager.currentPage + 1)
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val photo = photos[page % count]
            val thumb = rememberThumb(photo.uri, 900)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Насколько страница ушла от центра: 0 — в центре, 1 — на полстраницы и дальше
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        val off = (page - position).absoluteValue.coerceAtMost(1f)
                        scaleX = 1f - 0.12f * off
                        scaleY = 1f - 0.12f * off
                        alpha = 1f - 0.5f * off
                    }
                    .hudPanel(cut = 14.dp)
                    .padding(3.dp)
                    .clickable { onOpen(photo) },
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (count > 1) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${pager.currentPage % count + 1} / $count",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

/** Статистика одной строкой: три числа без лишних рамок. */
@Composable
private fun StatsRow(posts: Int, channels: Int, friends: Int) {
    Row(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(vertical = 14.dp, horizontal = 10.dp)) {
        StatCell("ЗАПИСЕЙ", posts, Modifier.weight(1f))
        StatCell("КАНАЛОВ", channels, Modifier.weight(1f))
        StatCell("ДРУЗЕЙ", friends, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun TextPanel(text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(text, color = TextMain.copy(alpha = 0.85f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuotePanel(text: String) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, Violet.copy(alpha = 0.6f), RectangleShape).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            color = Cyan,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Друзья: счётчик и список с инициалом в кольце. */
@Composable
private fun FriendsPanel(friends: List<String>) {
    Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
        Text("ДРУЗЬЯ · ${friends.size}", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        friends.forEach { friend ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TargetRing(Modifier.size(30.dp)) {
                    Text(friend.take(1).uppercase(), color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text("@$friend", color = TextMain, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = TextMain, fontSize = 13.sp)
    }
}

/** Круг с фото или инициалом. Нажатие передаётся наружу: открыть фото или выбрать новое. */
@Composable
private fun Avatar(profile: Profile, onPick: () -> Unit, modifier: Modifier = Modifier, banned: Boolean = false, admin: Boolean = false) {
    val photo = profile.avatarUri?.let { rememberThumb(it, 512) }
    // Значок AV в углу аватара: аккаунт команды (админ)
    Box(modifier.size(104.dp)) {
    TargetRing(Modifier.size(104.dp).then(if (banned) Modifier.bannedGlitch() else Modifier).clickable(onClick = onPick)) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp).clip(CircleShape),
            )
        } else {
            Text(
                profile.name.take(1).uppercase(),
                color = Cyan,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (admin) AvBadge(30)
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
