package com.zerospin.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.AppSettings
import com.zerospin.app.ui.components.MutedLabel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.CardBg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                "НАСТРОЙКИ",
                color = Cyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 16.sp,
            )
        },
        text = {
            Column {
                MutedLabel("УВЕДОМЛЕНИЯ")
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "Уведомления",
                    hint = "Сообщения и новые записи круга",
                    checked = settings.notifications,
                ) { onChange(settings.copy(notifications = it)) }
                SettingRow(
                    title = "Звук",
                    hint = "Сигнал при новом сообщении",
                    checked = settings.sounds,
                ) { onChange(settings.copy(sounds = it)) }

                Spacer(Modifier.height(14.dp))
                MutedLabel("ПРИВАТНОСТЬ")
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "Показывать онлайн",
                    hint = "Круг видит, что вы в сети",
                    checked = settings.showOnline,
                ) { onChange(settings.copy(showOnline = it)) }
                SettingRow(
                    title = "Открыт для круга",
                    hint = "Другие могут писать вам первыми",
                    checked = settings.openToAll,
                ) { onChange(settings.copy(openToAll = it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ГОТОВО", color = Cyan, letterSpacing = 1.sp)
            }
        },
    )
}

@Composable
private fun SettingRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 14.sp)
            Text(hint, color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Bg,
                checkedTrackColor = Cyan,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Bg,
            ),
        )
    }
}
