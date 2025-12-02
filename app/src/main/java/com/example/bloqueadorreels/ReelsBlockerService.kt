package com.example.bloqueadorreels

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelsBlockerService"

        private const val PREFS_NAME = "reels_blocker_prefs"
        private const val KEY_BLOCK_UNTIL = "block_until"
        private const val KEY_RETO_DO_UNTIL = "reto_do_until"
        private const val INDEFINITE = -1L

        // Paquetes de redes a bloquear en Reto-Do
        private val SOCIAL_APPS = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.twitter.android",
            "com.x.android",               // por si cambia a X
            "com.facebook.katana",
            "com.zhiliaoapp.musically",    // TikTok
            "com.snapchat.android",
            "com.reddit.frontpage"
        )

        // Whitelist: apps de mensajería que NO bloqueamos
        private val MESSAGING_APPS = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.thoughtcrime.securesms",  // Signal
            "com.discord"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Escuchamos varias apps; algunas se usan solo para Reto-Do
            packageNames = (SOCIAL_APPS + MESSAGING_APPS).toTypedArray()

            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        serviceInfo = info
        Log.d(TAG, "Servicio iniciado correctamente")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        val retoActive = isRetoDoActive()
        val reelsBlockActive = isReelsBlockActive()

        // Si nada está activo, no tocamos nada
        if (!retoActive && !reelsBlockActive) {
            return
        }

        // ===========================
        //  MODO RETO-DO (1h sin redes)
        // ===========================
        if (retoActive) {
            // Si es una app de mensajería permitida, no la tocamos
            if (MESSAGING_APPS.contains(pkg)) {
                return
            }

            // Si es una red social del listado, la mandamos a Home
            if (SOCIAL_APPS.contains(pkg)) {
                Log.d(TAG, "RETO-DO: bloqueando app $pkg -> HOME")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }

        // ===========================
        //  MODO SOLO BLOQUEO DE REELS
        // ===========================
        if (reelsBlockActive) {
            when (pkg) {
                "com.instagram.android" -> {
                    if (isInstagramReel(root)) {
                        Log.d(TAG, "INSTAGRAM: Reel detectado -> BACK")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }

                "com.google.android.youtube" -> {
                    if (isYouTubeShort(root)) {
                        Log.d(TAG, "YOUTUBE: Short detectado -> BACK")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    // ========================
    //   ESTADO DE BLOQUEOS
    // ========================
    private fun isReelsBlockActive(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val blockUntil = prefs.getLong(KEY_BLOCK_UNTIL, 0L)
        val now = System.currentTimeMillis()

        return when {
            blockUntil == INDEFINITE -> true
            blockUntil > now -> true
            else -> false
        }
    }

    private fun isRetoDoActive(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val retoUntil = prefs.getLong(KEY_RETO_DO_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return retoUntil > now
    }

    // =====================================================
    //   INSTAGRAM — igual que tu versión
    // =====================================================
    private fun isInstagramReel(root: AccessibilityNodeInfo): Boolean {
        val gestureManager = find(root, "com.instagram.android:id/gesture_manager")
        val clipsPager = find(root, "com.instagram.android:id/clips_viewer_view_pager")
        return (gestureManager != null && clipsPager != null)
    }

    // =====================================================
    //   YOUTUBE — igual que tu versión
    // =====================================================
    private fun isYouTubeShort(root: AccessibilityNodeInfo): Boolean {
        val reelContainer = find(root, "com.google.android.youtube:id/reel_player_page_container")
        val reelProgress = find(root, "com.google.android.youtube:id/reel_progress_bar")
        return (reelContainer != null && reelProgress != null)
    }

    // =====================================================
    // Utilidad de búsqueda
    // =====================================================
    private fun find(root: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (root == null) return null
        return try {
            val list = root.findAccessibilityNodeInfosByViewId(id)
            if (list.isNullOrEmpty()) null else list[0]
        } catch (e: Exception) {
            null
        }
    }
}
