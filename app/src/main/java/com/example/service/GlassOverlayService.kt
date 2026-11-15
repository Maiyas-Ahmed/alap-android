package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.util.NotificationHelper

/**
 * Displays an iPhone-style translucent frosted-glass floating notification banner
 * above other apps/games when the user explicitly opts in and grants
 * SYSTEM_ALERT_WINDOW ("Display over other apps").
 * If overlay permission is not granted, Alap seamlessly falls back to Android's
 * Play-compliant high-priority Heads-Up Notification channel.
 */
class GlassOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var activeOverlayView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { removeActiveOverlay() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildOverlayForegroundNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }

        val action = intent?.action
        if (action == ACTION_STOP) {
            removeActiveOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_SHOW_POPUP) {
            val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
            val badge = intent.getStringExtra(EXTRA_BADGE).orEmpty().ifBlank { "Alap" }
            val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)

            if (title.isNotBlank() && Settings.canDrawOverlays(this)) {
                showGlassPopup(title = title, body = body, badge = badge, chatId = chatId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeActiveOverlay()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

    private fun showGlassPopup(
        title: String,
        body: String,
        badge: String,
        chatId: String?
    ) {
        removeActiveOverlay()
        val wm = windowManager ?: return

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                // Translucent frosted white glass tint
                setColor(Color.argb(228, 249, 251, 253))
                setStroke(dp(1), Color.argb(190, 210, 224, 220))
            }
            elevation = dp(16).toFloat()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(null)
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badgePill = TextView(this).apply {
            text = badge.uppercase()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#008069"))
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.argb(38, 0, 128, 105))
            }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val dismissBtn = TextView(this).apply {
            text = "Close"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#54656F"))
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener { removeActiveOverlay() }
        }

        headerRow.addView(badgePill)
        headerRow.addView(spacer)
        headerRow.addView(dismissBtn)

        val titleView = TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#111B21"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, dp(2))
        }

        val bodyView = TextView(this).apply {
            text = body
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTextColor(Color.parseColor("#3B4A54"))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        val openHint = TextView(this).apply {
            text = "Tap card to open in Alap"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#008069"))
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(headerRow)
        card.addView(titleView)
        card.addView(bodyView)
        card.addView(openHint)

        card.setOnClickListener {
            removeActiveOverlay()
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (!chatId.isNullOrBlank()) putExtra(NotificationHelper.EXTRA_CHAT_ID, chatId)
            }
            startActivity(launchIntent)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels - dp(24)).coerceAtMost(dp(440)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(40)
        }

        try {
            wm.addView(card, params)
            activeOverlayView = card
            card.alpha = 0f
            card.translationY = -dp(24).toFloat()
            card.animate().alpha(1f).translationY(0f).setDuration(240).start()
            mainHandler.postDelayed(dismissRunnable, 5500L)
        } catch (_: Exception) {
            activeOverlayView = null
        }
    }

    private fun removeActiveOverlay() {
        mainHandler.removeCallbacks(dismissRunnable)
        val view = activeOverlayView ?: return
        activeOverlayView = null
        try {
            windowManager?.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val FOREGROUND_ID = 7701
        const val ACTION_START = "com.example.service.OVERLAY_START"
        const val ACTION_STOP = "com.example.service.OVERLAY_STOP"
        const val ACTION_SHOW_POPUP = "com.example.service.OVERLAY_SHOW_POPUP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_BADGE = "extra_badge"

        fun startServiceIfPermitted(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, GlassOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun showPopup(
            context: Context,
            title: String,
            body: String,
            badge: String = "New Message",
            chatId: String? = null
        ) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, GlassOverlayService::class.java).apply {
                action = ACTION_SHOW_POPUP
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_BADGE, badge)
                if (!chatId.isNullOrBlank()) putExtra(NotificationHelper.EXTRA_CHAT_ID, chatId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, GlassOverlayService::class.java))
        }
    }
}
