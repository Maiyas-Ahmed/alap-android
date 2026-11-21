package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.service.NotificationActionReceiver

object NotificationHelper {
    const val CHANNEL_ID = "alap_announcements_v2"
    private const val CHANNEL_NAME = "Alap Updates & Announcements"
    const val CHANNEL_MESSAGES_ID = "alap_messages_v2"
    private const val CHANNEL_MESSAGES_NAME = "Alap Incoming Messages"
    private const val CHANNEL_OVERLAY_ID = "alap_overlay_service"
    private const val CHANNEL_OVERLAY_NAME = "Alap Floating Glass Popups"

    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val ACTION_DIRECT_REPLY = "com.example.action.DIRECT_REPLY"
    const val ACTION_MARK_READ = "com.example.action.MARK_READ"
    const val KEY_TEXT_REPLY = "key_text_reply"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultSound = Settings.System.DEFAULT_NOTIFICATION_URI
            val commAudio = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()
            val generalAudio = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val annChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up & lock screen alerts for official Alap updates and admin announcements"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 220L, 120L, 220L)
                enableLights(true)
                lightColor = Color.GREEN
                setShowBadge(true)
                setSound(defaultSound, generalAudio)
            }

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "WhatsApp-style heads-up banners & lock screen message alerts"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 200L, 100L, 200L)
                enableLights(true)
                lightColor = Color.GREEN
                setShowBadge(true)
                setSound(defaultSound, commAudio)
            }

            val overlayChannel = NotificationChannel(
                CHANNEL_OVERLAY_ID,
                CHANNEL_OVERLAY_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps opt-in floating glass popups active over other apps"
                setShowBadge(false)
            }

            manager.createNotificationChannel(annChannel)
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(overlayChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isDeviceLockedOrScreenOff(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val locked = km?.isKeyguardLocked == true
        val interactive = pm?.isInteractive ?: true
        return locked || !interactive
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openMessageChannelSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel(context)
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_MESSAGES_ID)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WakelockTimeout")
    fun wakeScreenIfOff(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (!pm.isInteractive) {
                val wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                    "Alap:NotifWakeLock"
                )
                wakeLock.acquire(4500L)
            }
        } catch (_: Exception) {
            // Safe fallback if OEM restricts wake lock
        }
    }

    fun showAnnouncementNotification(
        context: Context,
        announcementId: String,
        title: String,
        message: String,
        versionTag: String,
        wakeScreen: Boolean = true
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)
        if (wakeScreen) wakeScreenIfOff(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            announcementId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = if (versionTag.isNotBlank()) "$title ($versionTag)" else title
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(announcementId.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showIncomingMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        preview: String,
        isGroup: Boolean = false,
        conversationTitle: String = senderName,
        wakeScreen: Boolean = true,
        includeInlineActions: Boolean = true
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)
        if (wakeScreen) wakeScreenIfOff(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val senderPerson = Person.Builder().setName(senderName).build()
        val mePerson = Person.Builder().setName("You").build()
        val style = NotificationCompat.MessagingStyle(mePerson)
        style.isGroupConversation = isGroup
        if (isGroup && conversationTitle.isNotBlank()) {
            style.conversationTitle = conversationTitle
        }
        style.addMessage(preview, System.currentTimeMillis(), senderPerson)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isGroup) "$senderName • $conversationTitle" else senderName)
            .setContentText(preview)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
            .setGroup("alap_messages_group")
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (includeInlineActions) {
            val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_DIRECT_REPLY
                putExtra(EXTRA_CHAT_ID, chatId)
            }
            val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                ("reply_$chatId").hashCode(),
                replyIntent,
                replyFlags
            )
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Reply")
                .build()
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Reply",
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_MARK_READ
                putExtra(EXTRA_CHAT_ID, chatId)
            }
            val markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                ("read_$chatId").hashCode(),
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val markReadAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "Mark as read",
                markReadPendingIntent
            ).build()

            builder.addAction(replyAction)
            builder.addAction(markReadAction)
        }

        return try {
            NotificationManagerCompat.from(context).notify(chatId.hashCode(), builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendTestWhatsAppNotification(context: Context, wakeScreen: Boolean): Boolean {
        return showIncomingMessageNotification(
            context = context,
            chatId = "alap_test_preview",
            senderName = "Alap Messaging",
            preview = "WhatsApp-style Heads-Up & Lock Screen alert is working!",
            isGroup = false,
            wakeScreen = wakeScreen,
            includeInlineActions = true
        )
    }

    fun showOtpNotification(
        context: Context,
        email: String,
        code: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "Your Alap Gmail verification code for $email is: $code (valid for 10 minutes)."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alap Verification Code: $code")
            .setContentText("Code for $email")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9001, notification)
        } catch (_: SecurityException) {
            // Permission revoked at OS level
        }
    }

    fun buildOverlayForegroundNotification(context: Context): Notification {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_OVERLAY_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alap Floating Glass Popups Active")
            .setContentText("New messages will appear in a glass popup over other apps.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
