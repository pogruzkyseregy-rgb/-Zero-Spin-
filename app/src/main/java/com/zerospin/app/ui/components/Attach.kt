package com.zerospin.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.AttachKind
import com.zerospin.app.data.Attachment
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Выбор фото и видео (системный Photo Picker, с откатом на обычный выбор на старых телефонах). */
@Composable
fun rememberPhotoPicker(maxItems: Int = 10, onPicked: (List<Attachment>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems),
    ) { uris ->
        if (uris.isNotEmpty()) onPicked(uris.map { toAttachment(context, it) })
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }
}

/** Одно фото для фона опроса. */
@Composable
fun rememberCoverPicker(onPicked: (Attachment) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(toAttachment(context, uri))
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

/** Любые файлы: документы, архивы и прочее. Фото сюда не попадают — для них есть свой выбор. */
@Composable
fun rememberFilePicker(onPicked: (List<Attachment>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            onPicked(uris.map { uri ->
                // Право на чтение на будущее. Не все провайдеры его дают, поэтому ошибку игнорируем.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                toAttachment(context, uri, forceFile = true)
            })
        }
    }
    return { launcher.launch(arrayOf("*/*")) }
}

private fun toAttachment(context: Context, uri: Uri, forceFile: Boolean = false): Attachment {
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val kind = when {
        forceFile -> AttachKind.FILE
        mime.startsWith("image/") -> AttachKind.IMAGE
        mime.startsWith("video/") -> AttachKind.VIDEO
        else -> AttachKind.FILE
    }
    return Attachment(kind, uri.toString(), displayName(context, uri), mime)
}

private fun displayName(context: Context, uri: Uri): String {
    val fromProvider = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
    return fromProvider ?: uri.lastPathSegment ?: "файл"
}

/** Загружает уменьшенную копию картинки по ссылке, чтобы лента не тормозила. */
@Composable
fun rememberThumb(uri: String, maxSide: Int = 900): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { loadThumb(context, uri, maxSide) }
    }
    return bitmap
}

private fun loadThumb(context: Context, uri: String, maxSide: Int): ImageBitmap? = runCatching {
    val parsed = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide && bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
}.getOrNull()

/**
 * Список вложений. Фото показываются превью во всю ширину, видео и файлы — строкой.
 * onRemove задаётся только в режиме редактирования (черновик).
 */
@Composable
fun AttachmentList(items: List<Attachment>, onRemove: ((Int) -> Unit)? = null) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, item ->
            val remove: (() -> Unit)? = if (onRemove == null) null else ({ onRemove(index) })
            when (item.kind) {
                AttachKind.IMAGE -> PhotoTile(item, remove)
                else -> FileRow(item, remove)
            }
        }
    }
}

@Composable
private fun PhotoTile(item: Attachment, onRemove: (() -> Unit)?) {
    val bitmap = rememberThumb(item.uri)
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .hudPanel(cut = 10.dp, fill = Bg),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        } else {
            Text("ФОТО · ЗАГРУЗКА…", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(12.dp))
        }
        if (onRemove != null) {
            Text(
                "×",
                color = Red,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clickable { onRemove() },
            )
        }
    }
}

@Composable
private fun FileRow(item: Attachment, onRemove: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(cut = 8.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (item.kind == AttachKind.VIDEO) "ВИДЕО" else "ФАЙЛ",
            color = Violet,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
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
            Text("×", color = Red, fontSize = 18.sp, modifier = Modifier.clickable { onRemove() }.padding(start = 10.dp))
        }
    }
}

/** Подпись для кнопок выбора: «Фото» и «Файл» всегда отдельно. */
val PhotoLabel = "Фото и видео"
val FileLabel = "Файл"

@Composable
fun MutedLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
}

@Composable
fun CyanText(text: String) {
    Text(text, color = Cyan, fontSize = 12.sp)
}
