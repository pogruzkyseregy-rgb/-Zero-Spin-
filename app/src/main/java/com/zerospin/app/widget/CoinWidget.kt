package com.zerospin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.zerospin.app.MainActivity
import com.zerospin.app.R
import com.zerospin.app.data.CoinStore

/** Виджет 2x2: нажатие на монету подбрасывает её прямо на рабочем столе. */
class CoinWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            manager.updateAppWidget(id, buildViews(context, CoinStore.last(context)))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_FLIP || animating) return
        animating = true

        val app = context.applicationContext
        val pending = goAsync()
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, CoinWidget::class.java))

        val from = CoinStore.last(app)
        val result = CoinStore.toss()
        val frames = buildFrames(from, result)
        val handler = Handler(Looper.getMainLooper())

        // Пока монета крутится, подпись прячем
        val start = RemoteViews(app.packageName, R.layout.widget_coin).apply {
            setTextViewText(R.id.w_label, " ")
        }
        ids.forEach { manager.partiallyUpdateAppWidget(it, start) }

        frames.forEachIndexed { index, frame ->
            handler.postDelayed({
                if (index == frames.lastIndex) {
                    CoinStore.record(app, result)
                    ids.forEach { manager.updateAppWidget(it, buildViews(app, result)) }
                    animating = false
                    pending.finish()
                } else {
                    val rv = RemoteViews(app.packageName, R.layout.widget_coin).apply {
                        setImageViewResource(R.id.w_coin, frame)
                    }
                    ids.forEach { manager.partiallyUpdateAppWidget(it, rv) }
                }
            }, FRAME_MS * index)
        }
    }

    companion object {
        const val ACTION_FLIP = "com.zerospin.app.FLIP"
        private const val FRAME_MS = 45L

        @Volatile
        private var animating = false

        /** Кадры вращения: на каждый полуоборот шесть кадров (сжатие, ребро, раскрытие). */
        private fun buildFrames(from: Int, result: Int): List<Int> {
            val halfTurns = 6 + if (from == result) 0 else 1
            val frames = mutableListOf<Int>()
            var side = from
            repeat(halfTurns) {
                val next = 1 - side
                frames += if (side == CoinStore.SIDE_A) R.drawable.w_a1 else R.drawable.w_b1
                frames += if (side == CoinStore.SIDE_A) R.drawable.w_a2 else R.drawable.w_b2
                frames += R.drawable.w_edge
                frames += if (next == CoinStore.SIDE_A) R.drawable.w_a2 else R.drawable.w_b2
                frames += if (next == CoinStore.SIDE_A) R.drawable.w_a1 else R.drawable.w_b1
                frames += if (next == CoinStore.SIDE_A) R.drawable.w_a0 else R.drawable.w_b0
                side = next
            }
            return frames
        }

        fun buildViews(context: Context, side: Int): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.widget_coin)
            rv.setImageViewResource(
                R.id.w_coin,
                if (side == CoinStore.SIDE_A) R.drawable.w_a0 else R.drawable.w_b0,
            )
            rv.setTextViewText(
                R.id.w_label,
                context.getString(if (side == CoinStore.SIDE_A) R.string.side_a else R.string.side_b),
            )

            val flip = Intent(context, CoinWidget::class.java).setAction(ACTION_FLIP)
            rv.setOnClickPendingIntent(
                R.id.w_coin,
                PendingIntent.getBroadcast(
                    context, 0, flip,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            val open = Intent(context, MainActivity::class.java)
            rv.setOnClickPendingIntent(
                R.id.w_label,
                PendingIntent.getActivity(
                    context, 1, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            return rv
        }

        /** Обновляет все виджеты монетки, например после броска в приложении. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CoinWidget::class.java))
            val side = CoinStore.last(context)
            ids.forEach { manager.updateAppWidget(it, buildViews(context, side)) }
        }
    }
}
