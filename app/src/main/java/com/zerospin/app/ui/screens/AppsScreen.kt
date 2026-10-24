package com.zerospin.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.HudMono
import com.zerospin.app.ui.hud.QuantumAtom
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.FluoriteColors
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Violet

const val SECTION_NOTES = "Заметки"
const val SECTION_EVENTS = "События"
const val SECTION_POLLS = "Опросы"
const val SECTION_COIN = "Zero Spin"

private const val TILE_SUPPORT = "Аврора"
private const val TILE_SAVED = "Избранное"

private data class AppTile(
    val title: String,
    val desc: String,
    val icon: HudIconKind,
    val live: Boolean,
)

private val AppTiles = listOf(
    AppTile(SECTION_NOTES, "Личные записи", HudIconKind.FEED, true),
    AppTile(SECTION_EVENTS, "Встречи и даты", HudIconKind.APPS, true),
    AppTile(SECTION_POLLS, "Голосования круга", HudIconKind.CHAT, true),
    AppTile(TILE_SAVED, "Сохранённые сообщения", HudIconKind.SEARCH, true),
    AppTile(SECTION_COIN, "Монетка AvrorA / Zero Spin", HudIconKind.RING, true),
    AppTile("Игры", "Мини-игры для круга", HudIconKind.PLUS, false),
)

@Composable
fun AppsScreen(
    onOpen: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSaved: () -> Unit,
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Большая плитка Авроры во всю ширину
        item(span = { GridItemSpan(2) }) {
            AuroraTile(onClick = onOpenSupport)
        }
        items(AppTiles) { app ->
            AppTileCard(app = app) {
                when {
                    app.title == TILE_SAVED -> onOpenSaved()
                    app.live -> onOpen(app.title)
                    else -> Toast.makeText(context, "Скоро: ${app.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun AuroraTile(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(line = Violet, cut = 16.dp, glow = true)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TargetRing(Modifier.size(64.dp), color = Violet, ears = true) {
            QuantumAtom(Modifier.size(40.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text("АВРОРА", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 3.sp)
            Text("квантовый ИИ · поддержка круга", color = Muted, fontSize = 11.sp, fontFamily = HudMono)
            Spacer(Modifier.height(6.dp))
            Text("Спросите, как устроен Zero Spin", color = Mint, fontSize = 12.sp)
        }
        Text("›", color = Violet, fontSize = 28.sp)
    }
}

@Composable
private fun AppTileCard(app: AppTile, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .hudPanel(cut = 14.dp, line = if (app.live) Cyan else Muted)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), Color.Transparent))),
                )
                HudIcon(app.icon, if (app.live) Cyan else Muted, Modifier.size(28.dp))
            }
            Text(
                if (app.live) "ОТКРЫТЬ" else "СКОРО",
                color = if (app.live) Mint else Violet,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontFamily = HudMono,
            )
        }
        Column {
            Box(
                Modifier
                    .size(width = 28.dp, height = 2.dp)
                    .background(Brush.horizontalGradient(FluoriteColors)),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                app.title.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(app.desc, color = Muted, fontSize = 11.sp)
        }
    }
}
