package com.example.bloqueadorreels

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PhoneUsageUtils {

    /**
     * Devuelve los minutos de uso del teléfono en el día de hoy.
     * Si no hay permiso de acceso a uso, devuelve 0.
     */
    fun getTodayScreenTimeMinutes(context: Context): Int {
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        return getUsageMinutesBetween(context, startOfDay, now)
    }

    /**
     * Devuelve los minutos de uso de los últimos 7 días (incluyendo hoy).
     */
    fun getLast7DaysScreenTimeMinutes(context: Context): Int {
        val now = System.currentTimeMillis()

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6) // hoy + 6 días hacia atrás = 7 días
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        return getUsageMinutesBetween(context, start, now)
    }

    /**
     * Suma el tiempo en primer plano de todas las apps en el rango dado.
     */
    private fun getUsageMinutesBetween(
        context: Context,
        startTime: Long,
        endTime: Long
    ): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val stats: List<UsageStats> =
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                ?: return 0

        if (stats.isEmpty()) return 0

        var totalMillis = 0L
        for (s in stats) {
            totalMillis += s.totalTimeInForeground
        }

        return TimeUnit.MILLISECONDS.toMinutes(totalMillis).toInt()
    }
}
