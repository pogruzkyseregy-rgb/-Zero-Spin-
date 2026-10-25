package com.zerospin.app.ui.screens

import android.graphics.Bitmap
import android.util.LruCache
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.AURORA_LOGIN
import com.zerospin.app.data.ZsException
import com.zerospin.app.data.ZsFriends
import com.zerospin.app.data.ZsPerson
import com.zerospin.app.data.ZsServer
import com.zerospin.app.ui.hud.BannedGlitchText
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.bannedGlitch
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.launch

/** Связь с сервером для экранов: адрес, ключ входа, мой логин. Пустой token — не вошли. */
data class ZsCtx(val base: String = "", val token: String = "", val me: String = "")

val LocalZs = compositionLocalOf { ZsCtx() }

/** Карточки людей, которых телефон уже видел (друзья, Аврора): для аватаров и имён в чатах. Ключ — логин в нижнем регистре. */
val LocalPeople = compositionLocalOf<SnapshotStateMap<String, ZsPerson>> { mutableStateMapOf() }

/** Аватары с сервера в памяти: чтобы список чатов не качал их заново при каждой прокрутке. */
private object AvatarCache {
    val map = LruCache<String, Bitmap>(64)
}

/** Круглый аватар человека с сервера. Пока не загрузился или его нет — первая буква. */
@Composable
fun RemoteAvatar(
    login: String,
    version: Long,
    hasAvatar: Boolean,
    size: Int,
    letter: String,
    modifier: Modifier = Modifier,
    color: Color = Cyan,
    team: Boolean = false,
    cosmetic: String = "",
) {
    val base = LocalZs.current.base
    val isAurora = login.equals(AURORA_LOGIN, ignoreCase = true)
    val cacheKey = "${login.lowercase()}#$version"
    var bmp by remember(cacheKey) { mutableStateOf(AvatarCache.map.get(cacheKey)) }
    LaunchedEffect(cacheKey, hasAvatar, base) {
        if (bmp == null && hasAvatar && base.isNotBlank()) {
            ZsServer.image(base, "/api/users/$login/avatar?v=$version")?.let {
                AvatarCache.map.put(cacheKey, it)
                bmp = it
            }
        }
    }
    Box(modifier.size(size.dp)) {
        if (!isAurora) com.zerospin.app.ui.hud.CosmeticBack(cosmetic, size)
        TargetRing(Modifier.size(size.dp), color = color, ears = isAurora) {
            val b = bmp
            if (b != null) {
                Image(b.asImageBitmap(), contentDescription = login, contentScale = ContentScale.Crop,
                    modifier = Modifier.size((size * 0.84f).dp).clip(CircleShape))
            } else {
                Text(letter.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = (size / 2.6f).sp)
            }
        }
        // У Авроры свои ушки, остальным — выбранное украшение
        if (!isAurora) com.zerospin.app.ui.hud.CosmeticOverlay(cosmetic, size)
        if (team) AvBadge((size * 0.42f).toInt().coerceAtLeast(14))
    }
}

@Composable
private fun PersonAvatar(p: ZsPerson, size: Int) {
    Box(if (p.banned) Modifier.bannedGlitch() else Modifier) {
        RemoteAvatar(p.login, p.avatarV, p.hasAvatar, size, p.name.ifBlank { p.login },
            color = if (p.role == "aurora") Violet else Cyan, team = p.team, cosmetic = p.cosmetic)
    }
}

