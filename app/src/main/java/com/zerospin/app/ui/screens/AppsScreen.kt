package com.zerospin.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Violet

private data class AppTile(
    val title: String,
    val desc: String,
    val icon: HudIconKind,
    val status: String,
)

private val AppTiles = listOf(
    AppTile("Zero Spin", "Подброс монетки с виджетом", HudIconKind.RING, "СКОРО"),
    AppTile("Заметки", "Личные записи", HudIconKind.FEED, "СКОРО"),
    AppTile("События", "Календарь круга", HudIconKind.APPS, "СКОРО"),
    AppTile("Опросы", "Голосования в чатах", HudIconKind.CHAT, "СКОРО"),
    AppTile("Игры", "Мини-игры для круга", HudIconKind.PLUS, "СКОРО"),
    AppTile("Аврора", "Чат с персонажем", HudIconKind.PROFILE, "СКОРО"),
)

@Composable
fun AppsScreen() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppTiles) { app ->
            AppTileCard(app = app) {
                Toast.makeText(context, "Скоро: ${app.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun AppTileCard(app: AppTile, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .hudPanel(cut = 14.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudIcon(app.icon, Cyan, Modifier.size(30.dp))
            Text(app.status, color = Violet, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Column {
            Text(
                app.title.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(app.desc, color = Muted, fontSize = 11.sp)
        }
    }
}
