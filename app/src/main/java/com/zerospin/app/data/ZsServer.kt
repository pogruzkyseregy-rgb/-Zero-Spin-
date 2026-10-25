package com.zerospin.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Ответ сервера с ошибкой: код и понятный текст. code = 0 значит нет связи. */
class ZsException(val code: Int, message: String) : Exception(message)

data class ZsMe(val nick: String, val warnings: Int, val banned: Boolean, val banReason: String, val role: String = "user")

/** Вход: ключ сессии, логин как его записал сервер, роль. */
data class ZsSession(val token: String, val login: String, val role: String)

data class ZsProfile(val login: String, val name: String, val status: String, val bio: String, val role: String, val banned: Boolean, val hasAvatar: Boolean, val avatarV: Long)

data class ZsNews(val id: Int, val text: String, val hasImage: Boolean, val at: Long, val name: String)

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

    suspend fun register(base: String, login: String, password: String): ZsSession {
        val j = call(base, "/api/register", "POST", null, JSONObject().put("login", login).put("password", password))
        return ZsSession(j.getString("token"), j.getString("login"), j.optString("role", "user"))
    }

    suspend fun login(base: String, login: String, password: String): ZsSession {
        val j = call(base, "/api/login", "POST", null, JSONObject().put("login", login).put("password", password))
        return ZsSession(j.getString("token"), j.getString("login"), j.optString("role", "user"))
    }

    suspend fun logout(base: String, token: String) {
        runCatching { call(base, "/api/logout", "POST", token, JSONObject()) }
    }

    suspend fun me(base: String, token: String): ZsMe {
        val j = call(base, "/api/me", "GET", token, null)
        return ZsMe(j.optString("login", j.optString("nick")), j.optInt("warnings"), j.optBoolean("banned"), j.optString("ban_reason"), j.optString("role", "user"))
    }

    suspend fun profile(base: String, login: String): ZsProfile {
        val j = call(base, "/api/users/$login", "GET", null, null)
        return ZsProfile(j.optString("login"), j.optString("name"), j.optString("status"), j.optString("bio"), j.optString("role"),
            j.optBoolean("banned"), j.optBoolean("has_avatar"), j.optLong("avatar_v"))
    }

    suspend fun news(base: String): List<ZsNews> {
        val arr = call(base, "/api/news", "GET", null, null).optJSONArray("news") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ZsNews(o.optInt("id"), o.optString("text"), o.optBoolean("has_image"), (o.optDouble("at") * 1000).toLong(), o.optString("name"))
        }
    }

    /** Картинка с сервера (аватар, фото новости). null, если её нет или нет связи. */
    suspend fun image(base: String, path: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 30_000 }
            try {
                if (c.responseCode != 200) null else c.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
            } finally { c.disconnect() }
        }.getOrNull()
    }

    suspend fun support(base: String, token: String, text: String): ZsReply {
        val j = call(base, "/api/support", "POST", token, JSONObject().put("text", text))
        return ZsReply(j.optString("reply", "…"), j.optBoolean("warned"), j.optString("reason"), j.optInt("warnings"),
            j.optBoolean("banned"), j.optInt("warn_limit", 2))
    }
}
