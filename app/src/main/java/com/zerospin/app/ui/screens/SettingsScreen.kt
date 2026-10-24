package com.zerospin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.AppSettings
import com.zerospin.app.data.Profile
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudSection
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

/** Полноэкранные настройки: профиль сверху, разделы ниже, у каждого переключателя короткое пояснение. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    profile: Profile,
    version: String,
    onChange: (AppSettings) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBuilder: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("НАСТРОЙКИ", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
                    Text(
                        "ГОТОВО",
                        color = Cyan,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                    )
                }

                // Карточка профиля: переход в редактирование
                Row(
                    Modifier
                        .fillMaxWidth()
                        .hudPanel(cut = 14.dp)
                        .clickable { onOpenProfile() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TargetRing(Modifier.size(56.dp)) {
                        Text(profile.name.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("@${profile.nick} · ${profile.status.ifBlank { "без статуса" }}", color = Muted, fontSize = 12.sp)
                    }
                    Text("ИЗМЕНИТЬ", color = Violet, fontSize = 10.sp, letterSpacing = 1.sp)
                }

                HudSection("Профиль") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBuilder() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Конструктор профиля", color = TextMain, fontSize = 14.sp)
                            Text("Шаблоны, блоки и их порядок", color = Muted, fontSize = 11.sp)
                        }
                        Text("ОТКРЫТЬ", color = Violet, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                }

                HudSection("Уведомления") {
                    SwitchRow("Уведомления", "Сообщения и новые записи круга", settings.notifications) {
                        onChange(settings.copy(notifications = it))
                    }
                    SwitchRow("Звук", "Короткий сигнал при новом сообщении", settings.sounds) {
                        onChange(settings.copy(sounds = it))
                    }
                    SwitchRow("Вибрация", "Отклик при подбросе монетки", settings.vibration, last = true) {
                        onChange(settings.copy(vibration = it))
                    }
                }

                HudSection("Приватность") {
                    SwitchRow("Показывать онлайн", "Круг видит, что вы в сети", settings.showOnline) {
                        onChange(settings.copy(showOnline = it))
                    }
                    SwitchRow("Отметки прочтения", "Галочки ✓✓ у ваших сообщений", settings.readReceipts) {
                        onChange(settings.copy(readReceipts = it))
                    }
                    SwitchRow("Открыт для круга", "Другие могут писать вам первыми", settings.openToAll, last = true) {
                        onChange(settings.copy(openToAll = it))
                    }
                }

                HudSection("Оформление") {
                    SwitchRow("Глитч-эффекты", "Редкие сбои в заголовках и на экранах", settings.glitch, last = true) {
                        onChange(settings.copy(glitch = it))
                    }
                }

                HudSection("О приложении") {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HudIcon(HudIconKind.RING, Cyan, Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Zero Spin $version", color = TextMain, fontSize = 14.sp)
                            Text("Аврора · мессенджер круга", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    last: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
        if (!last) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cyan.copy(alpha = 0.12f)))
        }
    }
}
