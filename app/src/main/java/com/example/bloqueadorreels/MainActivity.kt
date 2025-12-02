package com.example.bloqueadorreels

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.ComponentActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "reels_blocker_prefs"

        private const val KEY_BLOCK_UNTIL = "block_until"
        private const val KEY_RETO_UNTIL = "reto_until"
        private const val KEY_BLOCK_START = "block_start"
        private const val KEY_RETO_START = "reto_start"

        private const val KEY_TODAY_DATE = "today_date"
        private const val KEY_TODAY_BLOCKED_MS = "today_blocked_ms"
        private const val KEY_TODAY_RETO_MS = "today_reto_ms"
        private const val KEY_TOTAL_BLOCKED_MS = "total_blocked_ms"
        private const val KEY_TOTAL_RETO_MS = "total_reto_ms"

        private const val ONE_HOUR = 60L * 60L * 1000L
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val btnRetoDo = findViewById<Button>(R.id.btnRetoDo)
        val btnActivate = findViewById<Button>(R.id.btnActivate)
        val btnDisable = findViewById<Button>(R.id.btnDisable)
        val pbRetoDo = findViewById<ProgressBar>(R.id.pbRetoDo)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupDurations)

        // Navegación
        findViewById<Button>(R.id.btnGoStats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<Button>(R.id.btnGoHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        btnRetoDo.setOnClickListener {
            activateRetoDo()
            updateUI(txtStatus, pbRetoDo)
            openAccessibilitySettings()
        }

        btnActivate.setOnClickListener {
            val duration = getSelectedDuration(radioGroup)
            if (duration == -1L) {
                Toast.makeText(this, "Elegí una duración", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activateBlock(duration)
            updateUI(txtStatus, pbRetoDo)
            openAccessibilitySettings()
        }

        btnDisable.setOnClickListener {
            finalizeSessions()
            prefs.edit()
                .putLong(KEY_BLOCK_UNTIL, 0L)
                .putLong(KEY_RETO_UNTIL, 0L)
                .apply()
            updateUI(txtStatus, pbRetoDo)
            Toast.makeText(this, "Bloqueo desactivado", Toast.LENGTH_SHORT).show()
        }

        updateUI(txtStatus, pbRetoDo)
    }

    private fun getSelectedDuration(group: RadioGroup): Long =
        when (group.checkedRadioButtonId) {
            R.id.rb15Min -> 15L * 60L * 1000L
            R.id.rb30Min -> 30L * 60L * 1000L
            R.id.rb1Hour -> 60L * 60L * 1000L
            R.id.rb2Hours -> 2L * 60L * 60L * 1000L
            R.id.rbIndefinite -> Long.MAX_VALUE
            else -> -1L
        }

    private fun activateBlock(duration: Long) {
        val now = System.currentTimeMillis()
        finalizeBlockSession(now)
        val until = if (duration == Long.MAX_VALUE) Long.MAX_VALUE else now + duration

        prefs.edit()
            .putLong(KEY_BLOCK_UNTIL, until)
            .putLong(KEY_BLOCK_START, now)
            .apply()
        Toast.makeText(this, "Bloqueo activado", Toast.LENGTH_SHORT).show()
    }

    private fun activateRetoDo() {
        val now = System.currentTimeMillis()
        finalizeRetoSession(now)
        prefs.edit()
            .putLong(KEY_RETO_UNTIL, now + ONE_HOUR)
            .putLong(KEY_RETO_START, now)
            .apply()
        Toast.makeText(this, "Reto-Do activado", Toast.LENGTH_SHORT).show()
    }

    private fun resetTodayIfNeeded() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val stored = prefs.getString(KEY_TODAY_DATE, "")
        if (today != stored) {
            prefs.edit()
                .putString(KEY_TODAY_DATE, today)
                .putLong(KEY_TODAY_BLOCKED_MS, 0L)
                .putLong(KEY_TODAY_RETO_MS, 0L)
                .apply()
        }
    }

    private fun finalizeSessions() {
        val now = System.currentTimeMillis()
        finalizeBlockSession(now)
        finalizeRetoSession(now)
    }

    private fun finalizeBlockSession(now: Long) {
        val start = prefs.getLong(KEY_BLOCK_START, 0L)
        val until = prefs.getLong(KEY_BLOCK_UNTIL, 0L)
        if (start == 0L) return
        val end = minOf(now, until)
        val delta = (end - start).coerceAtLeast(0L)
        addTime(delta, false)
        prefs.edit().putLong(KEY_BLOCK_START, 0L).apply()
    }

    private fun finalizeRetoSession(now: Long) {
        val start = prefs.getLong(KEY_RETO_START, 0L)
        val until = prefs.getLong(KEY_RETO_UNTIL, 0L)
        if (start == 0L) return
        val end = minOf(now, until)
        val delta = (end - start).coerceAtLeast(0L)
        addTime(delta, true)
        prefs.edit().putLong(KEY_RETO_START, 0L).apply()
    }

    private fun addTime(ms: Long, reto: Boolean) {
        resetTodayIfNeeded()
        val todayKey = if (reto) KEY_TODAY_RETO_MS else KEY_TODAY_BLOCKED_MS
        val totalKey = if (reto) KEY_TOTAL_RETO_MS else KEY_TOTAL_BLOCKED_MS
        prefs.edit()
            .putLong(todayKey, prefs.getLong(todayKey, 0L) + ms)
            .putLong(totalKey, prefs.getLong(totalKey, 0L) + ms)
            .apply()
    }

    private fun updateUI(txtStatus: TextView, pbRetoDo: ProgressBar) {
        val now = System.currentTimeMillis()
        val blockUntil = prefs.getLong(KEY_BLOCK_UNTIL, 0L)
        val retoUntil = prefs.getLong(KEY_RETO_UNTIL, 0L)
        val blockStart = prefs.getLong(KEY_BLOCK_START, 0L)
        val retoStart = prefs.getLong(KEY_RETO_START, 0L)

        val format = DateFormat.getTimeInstance(DateFormat.SHORT)

        val status = when {
            retoUntil > now -> "Reto-Do activo hasta ${format.format(Date(retoUntil))}"
            blockUntil == Long.MAX_VALUE && blockStart > 0L -> "Bloqueo permanente activo"
            blockUntil > now -> "Bloqueo activo hasta ${format.format(Date(blockUntil))}"
            else -> "Bloqueo desactivado"
        }
        txtStatus.text = status

        // Progreso Reto-Do del día
        resetTodayIfNeeded()
        val todayRetoStored = prefs.getLong(KEY_TODAY_RETO_MS, 0L)
        val retoActiveExtra =
            if (retoUntil > now && retoStart > 0L) (now - retoStart).coerceAtLeast(0L) else 0L
        val todayRetoTotal = todayRetoStored + retoActiveExtra

        pbRetoDo.max = ONE_HOUR.toInt()
        pbRetoDo.progress = todayRetoTotal.coerceAtMost(ONE_HOUR).toInt()
    }

    private fun openAccessibilitySettings() {
        val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }
}
