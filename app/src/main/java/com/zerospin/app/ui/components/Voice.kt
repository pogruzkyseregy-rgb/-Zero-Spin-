package com.zerospin.app.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerospin.app.data.AttachKind
import com.zerospin.app.data.Attachment
import com.zerospin.app.ui.hud.HudMono
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Mint
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.Red
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet
import com.zerospin.app.ui.theme.fluoriteBrush
import kotlinx.coroutines.delay
import java.io.File

/** Минуты и секунды: 72000 мс → 1:12. */
fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Запись голосовых в файлы приложения (AAC в .m4a). */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean = runCatching {
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val f = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(64_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = f
        startedAt = System.currentTimeMillis()
        true
    }.getOrElse {
        release()
        false
    }

    /** Громкость с микрофона от 0 до 1 — для живой полоски. */
    fun level(): Float = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f).coerceIn(0f, 1f)

    fun elapsed(): Long = if (recorder != null) System.currentTimeMillis() - startedAt else 0L

    /** Остановить и вернуть вложение. Слишком короткие записи (меньше секунды) отбрасываются. */
    fun stop(): Attachment? {
        val r = recorder ?: return null
        val f = file
        val duration = elapsed()
        val ok = runCatching { r.stop() }.isSuccess
        release()
        file = null
        if (!ok || f == null || duration < 1000) {
            f?.delete()
            return null
        }
        return Attachment(AttachKind.AUDIO, Uri.fromFile(f).toString(), "Голосовое ${formatDuration(duration)}", "audio/mp4")
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        release()
        file?.delete()
        file = null
    }

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
    }
}

/**
 * Голосовое сообщение: кнопка воспроизведения, «волна» с прогрессом и длительность.
 * Волна рисуется из ссылки на файл, поэтому у каждой записи своя.
 */
@Composable
internal fun VoiceRow(item: Attachment, onRemove: (() -> Unit)?) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var player by remember(item.uri) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(item.uri) {
        onDispose {
            runCatching { player?.release() }
            player = null
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            val p = player ?: break
            progress = runCatching { if (p.duration > 0) p.currentPosition.toFloat() / p.duration else 0f }.getOrDefault(0f)
            delay(80)
        }
    }

    fun toggle() {
        val current = player
        if (playing) {
            runCatching { current?.pause() }
            playing = false
            return
        }
        val mp = current ?: runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(item.uri))
                prepare()
            }
        }.getOrNull() ?: return
        mp.setOnCompletionListener {
            playing = false
            progress = 0f
        }
        player = mp
        mp.start()
        playing = true
    }

    val seed = item.uri.hashCode()
    Row(
        Modifier
            .fillMaxWidth()
            .hudPanel(line = Violet, cut = 8.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(fluoriteBrush(), CircleShape)
                .clickable { toggle() },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (playing) "❚❚" else "▶", color = Bg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.weight(1f).height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bars = 26
            repeat(bars) { i ->
                // Псевдослучайная высота столбика, стабильная для одной записи
                val h = 6 + ((seed xor (i * 2654435761L).toInt()) ushr 3) % 20
                val lit = i.toFloat() / bars < progress
                Box(
                    Modifier
                        .weight(1f)
                        .height(h.dp)
                        .background(if (lit) Mint else Muted.copy(alpha = 0.5f)),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(item.name.removePrefix("Голосовое "), color = TextMain, fontSize = 11.sp, fontFamily = HudMono)
        if (onRemove != null) {
            Text("×", color = Red, fontSize = 18.sp, modifier = Modifier.clickable { onRemove() }.padding(start = 10.dp))
        }
    }
}
