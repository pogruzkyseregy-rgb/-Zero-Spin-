package com.zerospin.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.AURORA_LOGIN
import com.zerospin.app.data.ZsException
import com.zerospin.app.data.ZsNews
import com.zerospin.app.data.ZsProfile
import com.zerospin.app.data.ZsServer
import com.zerospin.app.data.ZsSession
import com.zerospin.app.ui.hud.GlitchText
import com.zerospin.app.ui.hud.HudBackground
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Magenta
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Значок «AV» у аккаунтов команды: админа и Авроры. Кладётся поверх аватара, в правый нижний угол. */
@Composable
fun BoxScope.AvBadge(size: Int = 26) {
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .size(size.dp)
            .clip(CircleShape)
            .background(Bg)
            .border(1.5.dp, Magenta, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("AV", color = Magenta, fontSize = (size * 0.4f).sp, fontWeight = FontWeight.Bold)
    }
}

private val LoginRegex = Regex("^[A-Za-z0-9_]{3,24}$")

/** Вход и регистрация: логин и пароль. Логин уникален без учёта регистра. */
@Composable
fun AuthScreen(serverUrl: String, onDone: (ZsSession) -> Unit, onSkip: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val loginError = when {
        login.isEmpty() -> null
        !LoginRegex.matches(login) -> "3–24 символа: латиница, цифры и _"
        register && login.lowercase().endsWith("_av") -> "логины с _AV на конце заняты командой"
        else -> null
    }
    val passError = when {
        !register || password.isEmpty() -> null
        password.length < 6 -> "пароль не короче 6 символов"
        password2.isNotEmpty() && password2 != password -> "пароли не совпадают"
        else -> null
    }
    val canGo = !busy && login.isNotEmpty() && password.isNotEmpty() && loginError == null && passError == null &&
        (!register || password2 == password)

    fun go() {
        busy = true
        error = null
        scope.launch {
            try {
                val s = if (register) ZsServer.register(serverUrl, login.trim(), password) else ZsServer.login(serverUrl, login.trim(), password)
                onDone(s)
            } catch (e: ZsException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    HudBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlitchText("ZERO SPIN", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Text(if (register) "РЕГИСТРАЦИЯ" else "ВХОД", color = Violet, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.zerospin.app.ui.hud.HudTextField(
                    value = login, onValueChange = { login = it.trim().removePrefix("@") }, label = "ЛОГИН",
                    hint = if (register) "Уникальный: такого же логина ни у кого не будет" else null, error = loginError, maxLength = 24,
                )
                PasswordField(password, { password = it }, "ПАРОЛЬ", passError.takeIf { password2.isEmpty() })
                if (register) PasswordField(password2, { password2 = it }, "ПАРОЛЬ ЕЩЁ РАЗ", passError.takeIf { password2.isNotEmpty() })
                if (error != null) Text(error ?: "", color = Red, fontSize = 12.sp)
                HudButton(if (busy) "…" else if (register) "Создать аккаунт" else "Войти", onClick = { if (canGo) go() },
                    modifier = Modifier.fillMaxWidth(), primary = canGo)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (register) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
                color = Cyan, fontSize = 13.sp,
                modifier = Modifier.clickable { register = !register; error = null },
            )
            Spacer(Modifier.height(10.dp))
            Text("Позже", color = Muted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onSkip))
            Spacer(Modifier.height(18.dp))
            Text(
                "Разговоры с Авророй в поддержке хранятся на сервере Zero Spin и видны администратору.",
                color = Muted, fontSize = 11.sp,
            )
        }
    }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, error: String?) {
    var show by remember { mutableStateOf(false) }
    Box {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, fontSize = 11.sp) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, fontSize = 11.sp) } },
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = com.zerospin.app.ui.hud.hudFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (show) "скрыть" else "показать",
            color = Muted, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 12.dp).clickable { show = !show },
        )
    }
}

/** Профиль Авроры с сервера: аватар, статус, описание и новости приложения, которые она пишет сама. */
@Composable
fun AuroraProfileScreen(serverUrl: String, onBack: () -> Unit) {
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) { AuroraProfileBody(serverUrl, onBack) }
    }
}

@Composable
private fun AuroraProfileBody(serverUrl: String, onBack: () -> Unit) {
    var profile by remember { mutableStateOf<ZsProfile?>(null) }
    var news by remember { mutableStateOf<List<ZsNews>>(emptyList()) }
    var avatar by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val images = remember { mutableStateOf<Map<Int, android.graphics.Bitmap>>(emptyMap()) }

    LaunchedEffect(serverUrl) {
        try {
            val p = ZsServer.profile(serverUrl, AURORA_LOGIN)
            profile = p
            news = ZsServer.news(serverUrl)
            if (p.hasAvatar) avatar = ZsServer.image(serverUrl, "/api/users/$AURORA_LOGIN/avatar?v=${p.avatarV}")
            news.filter { it.hasImage }.take(10).forEach { n ->
                ZsServer.image(serverUrl, "/api/news/${n.id}/image")?.let { images.value = images.value + (n.id to it) }
            }
        } catch (e: ZsException) {
            error = e.message
        }
    }

    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale("ru")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).hudPanel(cut = 9.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("ПРОФИЛЬ АВРОРЫ", color = Violet, fontSize = 12.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(110.dp)) {
                TargetRing(Modifier.size(110.dp), color = Violet, ears = true) {
                    val bmp = avatar
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), contentDescription = "Аврора", contentScale = ContentScale.Crop,
                            modifier = Modifier.size(90.dp).clip(CircleShape))
                    } else {
                        Text("A", color = Violet, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                }
                AvBadge(28)
            }
        }
        Spacer(Modifier.height(12.dp))
        val p = profile
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GlitchText((p?.name ?: "Аврора").uppercase(), color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("@${p?.login ?: AURORA_LOGIN}" + (p?.status?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), color = Muted, fontSize = 12.sp)
        }
        if (error != null) {
            Spacer(Modifier.height(14.dp))
            Text("Не загрузилось: $error", color = Red, fontSize = 12.sp)
        }
        if (!p?.bio.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(p!!.bio, color = TextMain, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().hudPanel(cut = 10.dp).padding(14.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("НОВОСТИ ZERO SPIN", color = Violet, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        if (news.isEmpty() && error == null) Text("Новостей пока нет", color = Muted, fontSize = 12.sp)
        news.forEach { n ->
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).hudPanel(line = Violet, cut = 10.dp).padding(14.dp)) {
                Text(fmt.format(Date(n.at)), color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(n.text, color = TextMain, fontSize = 14.sp)
                images.value[n.id]?.let { bmp ->
                    Spacer(Modifier.height(8.dp))
                    Image(bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height.coerceAtLeast(1)).clip(RoundedCornerShape(8.dp)))
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
