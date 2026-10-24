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

const val SECTION_NOTES = "Заметки"
const val SECTION_EVENTS = "События"
const val SECTION_POLLS = "Опросы"
const val SECTION_COIN = "Zero Spin"

private const val TILE_SUPPORT = "Аврора"

private data class AppTile(
    val title: String,
    val desc: String,
    val icon: HudIconKind,
    val live: Boolean,
)

private val AppTiles = listOf(
    AppTile(SECTION_NOTES, "Личные записи круга", HudIconKind.FEED, true),
    AppTile(SECTION_EVENTS, "Встречи и даты", HudIconKind.APPS, true),
    AppTile(SECTION_POLLS, "Голосования в чатах", HudIconKind.CHAT, true),
    AppTile(TILE_SUPPORT, "Поддержка и подсказки", HudIconKind.PROFILE, true),
    AppTile(SECTION_COIN, "Подброс монетки AvrorA / Zero Spin", HudIconKind.RING, true),
    AppTile("Игры", "Мини-игры для круга", HudIconKind.PLUS, false),
)

@Composable
fun AppsScreen(
    onOpen: (String) -> Unit,
    onOpenSupport: () -> Unit,
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppTiles) { app ->
            AppTileCard(app = app) {
                when {
                    app.title == TILE_SUPPORT -> onOpenSupport()
                    app.live -> onOpen(app.title)
                    else -> Toast.makeText(context, "Скоро: ${app.title}", Toast.LENGTH_SHORT).show()
                }
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
            Text(
                if (app.live) "ОТКРЫТЬ" else "СКОРО",
                color = if (app.live) Cyan else Violet,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
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
