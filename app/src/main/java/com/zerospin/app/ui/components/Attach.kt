package com.zerospin.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/**
 * Выбор фото и видео (системный Photo Picker).
 * Фото копируются во внутреннюю память приложения, чтобы не пропадали после перезапуска.
 */
@Composable
fun rememberPhotoPicker(maxItems: Int = 10, onPicked: (List<Attachment>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val list = withContext(Dispatchers.IO) { uris.map { storedAttachment(context, it) } }
                onPicked(list)
            }
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }
}

/** Одно фото (обложка, аватар, история). Тоже сохраняется в память приложения. */
@Composable
fun rememberCoverPicker(onPicked: (Attachment) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val item = withContext(Dispatchers.IO) { storedAttachment(context, uri) }
                onPicked(item)
            }
        }
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

/**
 * Вложение из галереи: право на чтение на будущее, а фото — копия в папке приложения.
 * Если скопировать не вышло, остаётся исходная ссылка.
 */
private fun storedAttachment(context: Context, uri: Uri): Attachment {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val base = toAttachment(context, uri)
    if (base.kind != AttachKind.IMAGE) return base
    val copied = runCatching {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val ext = when (base.mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val file = File(dir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
        val ok = context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (ok) Uri.fromFile(file).toString() else null
    }.getOrNull()
    return if (copied != null) base.copy(uri = copied) else base
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
                AttachKind.AUDIO -> VoiceRow(item, remove)
                else -> FileRow(item, remove)
            }
        }
    }
}

/**
 * Фото в ленте и чатах. Без срезанной панели: картинка сидит в прямоугольной рамке
 * с естественными пропорциями (в пределах разумного), поэтому ничего не налезает на углы.
 */
@Composable
private fun PhotoTile(item: Attachment, onRemove: (() -> Unit)?) {
    val preview = rememberThumb(item.uri, 900)
    var open by remember { mutableStateOf(false) }
    val ratio = preview?.let { (it.width.toFloat() / it.height).coerceIn(0.75f, 1.9f) } ?: 1.33f

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(Bg)
                .border(1.dp, Muted.copy(alpha = 0.5f), RectangleShape)
                .clickable(enabled = preview != null) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("ФОТО · ЗАГРУЗКА…", color = Muted, fontSize = 10.sp)
            }
        }
        if (onRemove != null) {
            Text(
                "×",
                color = Red,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Bg.copy(alpha = 0.7f))
                    .clickable { onRemove() }
                    .padding(horizontal = 6.dp),
            )
        }
    }
    if (open) PhotoViewer(item, onDismiss = { open = false })
}

/** Установка фото профиля из любого места приложения. Задаётся в ZeroSpinApp. */
val LocalAvatarSetter = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Копирует фото в галерею (папка Pictures/Zero Spin). Возвращает true при успехе. */
private fun saveToGallery(context: Context, item: Attachment): Boolean = runCatching {
    val resolver = context.contentResolver
    val ext = when (item.mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "zerospin_${System.currentTimeMillis()}.$ext")
        put(MediaStore.Images.Media.MIME_TYPE, if (ext == "jpg") "image/jpeg" else item.mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zero Spin")
        }
    }
    val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val copied = resolver.openInputStream(Uri.parse(item.uri))?.use { input ->
        resolver.openOutputStream(target)?.use { input.copyTo(it) } != null
    } ?: false
    if (!copied) resolver.delete(target, null, null)
    copied
}.getOrDefault(false)

/** Полноэкранный просмотр: фото во весь экран, двойной тап и щипок для увеличения, ✕ закрывает. */
/** Фото изображения по ссылке как вложение: для обложки, аватара и т.п. */
fun imageAttachment(uri: String, name: String = "фото"): Attachment =
    Attachment(AttachKind.IMAGE, uri, name, "image/jpeg")

/** Полноэкранный просмотр любого фото: зум, сохранение в галерею, установка на профиль. */
@Composable
fun PhotoViewer(item: Attachment, onDismiss: () -> Unit) {
    val full = rememberThumb(item.uri, 2048)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setAvatar = LocalAvatarSetter.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var notice by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (full != null) {
                Image(
                    bitmap = full,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                )
            } else {
                Text("ЗАГРУЗКА…", color = Muted, fontSize = 10.sp)
            }
            Text(
                "✕",
                color = TextMain,
                fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Bg.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ViewerAction("СОХРАНИТЬ") {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { saveToGallery(context, item) }
                        notice = if (ok) "Сохранено в галерею" else "Не удалось сохранить"
                    }
                }
                if (setAvatar != null) {
                    ViewerAction("НА ПРОФИЛЬ") {
                        setAvatar(item.uri)
                        notice = "Фото профиля обновлено"
                    }
                }
            }
            notice?.let {
                Text(
                    it,
                    color = Cyan,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 92.dp)
                        .background(Bg.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ViewerAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Bg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .background(Cyan)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
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
