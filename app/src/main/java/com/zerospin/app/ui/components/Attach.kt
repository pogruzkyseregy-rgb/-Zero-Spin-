package com.zerospin.app.ui.components

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.AttachKind
import com.zerospin.app.data.Attachment
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

/**
 * Возвращает функцию, которая открывает системный выбор файлов.
 * Выбранные файлы передаются в onPicked как ссылки (uri), сами файлы не копируются.
 */
@Composable
fun rememberAttachPicker(onPicked: (List<Attachment>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val list = uris.map { uri ->
            // Право на чтение на будущее. Не все провайдеры его дают, поэтому ошибку игнорируем.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            Attachment(
                kind = kindOf(mime),
                uri = uri.toString(),
                name = displayName(context, uri),
                mime = mime,
            )
        }
        onPicked(list)
    }
    return { launcher.launch(arrayOf("*/*")) }
}

private fun kindOf(mime: String): AttachKind = when {
    mime.startsWith("image/") -> AttachKind.IMAGE
    mime.startsWith("video/") -> AttachKind.VIDEO
    else -> AttachKind.FILE
}

private fun displayName(context: android.content.Context, uri: android.net.Uri): String {
    val fromProvider = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
    return fromProvider ?: uri.lastPathSegment ?: "файл"
}

private fun kindLabel(kind: AttachKind): String = when (kind) {
    AttachKind.IMAGE -> "ФОТО"
    AttachKind.VIDEO -> "ВИДЕО"
    AttachKind.FILE -> "ФАЙЛ"
}

/** Список вложений. onRemove задаётся только в режиме редактирования сообщения. */
@Composable
fun AttachmentList(items: List<Attachment>, onRemove: ((Int) -> Unit)? = null) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .hudPanel(cut = 8.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(kindLabel(item.kind), color = Violet, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    item.name,
                    color = TextMain,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onRemove != null) {
                    Text(
                        "×",
                        color = Red,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onRemove(index) }.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MutedLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
}
