package com.zerospin.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.data.Attachment
import com.zerospin.app.data.BlockKind
import com.zerospin.app.data.BlockTemplates
import com.zerospin.app.data.Profile
import com.zerospin.app.data.ProfileBlock
import com.zerospin.app.ui.components.rememberCoverPicker
import com.zerospin.app.ui.components.rememberPhotoPicker
import com.zerospin.app.ui.components.rememberThumb
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
 * Конструктор профиля: шаблоны, порядок блоков, добавление и удаление,
 * а также обложка, фото и друзья прямо в карточках блоков.
 * Изменения сохраняются сразу.
 */
@Composable
fun ProfileBuilderScreen(
    profile: Profile,
    onProfile: (Profile) -> Unit,
    onDismiss: () -> Unit,
) {
    val blocks = profile.blocks
    fun setBlocks(list: List<ProfileBlock>) = onProfile(profile.copy(blocks = list))

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
                        TemplateChip(name = name, active = blocks == template) { setBlocks(template) }
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
                        onUp = { setBlocks(swap(blocks, index, index - 1)) },
                        onDown = { setBlocks(swap(blocks, index, index + 1)) },
                        onRemove = { setBlocks(blocks.filterIndexed { i, _ -> i != index }) },
                        onText = { text ->
                            setBlocks(blocks.mapIndexed { i, b -> if (i == index) b.copy(text = text) else b })
                        },
                    ) {
                        when (block.kind) {
                            BlockKind.COVER -> CoverEditor(profile.coverUri) { onProfile(profile.copy(coverUri = it)) }
                            BlockKind.PHOTOS -> PhotosEditor(profile.photos) { onProfile(profile.copy(photos = it)) }
                            BlockKind.FRIENDS -> FriendsEditor(profile.friends) { onProfile(profile.copy(friends = it)) }
                            else -> Text(block.kind.hint, color = Muted, fontSize = 11.sp)
                        }
                    }
                }

                val available = BlockKind.values().filter { kind -> blocks.none { it.kind == kind } }
                SectionLabel("ДОСТУПНЫЕ БЛОКИ")
                if (available.isEmpty()) {
                    Text("Все блоки уже в профиле", color = Muted, fontSize = 12.sp)
                }
                available.forEach { kind ->
                    LibraryCard(kind = kind) { setBlocks(blocks + ProfileBlock(kind)) }
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

/** Блок в профиле: порядок (↑ ↓), удаление, текст или содержимое блока (body). */
@Composable
private fun ActiveBlockCard(
    block: ProfileBlock,
    index: Int,
    count: Int,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    onText: (String) -> Unit,
    body: @Composable () -> Unit,
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
            body()
        }
    }
}

/** Обложка: превью и выбор фото. */
@Composable
private fun CoverEditor(coverUri: String?, onChange: (String?) -> Unit) {
    val pick = rememberCoverPicker { onChange(it.uri) }
    val preview = coverUri?.let { rememberThumb(it, 800) }
    Column(Modifier.fillMaxWidth()) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .hudPanel(cut = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudButton(
                text = if (preview == null) "Выбрать обложку" else "Заменить",
                onClick = pick,
                modifier = Modifier.weight(1f),
            )
            if (coverUri != null) {
                Spacer(Modifier.width(12.dp))
                Text("УБРАТЬ", color = Red, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.clickable { onChange(null) })
            }
        }
    }
}

/** Фото для карусели: добавление нескольких и удаление по одному. */
@Composable
private fun PhotosEditor(photos: List<Attachment>, onChange: (List<Attachment>) -> Unit) {
    val pick = rememberPhotoPicker(maxItems = 20) { onChange(photos + it) }
    Column(Modifier.fillMaxWidth()) {
        if (photos.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEachIndexed { index, photo ->
                    val thumb = rememberThumb(photo.uri, 200)
                    Box(Modifier.size(64.dp).hudPanel(cut = 6.dp)) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().padding(3.dp),
                            )
                        }
                        Text(
                            "×",
                            color = Red,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Bg.copy(alpha = 0.8f))
                                .clickable { onChange(photos.filterIndexed { i, _ -> i != index }) }
                                .padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        HudButton(
            text = if (photos.isEmpty()) "Добавить фото" else "Добавить ещё",
            onClick = pick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Друзья: поле для ника, добавление и удаление. */
@Composable
private fun FriendsEditor(friends: List<String>, onChange: (List<String>) -> Unit) {
    var input by remember { mutableStateOf("") }
    val nick = input.trim().removePrefix("@")
    val canAdd = nick.isNotEmpty() && nick !in friends
    Column(Modifier.fillMaxWidth()) {
        HudTextField(
            value = input,
            onValueChange = { input = it },
            label = "Ник друга",
            placeholder = "adonis",
            maxLength = 20,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        HudButton(
            text = "Добавить друга",
            onClick = {
                if (canAdd) {
                    onChange(friends + nick)
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (friends.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            friends.forEach { friend ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("@$friend", color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        "УБРАТЬ",
                        color = Red,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { onChange(friends - friend) }.padding(4.dp),
                    )
                }
            }
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
