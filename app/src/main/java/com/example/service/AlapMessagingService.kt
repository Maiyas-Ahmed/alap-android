package com.example.service

import android.util.Log
import com.example.data.firestore.AlapFirestoreProvider
import com.example.data.local.LocalPreferencesManager
import com.example.data.model.Announcement
import com.example.data.repository.AlapRepository
import com.example.util.NotificationHelper
import com.example.util.WebsiteUpdateChecker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles high-priority data-only FCM push notifications even when Alap is completely closed/killed.
 * Automatically routes to Lock-Screen Public WhatsApp-style banners when locked, and
 * Heads-Up + optional Floating Glass Popup when unlocked.
 */
class AlapMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AlapFCM", "Refreshed FCM token received")
        FirebaseMessaging.getInstance().subscribeToTopic("alap_updates")
        if (Firebase.auth.currentUser != null) {
            serviceScope.launch {
                try {
                    val repo = AlapRepository(AlapFirestoreProvider.getDatabase(applicationContext))
                    repo.saveDevicePushToken(token)
                } catch (e: Exception) {
                    Log.w("AlapFCM", "Failed saving refreshed FCM token", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notif = remoteMessage.notification

        val type = data["type"] ?: "message"
        val title = data["title"] ?: data["senderName"] ?: notif?.title ?: "Alap"
        val body = data["body"] ?: notif?.body ?: "You have a new notification"
        val versionTag = data["versionTag"] ?: ""
        val chatId = data["chatId"] ?: ""
        val isGroup = data["isGroup"]?.equals("true", ignoreCase = true) == true
        val conversationTitle = data["conversationTitle"] ?: title
        val announcementId = data["announcementId"] ?: "fcm_${System.currentTimeMillis()}"

        serviceScope.launch {
            if ((type == "announcement" || type == "update") &&
                !WebsiteUpdateChecker.shouldShowAnnouncement(
                    Announcement(
                        id = announcementId,
                        title = title,
                        message = body,
                        versionTag = versionTag,
                        priority = type
                    )
                )
            ) {
                return@launch
            }

            val prefs = LocalPreferencesManager(applicationContext)
            val notificationsEnabled = prefs.notificationsEnabled.first()
            val floatingGlassEnabled = prefs.floatingGlassEnabled.first()
            val wakeScreen = prefs.wakeScreenOnNotification.first()
            val isLockedOrOff = NotificationHelper.isDeviceLockedOrScreenOff(applicationContext)

            // On Android 8+, SYSTEM_ALERT_WINDOW cannot render over a secure Lock Screen;
            // only show floating overlay when device is unlocked & permitted.
            if (!isLockedOrOff &&
                floatingGlassEnabled &&
                NotificationHelper.hasOverlayPermission(applicationContext)
            ) {
                try {
                    GlassOverlayService.showPopup(
                        context = applicationContext,
                        title = title,
                        body = body,
                        badge = if (type == "announcement" || type == "update") {
                            versionTag.ifBlank { "Alap Update" }
                        } else {
                            "Alap Message"
                        },
                        chatId = chatId.takeIf { it.isNotBlank() }
                    )
                } catch (e: Exception) {
                    Log.w("AlapFCM", "Overlay start restricted in background; falling back to system notification", e)
                }
            }

            if (notificationsEnabled) {
                if (type == "announcement" || type == "update") {
                    NotificationHelper.showAnnouncementNotification(
                        context = applicationContext,
                        announcementId = announcementId,
                        title = title,
                        message = body,
                        versionTag = versionTag,
                        wakeScreen = wakeScreen
                    )
                } else {
                    NotificationHelper.showIncomingMessageNotification(
                        context = applicationContext,
                        chatId = chatId.ifBlank { "alap_general" },
                        senderName = title,
                        preview = body,
                        isGroup = isGroup,
                        conversationTitle = conversationTitle,
                        wakeScreen = wakeScreen,
                        includeInlineActions = chatId.isNotBlank()
                    )
                }
            }
        }
    }
}
