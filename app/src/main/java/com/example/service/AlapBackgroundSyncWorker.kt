package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.firestore.AlapFirestoreProvider
import com.example.data.local.LocalPreferencesManager
import com.example.data.model.Announcement
import com.example.data.model.Chat
import com.example.util.NotificationHelper
import com.example.util.WebsiteUpdateChecker
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager fallback that checks active announcements and unread chats
 * in the background and posts WhatsApp-style lock-screen & heads-up notifications.
 */
class AlapBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUser = Firebase.auth.currentUser ?: return Result.success()
        val uid = currentUser.uid
        val prefs = LocalPreferencesManager(applicationContext)
        val notificationsEnabled = prefs.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()
        val wakeScreen = prefs.wakeScreenOnNotification.first()

        return try {
            val db = AlapFirestoreProvider.getDatabase(applicationContext)

            // 1. Check latest active announcement
            val annSnap = db.collection("announcements")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val topAnnouncement = annSnap.toObjects(Announcement::class.java)
                .filter { WebsiteUpdateChecker.shouldShowAnnouncement(it) }
                .maxByOrNull { it.createdAt ?: Timestamp(0, 0) }
            val lastNotifiedAnn = prefs.lastNotifiedAnnouncementId.first()
            if (topAnnouncement != null && topAnnouncement.id.isNotBlank() && topAnnouncement.id != lastNotifiedAnn) {
                val posted = NotificationHelper.showAnnouncementNotification(
                    context = applicationContext,
                    announcementId = topAnnouncement.id,
                    title = topAnnouncement.title,
                    message = topAnnouncement.message,
                    versionTag = topAnnouncement.versionTag,
                    wakeScreen = wakeScreen
                )
                if (posted) {
                    prefs.markAnnouncementNotified(topAnnouncement.id)
                }
            }

            // 2. Check unread chats since last background sync
            val chatsSnap = db.collection("chats")
                .whereArrayContains("participantUids", uid)
                .get()
                .await()
            val chats = chatsSnap.toObjects(Chat::class.java)
            val lastBgCheckMs = prefs.lastBgSyncEpochMs.first()
            var maxSeenMs = lastBgCheckMs

            chats.forEach { chat ->
                val msgAtMs = chat.lastMessageAt?.toDate()?.time ?: 0L
                if (msgAtMs > lastBgCheckMs &&
                    chat.unreadCountFor(uid) > 0 &&
                    chat.lastMessageSenderId != uid &&
                    chat.lastMessageSenderId.isNotBlank() &&
                    !chat.isMutedBy(uid)
                ) {
                    val isGroup = chat.chatType == "group"
                    val senderTitle = if (isGroup) {
                        chat.title.ifBlank { "Group Chat" }
                    } else {
                        try {
                            val senderDoc = db.collection("users").document(chat.lastMessageSenderId).get().await()
                            senderDoc.getString("displayName")?.takeIf { it.isNotBlank() } ?: "Alap"
                        } catch (_: Exception) {
                            "Alap"
                        }
                    }
                    NotificationHelper.showIncomingMessageNotification(
                        context = applicationContext,
                        chatId = chat.id,
                        senderName = senderTitle,
                        preview = chat.lastMessageText.ifBlank { "You have an unread message" },
                        isGroup = isGroup,
                        conversationTitle = chat.title.ifBlank { senderTitle },
                        wakeScreen = wakeScreen,
                        includeInlineActions = true
                    )
                }
                if (msgAtMs > maxSeenMs) maxSeenMs = msgAtMs
            }

            prefs.setLastBgSyncEpochMs(maxSeenMs.coerceAtLeast(System.currentTimeMillis()))
            Result.success()
        } catch (e: Exception) {
            Log.w("AlapBgSync", "Background sync skipped/retryable", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "alap_background_notification_sync"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = PeriodicWorkRequestBuilder<AlapBackgroundSyncWorker>(
                    15,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                Log.w("AlapBgSync", "WorkManager schedule deferred", e)
            }
        }
    }
}
