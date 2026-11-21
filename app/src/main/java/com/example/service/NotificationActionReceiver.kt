package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.data.firestore.AlapFirestoreProvider
import com.example.data.model.Chat
import com.example.data.repository.AlapRepository
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles WhatsApp-style inline notification actions (Direct Reply & Mark as Read)
 * directly from the Heads-Up banner or Lock Screen without opening the app.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID).orEmpty()
        if (chatId.isBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val user = Firebase.auth.currentUser ?: return@launch
                val uid = user.uid
                val db = AlapFirestoreProvider.getDatabase(appContext)
                val repo = AlapRepository(db)

                when (action) {
                    NotificationHelper.ACTION_MARK_READ -> {
                        val chatSnap = db.collection("chats").document(chatId).get().await()
                        val chat = chatSnap.toObject(
                            Chat::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (chat != null) {
                            val updatedUnread = chat.unreadCounts.toMutableMap()
                            updatedUnread[uid] = 0L
                            db.collection("chats").document(chatId).update(
                                mapOf(
                                    "unreadCounts" to updatedUnread,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                        NotificationManagerCompat.from(appContext).cancel(chatId.hashCode())
                    }

                    NotificationHelper.ACTION_DIRECT_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                        if (replyText.isBlank()) return@launch

                        val chatSnap = db.collection("chats").document(chatId).get().await()
                        val chat = chatSnap.toObject(
                            Chat::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: return@launch

                        val userSnap = db.collection("users").document(uid).get().await()
                        val senderName = userSnap.getString("displayName")
                            ?.takeIf { it.isNotBlank() }
                            ?: user.displayName
                            ?: "Alap User"

                        val result = repo.sendMessage(
                            chat = chat,
                            senderName = senderName,
                            text = replyText
                        )
                        if (result.isSuccess) {
                            NotificationManagerCompat.from(appContext).cancel(chatId.hashCode())
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep receiver crash-safe
            } finally {
                pendingResult.finish()
            }
        }
    }
}
