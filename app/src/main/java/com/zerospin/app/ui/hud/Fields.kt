package com.zerospin.app.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain

/**
 * Поле ввода в стиле HUD: подпись сверху, подсказка внутри, счётчик символов и текст ошибки.
 * Используется везде, где человек что-то вводит.
 */
@Composable
fun HudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    hint: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
) {
    val isError = error != null
    Column(modifier) {
        Text(
            label.uppercase(),
            color = if (isError) Red else Cyan.copy(alpha = 0.85f),
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .hudPanel(line = if (isError) Red else Cyan, fill = Bg, cut = 8.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, color = Muted, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
                singleLine = singleLine,
                minLines = minLines,
                textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = SolidColor(Cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val footer = error ?: hint
        if (footer != null || maxLength != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    footer.orEmpty(),
                    color = if (isError) Red else Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                if (maxLength != null) {
                    Text("${value.length}/$maxLength", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Карточка-раздел для настроек и форм: заголовок и содержимое внутри HUD-панели. */
@Composable
fun HudSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(title.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .hudPanel(cut = 12.dp)
                .padding(vertical = 4.dp, horizontal = 14.dp),
        ) {
            content()
        }
    }
}

/** Шторка действий (меню сообщения, чата): список кнопок в HUD-панели. */
@Composable
fun HudActionSheet(
    title: String,
    actions: List<HudAction>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .hudPanel(cut = 14.dp, fill = Panel)
                .padding(vertical = 8.dp),
        ) {
            Text(
                title.uppercase(),
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            actions.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            onDismiss()
                            action.onClick()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        action.label,
                        color = if (action.danger) Red else Cyan,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

data class HudAction(val label: String, val danger: Boolean = false, val onClick: () -> Unit)
