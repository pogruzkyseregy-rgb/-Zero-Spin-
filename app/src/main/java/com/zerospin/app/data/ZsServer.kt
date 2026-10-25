package com.zerospin.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Ответ сервера с ошибкой: код и понятный текст. code = 0 значит нет связи. */
class ZsException(val code: Int, message: String) : Exception(message)

data class ZsMe(val nick: String, val warnings: Int, val banned: Boolean, val banReason: String)

data class ZsReply(
    val reply: String,
    val warned: Boolean,
    val reason: String,
    val warnings: Int,
    val banned: Boolean,
    val warnLimit: Int,
)

/** Связь с сервером Zero Spin. Без сторонних библиотек: HttpURLConnection и org.json. */
object ZsServer {
    private suspend fun call(base: String, path: String, method: String, token: String?, body: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = URL(base.trimEnd('/') + path)
            val c = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 95_000
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            try {
                if (body != null) c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                if (code !in 200..299) throw ZsException(code, json.optString("detail", "ошибка сервера ($code)"))
                json
            } catch (e: IOException) {
                throw ZsException(0, "нет связи с сервером")
            } finally {
                c.disconnect()
            }
        }

    suspend fun health(base: String): Boolean = runCatching { call(base, "/api/health", "GET", null, null).optBoolean("ok") }.getOrDefault(false)

    /** Возвращает (токен, ник). */
    suspend fun register(base: String, nick: String, name: String): Pair<String, String> {
        val j = call(base, "/api/register", "POST", null, JSONObject().put("nick", nick).put("name", name))
        return Pair(j.getString("token"), j.getString("nick"))
    }

    suspend fun me(base: String, token: String): ZsMe {
        val j = call(base, "/api/me", "GET", token, null)
        return ZsMe(j.optString("nick"), j.optInt("warnings"), j.optBoolean("banned"), j.optString("ban_reason"))
    }

    suspend fun support(base: String, token: String, text: String): ZsReply {
        val j = call(base, "/api/support", "POST", token, JSONObject().put("text", text))
        return ZsReply(j.optString("reply", "…"), j.optBoolean("warned"), j.optString("reason"), j.optInt("warnings"),
            j.optBoolean("banned"), j.optInt("warn_limit", 2))
    }
}
