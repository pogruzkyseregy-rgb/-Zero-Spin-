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

data class ZsProfile(val login: String, val name: String, val status: String, val bio: String, val role: String, val banned: Boolean, val hasAvatar: Boolean, val avatarV: Long, val cosmetic: String = "")

data class ZsNews(val id: Int, val text: String, val hasImage: Boolean, val at: Long, val name: String)

/** Человек на сервере, как его видят другие. relation: self, friends, outgoing, incoming, none. */
data class ZsPerson(
    val login: String,
    val name: String,
    val status: String,
    val bio: String,
    val role: String,
    val banned: Boolean,
    val hasAvatar: Boolean,
    val avatarV: Long,
    val relation: String = "",
    /** Украшение аватара: halo, demon, orbit, hat, crown, stars или пусто */
    val cosmetic: String = "",
) {
    val team: Boolean get() = role == "admin" || role == "aurora"
}

data class ZsFriends(val friends: List<ZsPerson>, val incoming: List<ZsPerson>, val outgoing: List<ZsPerson>)

/** Личное сообщение с сервера. */
data class ZsDm(val id: Int, val from: String, val to: String, val text: String, val hasImage: Boolean, val at: Long)

data class ZsSync(val messages: List<ZsDm>, val incoming: Int, val lastId: Int, val more: Boolean)

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
            j.optBoolean("banned"), j.optBoolean("has_avatar"), j.optLong("avatar_v"), j.optString("cosmetic"))
    }

    suspend fun news(base: String): List<ZsNews> {
        val arr = call(base, "/api/news", "GET", null, null).optJSONArray("news") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ZsNews(o.optInt("id"), o.optString("text"), o.optBoolean("has_image"), (o.optDouble("at") * 1000).toLong(), o.optString("name"))
        }
    }

    /** Команда «!аватар»: админ ставит Авроре новый аватар. Картинка уже сжата в JPEG. */
    suspend fun setAuroraAvatar(base: String, token: String, jpeg: ByteArray) {
        val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
        call(base, "/api/admin/aurora-avatar", "POST", token, JSONObject().put("image", b64))
    }

    private fun person(o: JSONObject) = ZsPerson(
        o.optString("login"), o.optString("name"), o.optString("status"), o.optString("bio"), o.optString("role", "user"),
        o.optBoolean("banned"), o.optBoolean("has_avatar"), o.optLong("avatar_v"), o.optString("relation"), o.optString("cosmetic"),
    )

    private fun people(arr: org.json.JSONArray?): List<ZsPerson> =
        if (arr == null) emptyList() else (0 until arr.length()).map { person(arr.getJSONObject(it)) }

    private fun dm(o: JSONObject) = ZsDm(o.optInt("id"), o.optString("from"), o.optString("to"), o.optString("text"),
        o.optBoolean("has_image"), (o.optDouble("at") * 1000).toLong())

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    suspend fun search(base: String, token: String, q: String): List<ZsPerson> =
        people(call(base, "/api/people/search?q=${enc(q)}", "GET", token, null).optJSONArray("people"))

    suspend fun person(base: String, token: String, login: String): ZsPerson =
        person(call(base, "/api/people/${enc(login)}", "GET", token, null))

    suspend fun friends(base: String, token: String): ZsFriends {
        val j = call(base, "/api/friends", "GET", token, null)
        return ZsFriends(people(j.optJSONArray("friends")), people(j.optJSONArray("incoming")), people(j.optJSONArray("outgoing")))
    }

    /** action: request (позвать), accept (принять), remove (отклонить, отменить, убрать из друзей). */
    suspend fun friend(base: String, token: String, action: String, login: String): ZsPerson =
        person(call(base, "/api/friends/$action", "POST", token, JSONObject().put("login", login)))

    suspend fun sendDm(base: String, token: String, login: String, text: String, jpeg: ByteArray? = null): ZsDm {
        val body = JSONObject().put("text", text)
        if (jpeg != null) body.put("image", android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP))
        return dm(call(base, "/api/dm/${enc(login)}", "POST", token, body))
    }

    suspend fun sync(base: String, token: String, after: Int): ZsSync {
        val j = call(base, "/api/sync?after=$after", "GET", token, null)
        val arr = j.optJSONArray("messages")
        val list = if (arr == null) emptyList() else (0 until arr.length()).map { dm(arr.getJSONObject(it)) }
        return ZsSync(list, j.optInt("incoming"), j.optInt("last_id", after), j.optBoolean("more"))
    }

    suspend fun updateMe(base: String, token: String, name: String, status: String, bio: String, cosmetic: String): ZsPerson =
        person(call(base, "/api/me/profile", "POST", token,
            JSONObject().put("name", name).put("status", status).put("bio", bio).put("cosmetic", cosmetic)))

    suspend fun uploadMyAvatar(base: String, token: String, jpeg: ByteArray): ZsPerson {
        val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
        return person(call(base, "/api/me/avatar", "POST", token, JSONObject().put("image", b64)))
    }

    /** Удалить аккаунт насовсем. Нужен пароль. */
    suspend fun deleteAccount(base: String, token: String, password: String) {
        call(base, "/api/me/delete", "POST", token, JSONObject().put("password", password))
    }

    /** Скачать файл с сервера (фото из лички) на телефон. true, если получилось. */
    suspend fun download(base: String, token: String, path: String, file: java.io.File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000; readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (c.responseCode != 200) return@runCatching false
                file.parentFile?.mkdirs()
                val tmp = java.io.File(file.path + ".part")
                c.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(file)
            } finally { c.disconnect() }
        }.getOrDefault(false)
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

/**
 * Фото с телефона → квадратный JPEG для аватара. Вертикальное фото режется ближе к верху,
 * где обычно лицо. null, если картинку не удалось прочитать.
 */
suspend fun squareAvatarJpeg(context: android.content.Context, uri: String, size: Int = 512): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val u = android.net.Uri.parse(uri)
        val r = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(u)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
        val src = r.openInputStream(u)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = ((src.height - side) * 0.2f).toInt()
        val square = android.graphics.Bitmap.createBitmap(src, x, y, side, side)
        val out = android.graphics.Bitmap.createScaledBitmap(square, size, size, true)
        java.io.ByteArrayOutputStream().use { bos ->
            out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, bos)
            bos.toByteArray()
        }
    }.getOrNull()
}

/** Фото для лички: длинная сторона не больше maxSide, JPEG. null, если картинку не удалось прочитать. */
suspend fun scaledJpeg(context: android.content.Context, uri: String, maxSide: Int = 1600): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val u = android.net.Uri.parse(uri)
        val r = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(u)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        var bmp = r.openInputStream(u)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val long = maxOf(bmp.width, bmp.height)
        if (long > maxSide) {
            val k = maxSide.toFloat() / long
            bmp = android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
        }
        java.io.ByteArrayOutputStream().use { bos ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, bos)
            bos.toByteArray()
        }
    }.getOrNull()
}
