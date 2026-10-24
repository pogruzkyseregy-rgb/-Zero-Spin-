package com.zerospin.app.data

import android.content.Context
import java.security.SecureRandom

/**
 * Общее состояние монетки для экрана приложения и виджета на рабочем столе.
 * SIDE_A = AvrorA, SIDE_B = Zero Spin.
 */
object CoinStore {
    const val SIDE_A = 0
    const val SIDE_B = 1
    const val HISTORY_SIZE = 12

    private const val PREFS = "coin"
    private val rng = SecureRandom()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Честный бросок 50/50 на криптостойком генераторе. */
    fun toss(): Int = if (rng.nextBoolean()) SIDE_A else SIDE_B

    fun last(context: Context): Int = prefs(context).getInt("last", SIDE_A)
    fun countA(context: Context): Int = prefs(context).getInt("a", 0)
    fun countB(context: Context): Int = prefs(context).getInt("b", 0)
    fun hasFlipped(context: Context): Boolean = prefs(context).getBoolean("flipped", false)

    /** История: самый новый бросок первым, a = AvrorA, b = Zero Spin. */
    fun history(context: Context): String = prefs(context).getString("hist", "") ?: ""

    @Synchronized
    fun record(context: Context, side: Int) {
        val p = prefs(context)
        val key = if (side == SIDE_A) "a" else "b"
        val hist = ((if (side == SIDE_A) "a" else "b") + (p.getString("hist", "") ?: "")).take(HISTORY_SIZE)
        p.edit()
            .putInt("last", side)
            .putInt(key, p.getInt(key, 0) + 1)
            .putString("hist", hist)
            .putBoolean("flipped", true)
            .apply()
    }

    /** Сбрасывает счётчики и историю, последнюю сторону оставляет. */
    fun reset(context: Context) {
        val last = last(context)
        prefs(context).edit().clear().putInt("last", last).apply()
    }
}
