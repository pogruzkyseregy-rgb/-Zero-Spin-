package com.zerospin.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Mint

/**
 * Простая разметка в сообщениях, как в Telegram:
 * **жирный**, __подчёркнутый__, ~~зачёркнутый~~, `код`, _курсив_ и ссылки http(s)://…
 */
private val FormatRegex = Regex("""\*\*(.+?)\*\*|__(.+?)__|~~(.+?)~~|`([^`]+)`|_(.+?)_|(https?://\S+)""")

private val UrlRegex = Regex("""https?://\S+""")

fun formatMessage(text: String, highlight: String = ""): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in FormatRegex.findAll(text)) {
        appendHighlighted(text.substring(last, m.range.first), highlight)
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendHighlighted(g[1], highlight) }
            g[2].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { appendHighlighted(g[2], highlight) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendHighlighted(g[3], highlight) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = Mint, background = Color(0x2233FFC3))) {
                append(g[4])
            }
            g[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendHighlighted(g[5], highlight) }
            g[6].isNotEmpty() -> withStyle(SpanStyle(color = Cyan, textDecoration = TextDecoration.Underline)) { append(g[6]) }
        }
        last = m.range.last + 1
    }
    appendHighlighted(text.substring(last), highlight)
}

/** Добавляет текст, подсвечивая совпадения поиска. */
private fun AnnotatedString.Builder.appendHighlighted(part: String, highlight: String) {
    if (highlight.isBlank()) {
        append(part)
        return
    }
    var i = 0
    while (i < part.length) {
        val hit = part.indexOf(highlight, i, ignoreCase = true)
        if (hit < 0) {
            append(part.substring(i))
            return
        }
        append(part.substring(i, hit))
        withStyle(SpanStyle(background = Color(0x66FF5BD8), color = Color.White)) {
            append(part.substring(hit, hit + highlight.length))
        }
        i = hit + highlight.length
    }
}

/** Первая ссылка в тексте или null. */
fun firstUrl(text: String): String? = UrlRegex.find(text)?.value

/** Текст без разметки — для превью и поиска. */
fun plainText(text: String): String =
    text.replace(Regex("""\*\*|__|~~|`"""), "")
