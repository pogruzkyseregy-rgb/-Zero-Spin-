package com.zerospin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.BlockKind
import com.zerospin.app.data.BlockTemplates
import com.zerospin.app.data.ProfileBlock
import com.zerospin.app.ui.hud.HudButton
import com.zerospin.app.ui.hud.HudTextField
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Panel
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

/**
 * Конструктор профиля: шаблоны, порядок блоков, добавление и удаление.
 * Открывается из настроек и с вкладки профиля, изменения сохраняются сразу.
 */
@Composable
fun ProfileBuilderScreen(
    blocks: List<ProfileBlock>,
    onChange: (List<ProfileBlock>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("КОНСТРУКТОР", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 3.sp)
                        Text("Соберите профиль из блоков", color = Muted, fontSize = 11.sp)
                    }
                    HudButton(text = "Готово", onClick = onDismiss, primary = true)
                }

                SectionLabel("ШАБЛОНЫ")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BlockTemplates.forEach { (name, template) ->
                        TemplateChip(name = name, active = blocks == template) { onChange(template) }
                    }
                }

                SectionLabel("ВАШИ БЛОКИ · ${blocks.size}")
                if (blocks.isEmpty()) {
                    Text("Блоков нет. Добавьте из списка ниже или выберите шаблон", color = Muted, fontSize = 12.sp)
                }
                blocks.forEachIndexed { index, block ->
                    ActiveBlockCard(
                        block = block,
                        index = index,
                        count = blocks.size,
                        onUp = { onChange(swap(blocks, index, index - 1)) },
                        onDown = { onChange(swap(blocks, index, index + 1)) },
                        onRemove = { onChange(blocks.filterIndexed { i, _ -> i != index }) },
                        onText = { text ->
                            onChange(blocks.mapIndexed { i, b -> if (i == index) b.copy(text = text) else b })
                        },
                    )
                }

                val available = BlockKind.values().filter { kind -> blocks.none { it.kind == kind } }
                SectionLabel("ДОСТУПНЫЕ БЛОКИ")
                if (available.isEmpty()) {
                    Text("Все блоки уже в профиле", color = Muted, fontSize = 12.sp)
                }
                available.forEach { kind ->
                    LibraryCard(kind = kind) { onChange(blocks + ProfileBlock(kind)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun swap(list: List<ProfileBlock>, a: Int, b: Int): List<ProfileBlock> {
    if (b < 0 || b >= list.size) return list
    return list.toMutableList().also {
        val tmp = it[a]
        it[a] = it[b]
        it[b] = tmp
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Violet, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun TemplateChip(name: String, active: Boolean, onClick: () -> Unit) {
    Text(
        name.uppercase(),
        color = if (active) Bg else Cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .hudPanel(
                line = Cyan,
                fill = if (active) Cyan else Panel,
                cut = 8.dp,
                glow = active,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Блок в профиле: порядок (↑ ↓), удаление и текст, если у вида блока он есть. */
@Composable
private fun ActiveBlockCard(
    block: ProfileBlock,
    index: Int,
    count: Int,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    onText: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().hudPanel(cut = 12.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "БЛОК ${(index + 1).toString().padStart(2, '0')} · ${block.kind.title}",
                color = Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Выше",
                tint = if (index > 0) Cyan else Muted.copy(alpha = 0.3f),
                modifier = Modifier.size(26.dp).clickable(enabled = index > 0, onClick = onUp),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Ниже",
                tint = if (index < count - 1) Cyan else Muted.copy(alpha = 0.3f),
                modifier = Modifier.size(26.dp).clickable(enabled = index < count - 1, onClick = onDown),
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "Убрать блок",
                tint = Red,
                modifier = Modifier.size(24.dp).clickable(onClick = onRemove),
            )
        }
        Spacer(Modifier.height(8.dp))
        if (block.kind.hasText) {
            HudTextField(
                value = block.text,
                onValueChange = onText,
                label = block.kind.title.lowercase().replaceFirstChar { it.uppercase() },
                placeholder = block.kind.hint,
                maxLength = 80,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(block.kind.hint, color = Muted, fontSize = 11.sp)
        }
    }
}

/** Блок, которого ещё нет в профиле. */
@Composable
private fun LibraryCard(kind: BlockKind, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().hudPanel(line = Violet, cut = 12.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(kind.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
            Text(kind.hint, color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "+ ДОБАВИТЬ",
            color = Violet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.clickable(onClick = onAdd).padding(6.dp),
        )
    }
}
