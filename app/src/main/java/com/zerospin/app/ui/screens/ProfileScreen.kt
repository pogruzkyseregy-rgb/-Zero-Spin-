package com.zerospin.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.SegmentedBar
import com.zerospin.app.ui.hud.TargetRing
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain

@Composable
fun ProfileScreen(postCount: Int, chatCount: Int) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TargetRing(Modifier.size(120.dp)) {
            Text("А", color = Cyan, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("АДОНИС", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 3.sp)
        Text("@adonis", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Пока без описания",
            color = TextMain.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        StatBlock(label = "ЗАПИСЕЙ", value = postCount, goal = 10)
        Spacer(Modifier.height(12.dp))
        StatBlock(label = "КАНАЛОВ", value = chatCount, goal = 5)
        Spacer(Modifier.height(24.dp))

        HudButton(
            text = "Редактировать",
            onClick = { Toast.makeText(context, "Скоро", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatBlock(label: String, value: Int, goal: Int) {
    Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Text(value.toString(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        SegmentedBar(progress = value.toFloat() / goal)
    }
}
