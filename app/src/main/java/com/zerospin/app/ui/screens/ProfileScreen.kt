package com.zerospin.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.ui.components.Avatar
import com.zerospin.app.ui.theme.CardBg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain

@Composable
fun ProfileScreen(postCount: Int, chatCount: Int) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(name = "Адонис", accent = Cyan, size = 96.dp)
        Spacer(Modifier.height(12.dp))
        Text("Адонис", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("@adonis", color = Cyan)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Пока без описания.",
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardBg,
            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(value = postCount.toString(), label = "постов")
                StatItem(value = chatCount.toString(), label = "чатов")
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { Toast.makeText(context, "Скоро", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Редактировать")
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
