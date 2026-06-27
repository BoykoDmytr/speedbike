package com.speedbike.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.speedbike.app.MainActivity
import com.speedbike.app.R
import com.speedbike.app.data.SettingsStore
import com.speedbike.app.data.db.AppDatabase
import com.speedbike.app.data.pet.PetMood
import com.speedbike.app.data.pet.PetStore
import com.speedbike.app.util.RideAnalytics
import com.speedbike.app.util.Units
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home-screen widget: shows today's distance, this week's total and the pet's
 * current mood. Refreshed whenever a ride is saved (see [updateAll]).
 */
class SpeedBikeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = loadData(context)
                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) render(context, appWidgetManager, id, data)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun loadData(context: Context): WidgetData {
        val now = System.currentTimeMillis()
        val dao = AppDatabase.get(context).rideDao()
        val todayM = runCatching { dao.sumDistanceMetersSince(RideAnalytics.startOfDay(now)) }.getOrDefault(0.0)
        val weekM = runCatching { dao.sumDistanceMetersSince(RideAnalytics.startOfWeek(now)) }.getOrDefault(0.0)
        val mood = runCatching { PetStore(context).load().mood }.getOrDefault(PetMood.CONTENT)
        return WidgetData(
            todayKm = todayM / 1000.0,
            weekKm = weekM / 1000.0,
            emoji = emojiFor(mood),
            useMiles = SettingsStore(context).useMiles
        )
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int, data: WidgetData) {
        val miles = data.useMiles
        val unit = Units.distUnit(miles)
        val views = RemoteViews(context.packageName, R.layout.widget_speedbike).apply {
            setTextViewText(R.id.widget_pet, data.emoji)
            setTextViewText(R.id.widget_distance, "${Units.fmtCompact(data.todayKm, miles)} $unit")
            setTextViewText(
                R.id.widget_subtitle,
                "сьогодні · ${Units.fmtCompact(data.weekKm, miles)} $unit за тиждень"
            )
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
        manager.updateAppWidget(id, views)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private data class WidgetData(
        val todayKm: Double,
        val weekKm: Double,
        val emoji: String,
        val useMiles: Boolean
    )

    companion object {
        private fun emojiFor(mood: PetMood): String = when (mood) {
            PetMood.EXCITED -> "🤩"
            PetMood.HAPPY -> "😄"
            PetMood.CONTENT -> "🙂"
            PetMood.SAD -> "😢"
            PetMood.HUNGRY -> "🍽️"
            PetMood.SLEEPY -> "😴"
        }

        /** Ask the framework to redraw every placed widget (called after a ride is saved). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SpeedBikeWidget::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, SpeedBikeWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
