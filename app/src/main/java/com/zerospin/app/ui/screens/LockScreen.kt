package com.zerospin.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.hashPin
import com.zerospin.app.ui.hud.HudBackground
import com.zerospin.app.ui.hud.HudMono
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

private const val PIN_LENGTH = 4

/**
 * Цифровая клавиатура для PIN: точки сверху, кнопки 3×4.
 * Когда введены все цифры, вызывается onComplete, ввод очищается.
 */
@Composable
fun PinPad(
    title: String,
    subtitle: String,
    error: String?,
    onComplete: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    var errorTick by remember { mutableIntStateOf(0) }

    // Ошибка — короткая дрожь точек
    LaunchedEffect(error, errorTick) {
        if (error != null) {
            for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    fun press(digit: String) {
        if (pin.length >= PIN_LENGTH) return
        pin += digit
        if (pin.length == PIN_LENGTH) {
            val entered = pin
            pin = ""
            errorTick += 1
            onComplete(entered)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TargetRing(Modifier.size(96.dp), color = Violet, ears = true) {
            Text("AV", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, fontFamily = HudMono)
        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.offset(x = shake.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(PIN_LENGTH) { i ->
                val filled = i < pin.length
                val color = if (error != null && pin.isEmpty()) Red else if (filled) Mint else Muted
                Box(
                    Modifier
                        .size(16.dp)
                        .border(1.5.dp, color, CircleShape)
                        .padding(3.dp)
                        .background(if (filled) color else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(error ?: " ", color = Red, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Box(Modifier.size(72.dp, 60.dp))
                        "⌫" -> PadKey(key, dim = true) { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                        else -> PadKey(key) { press(key) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (onCancel != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "ОТМЕНА",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable(onClick = onCancel).padding(10.dp),
            )
        }
    }
}

@Composable
private fun PadKey(label: String, dim: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp, 60.dp)
            .hudPanel(line = if (dim) Muted else Cyan, cut = 10.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (dim) Muted else TextMain, fontSize = 22.sp, fontFamily = HudMono)
    }
}

/** Экран блокировки при входе. */
@Composable
fun UnlockScreen(pinHash: String, animated: Boolean, onUnlocked: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    HudBackground(animated = animated) {
        PinPad(
            title = "ZERO SPIN",
            subtitle = "введите код доступа",
            error = error,
            onComplete = { pin ->
                if (hashPin(pin) == pinHash) {
                    error = null
                    onUnlocked()
                } else {
                    attempts += 1
                    error = "Неверный код · попытка $attempts"
                }
            },
        )
    }
}

/** Установка нового PIN: ввести и повторить. onSet получает хэш. */
@Composable
fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HudBackground(animated = false) {
            PinPad(
                title = if (first == null) "НОВЫЙ КОД" else "ПОВТОРИТЕ КОД",
                subtitle = if (first == null) "4 цифры для входа" else "для подтверждения",
                error = error,
                onComplete = { pin ->
                    val saved = first
                    if (saved == null) {
                        first = pin
                        error = null
                    } else if (saved == pin) {
                        onSet(hashPin(pin))
                        onDismiss()
                    } else {
                        first = null
                        error = "Коды не совпали, начните заново"
                    }
                },
                onCancel = onDismiss,
            )
        }
    }
}
