package com.example.bloqueadorreels

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.min

class StatsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        // Views que SÍ existen en activity_stats.xml
        val txtDailyUsage = findViewById<TextView>(R.id.txtDailyUsage)
        val txtWeeklyUsage = findViewById<TextView>(R.id.txtWeeklyUsage)
        val txtRetoProgress = findViewById<TextView>(R.id.txtRetoProgress)

        val pbDailyUsage = findViewById<ProgressBar>(R.id.pbDailyUsage)
        val pbRetoDoStats = findViewById<ProgressBar>(R.id.pbRetoDoStats)

        // ====== Estadísticas de uso del teléfono ======
        val todayMinutes = PhoneUsageUtils.getTodayScreenTimeMinutes(this)
        val weekMinutes = PhoneUsageUtils.getLast7DaysScreenTimeMinutes(this)

        txtDailyUsage.text = "Uso diario del teléfono: ${formatMinutes(todayMinutes)}"
        txtWeeklyUsage.text = "Uso semanal (últimos 7 días): ${formatMinutes(weekMinutes)}"

        pbDailyUsage.max = 240
        pbDailyUsage.progress = min(todayMinutes, 240)

        // ====== Progreso acumulado del Reto-Do ======
        val prefs = getSharedPreferences("reels_prefs", MODE_PRIVATE)
        val retoTotalMinutes = prefs.getLong("reto_do_total_minutes", 0L).toInt()

        txtRetoProgress.text = "Horas acumuladas en Reto-Do: ${formatMinutes(retoTotalMinutes)}"

        pbRetoDoStats.max = 300
        pbRetoDoStats.progress = min(retoTotalMinutes, 300)
    }

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m} min"
    }
}