/** Мелкая кнопка-надпись в строке человека. */
@Composable
private fun SmallAction(text: String, color: Color = Cyan, filled: Boolean = false, onClick: () -> Unit) {
    Text(
        text.uppercase(),
        color = if (filled) Bg else color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .hudPanel(line = color, fill = if (filled) color else Panel, cut = 6.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun PersonRow(p: ZsPerson, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().hudPanel(cut = 10.dp).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(p, 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (p.banned) BannedGlitchText(p.name.ifBlank { p.login }, Red, 14.sp)
            else Text(p.name.ifBlank { p.login }, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${p.login}" + (p.status.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), color = Muted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color = Violet) {
    Text(text, color = color, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

/**
 * «Люди»: поиск по логину, заявки в друзья, друзья и мои заявки.
 * onChanged — что-то поменялось в дружбе: приложение обновит свои списки.
 */
@Composable
fun PeopleScreen(
    onDismiss: () -> Unit,
    onWrite: (ZsPerson) -> Unit,
    onOpenProfile: (String) -> Unit,
    onChanged: (ZsFriends) -> Unit,
) {
    val zs = LocalZs.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<ZsPerson>>(emptyList()) }
    var data by remember { mutableStateOf<ZsFriends?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun reload() {
        try {
            val f = ZsServer.friends(zs.base, zs.token)
            data = f
            onChanged(f)
        } catch (e: ZsException) {
            error = e.message
        }
    }

    fun act(action: String, login: String) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val p = ZsServer.friend(zs.base, zs.token, action, login)
                found = found.map { if (it.login.equals(p.login, true)) p else it }
                error = null
                reload()
            } catch (e: ZsException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(zs.token) { if (zs.token.isNotBlank()) reload() }
    LaunchedEffect(query) {
        val q = query.trim().removePrefix("@")
        if (q.length < 2 || zs.token.isBlank()) {
            found = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(350) // ждём, пока человек допечатает
        try {
            found = ZsServer.search(zs.base, zs.token, q)
            error = null
        } catch (e: ZsException) {
            error = e.message
        }
    }

    @Composable
    fun ActionsFor(p: ZsPerson) {
        when (p.relation) {
            "friends" -> SmallAction("Написать", Mint, filled = true) { onWrite(p) }
            "incoming" -> {
                SmallAction("Принять", Mint, filled = true) { act("accept", p.login) }
                SmallAction("✕", Muted) { act("remove", p.login) }
            }
            "outgoing" -> SmallAction("Отменить", Muted) { act("remove", p.login) }
            else -> SmallAction("+ В друзья", Cyan, filled = true) { act("request", p.login) }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            LazyColumn(
                Modifier.fillMaxSize().systemBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).hudPanel(cut = 9.dp).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                            HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("ЛЮДИ", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 3.sp)
                    }
                }
                if (zs.token.isBlank()) {
                    item { Text("Чтобы искать людей и дружить, войди в аккаунт: Настройки → Сервер.", color = Muted, fontSize = 13.sp) }
                    return@LazyColumn
                }
                item {
                    com.zerospin.app.ui.hud.HudTextField(
                        value = query, onValueChange = { query = it.trim() }, label = "Найти по логину",
                        placeholder = "например, anna_k", maxLength = 24, modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { e -> item { Text(e, color = Red, fontSize = 12.sp) } }
                if (query.trim().length >= 2) {
                    item { SectionTitle("НАЙДЕНО") }
                    if (found.isEmpty()) item { Text("Никого с таким логином", color = Muted, fontSize = 12.sp) }
                    items(found, key = { "f_" + it.login }) { p -> PersonRow(p, { onOpenProfile(p.login) }) { ActionsFor(p) } }
                }
                val d = data
                if (d != null) {
                    if (d.incoming.isNotEmpty()) {
                        item { SectionTitle("ЗАЯВКИ В ДРУЗЬЯ · ${d.incoming.size}", Mint) }
                        items(d.incoming, key = { "i_" + it.login }) { p ->
                            PersonRow(p, { onOpenProfile(p.login) }) { ActionsFor(p.copy(relation = "incoming")) }
                        }
                    }
                    item { SectionTitle("ДРУЗЬЯ · ${d.friends.size}") }
                    if (d.friends.isEmpty()) item {
                        Text("Пока никого. Найди человека по логину и отправь заявку.", color = Muted, fontSize = 12.sp)
                    }
                    items(d.friends, key = { "d_" + it.login }) { p ->
                        PersonRow(p, { onOpenProfile(p.login) }) { ActionsFor(p.copy(relation = "friends")) }
                    }
                    if (d.outgoing.isNotEmpty()) {
                        item { SectionTitle("ЖДУТ ОТВЕТА · ${d.outgoing.size}", Muted) }
                        items(d.outgoing, key = { "o_" + it.login }) { p ->
                            PersonRow(p, { onOpenProfile(p.login) }) { ActionsFor(p.copy(relation = "outgoing")) }
                        }
                    }
                }
            }
        }
    }
}

/** Профиль другого человека: аватар, имя, статус, описание и что можно сделать (дружба, личка). */
@Composable
fun UserProfileScreen(login: String, onBack: () -> Unit, onWrite: (ZsPerson) -> Unit, onChanged: () -> Unit) {
    val zs = LocalZs.current
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<ZsPerson?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(login, zs.token) {
        try {
            p = if (zs.token.isNotBlank()) ZsServer.person(zs.base, zs.token, login)
            else ZsServer.profile(zs.base, login).let { ZsPerson(it.login, it.name, it.status, it.bio, it.role, it.banned, it.hasAvatar, it.avatarV) }
        } catch (e: ZsException) {
            error = e.message
        }
    }

    fun act(action: String) {
        val cur = p ?: return
        scope.launch {
            try {
                p = ZsServer.friend(zs.base, zs.token, action, cur.login)
                error = null
                onChanged()
            } catch (e: ZsException) {
                error = e.message
            }
        }
    }

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).hudPanel(cut = 9.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("ПРОФИЛЬ", color = Violet, fontSize = 12.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(22.dp))
                val cur = p
                if (cur == null) {
                    Text(error ?: "Загружаю…", color = if (error != null) Red else Muted, fontSize = 13.sp)
                    return@Column
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PersonAvatar(cur, 112) }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (cur.banned) BannedGlitchText(cur.name.ifBlank { cur.login }.uppercase(), Red, 20.sp)
                    else Text(cur.name.ifBlank { cur.login }, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("@${cur.login}", color = Muted, fontSize = 13.sp)
                    if (cur.team) Text(if (cur.role == "aurora") "ИИ ZERO SPIN" else "АДМИН ZERO SPIN", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp)
                    if (cur.banned) Text("⛔ ЗАБЛОКИРОВАН АВРОРОЙ", color = Red, fontSize = 11.sp, letterSpacing = 1.sp)
                    if (cur.status.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(cur.status, color = Cyan, fontSize = 13.sp)
                    }
                }
                if (cur.bio.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(cur.bio, color = TextMain, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().hudPanel(cut = 10.dp).padding(14.dp))
                }
                Spacer(Modifier.height(20.dp))
                error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                if (zs.token.isNotBlank() && cur.role != "aurora") {
                    when (cur.relation) {
                        "friends" -> {
                            HudButton("Написать", onClick = { onWrite(cur) }, modifier = Modifier.fillMaxWidth(), primary = true)
                            Spacer(Modifier.height(8.dp))
                            if (confirmRemove) {
                                HudButton("Точно убрать из друзей?", onClick = { confirmRemove = false; act("remove") }, modifier = Modifier.fillMaxWidth())
                            } else {
                                Text("Убрать из друзей", color = Muted, fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable { confirmRemove = true }.padding(8.dp))
                            }
                        }
                        "incoming" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HudButton("Принять", onClick = { act("accept") }, modifier = Modifier.weight(1f), primary = true)
                            HudButton("Отклонить", onClick = { act("remove") }, modifier = Modifier.weight(1f))
                        }
                        "outgoing" -> HudButton("Заявка отправлена · отменить", onClick = { act("remove") }, modifier = Modifier.fillMaxWidth())
                        "none" -> HudButton("Добавить в друзья", onClick = { act("request") }, modifier = Modifier.fillMaxWidth(), primary = true)
                    }
                }
            }
        }
    }
}
