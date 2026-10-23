package com.zerospin.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.CardBg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.TextMain

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var notifications by rememberSaveable { mutableStateOf(true) }
    var sound by rememberSaveable { mutableStateOf(true) }
    var showOnline by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text("Настройки", color = Cyan, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                SettingRow("Уведомления", notifications) { notifications = it }
                SettingRow("Звук", sound) { sound = it }
                SettingRow("Показывать онлайн", showOnline) { showOnline = it }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово", color = Cyan)
            }
        },
    )
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMain, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Bg,
                checkedTrackColor = Cyan,
            ),
        )
    }
}
