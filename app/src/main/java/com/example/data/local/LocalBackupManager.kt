package com.example.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.local.room.AlapLocalDao
import com.example.data.local.room.LocalChatEntity
import com.example.data.local.room.LocalMessageEntity
import com.example.data.local.room.LocalStudyItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupRestoreSummary(
    val chatCount: Int,
    val messageCount: Int,
    val exportedAtEpochMs: Long
)

class LocalBackupManager(
    private val context: Context,
    private val dao: AlapLocalDao
) {
    suspend fun buildBackupJson(ownerUid: String): String = withContext(Dispatchers.IO) {
        val chats = dao.getAllChatsSnapshot()
        val messages = dao.getAllMessagesSnapshot()
        val studyItems = dao.getAllStudyItemsSnapshot()

        val root = JSONObject().apply {
            put("app", "Alap")
            put("schemaVersion", 2)
            put("ownerUid", ownerUid)
            put("exportedAtEpochMs", System.currentTimeMillis())

            val chatArr = JSONArray()
            chats.forEach { c ->
                chatArr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("chatType", c.chatType)
                        put("title", c.title)
                        put("description", c.description)
                        put("avatarId", c.avatarId)
                        put("participantUidsJson", c.participantUidsJson)
                        put("adminUidsJson", c.adminUidsJson)
                        put("createdBy", c.createdBy)
                        put("lastMessageText", c.lastMessageText)
                        put("lastMessageSenderId", c.lastMessageSenderId)
                        put("lastMessageType", c.lastMessageType)
                        put("lastMessageAtEpochMs", c.lastMessageAtEpochMs)
                        put("disappearingHours", c.disappearingHours)
                        put("isStudyGroup", c.isStudyGroup)
                        put("studyLockToAdmin", c.studyLockToAdmin)
                        put("inviteCode", c.inviteCode)
                        put("pinnedByUidsJson", c.pinnedByUidsJson)
                        put("mutedByUidsJson", c.mutedByUidsJson)
                        put("archivedByUidsJson", c.archivedByUidsJson)
                        put("unreadCountsJson", c.unreadCountsJson)
                        put("createdAtEpochMs", c.createdAtEpochMs)
                        put("updatedAtEpochMs", c.updatedAtEpochMs)
                    }
                )
            }
            put("chats", chatArr)

            val msgArr = JSONArray()
            messages.forEach { m ->
                msgArr.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("chatId", m.chatId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("text", m.text)
                        put("messageType", m.messageType)
                        put("stickerId", m.stickerId ?: JSONObject.NULL)
                        put("pollQuestion", m.pollQuestion ?: JSONObject.NULL)
                        put("pollOptionsJson", m.pollOptionsJson ?: JSONObject.NULL)
                        put("pollVotesJson", m.pollVotesJson ?: JSONObject.NULL)
                        put("correctOptionIndex", m.correctOptionIndex ?: JSONObject.NULL)
                        put("quizExplanation", m.quizExplanation ?: JSONObject.NULL)
                        put("flashcardFront", m.flashcardFront ?: JSONObject.NULL)
                        put("flashcardBack", m.flashcardBack ?: JSONObject.NULL)
                        put("replyToMessageId", m.replyToMessageId ?: JSONObject.NULL)
                        put("replyToSenderName", m.replyToSenderName ?: JSONObject.NULL)
                        put("replyToPreview", m.replyToPreview ?: JSONObject.NULL)
                        put("isForwarded", m.isForwarded)
                        put("isEdited", m.isEdited)
                        put("isDeleted", m.isDeleted)
                        put("participantUidsJson", m.participantUidsJson)
                        put("readByUidsJson", m.readByUidsJson)
                        put("starredByUidsJson", m.starredByUidsJson)
                        put("reactionsJson", m.reactionsJson)
                        put("createdAtEpochMs", m.createdAtEpochMs)
                        put("updatedAtEpochMs", m.updatedAtEpochMs)
                    }
                )
            }
            put("messages", msgArr)

            val studyArr = JSONArray()
            studyItems.forEach { s ->
                studyArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("chatId", s.chatId)
                        put("itemType", s.itemType)
                        put("title", s.title)
                        put("details", s.details)
                        put("scheduleText", s.scheduleText)
                        put("examDateEpochMs", s.examDateEpochMs)
                        put("createdBy", s.createdBy)
                        put("createdByName", s.createdByName)
                        put("participantUidsJson", s.participantUidsJson)
                        put("createdAtEpochMs", s.createdAtEpochMs)
                        put("updatedAtEpochMs", s.updatedAtEpochMs)
                    }
                )
            }
            put("studyItems", studyArr)
        }
        root.toString(2)
    }

    suspend fun exportToUri(uri: Uri, ownerUid: String): Result<BackupRestoreSummary> = runCatching {
        val json = buildBackupJson(ownerUid)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: throw IllegalStateException("Unable to open destination file")
        }
        val obj = JSONObject(json)
        BackupRestoreSummary(
            chatCount = obj.getJSONArray("chats").length(),
            messageCount = obj.getJSONArray("messages").length(),
            exportedAtEpochMs = obj.getLong("exportedAtEpochMs")
        )
    }

    suspend fun saveAutoSnapshot(ownerUid: String): Result<File> = runCatching {
        val json = buildBackupJson(ownerUid)
        withContext(Dispatchers.IO) {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val backupDir = File(baseDir, "AlapBackup").apply { mkdirs() }
            val outFile = File(backupDir, "alap_local_backup.json")
            outFile.writeText(json, Charsets.UTF_8)
            outFile
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<BackupRestoreSummary> = runCatching {
        val json = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: throw IllegalStateException("Unable to read backup file")
        }
        restoreFromJsonString(json)
    }

    suspend fun restoreFromAutoSnapshot(): Result<BackupRestoreSummary> = runCatching {
        val file = withContext(Dispatchers.IO) {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            File(File(baseDir, "AlapBackup"), "alap_local_backup.json")
        }
        require(file.exists()) { "No automatic local snapshot found on this device yet." }
        restoreFromJsonString(file.readText(Charsets.UTF_8))
    }

    private suspend fun restoreFromJsonString(json: String): BackupRestoreSummary {
        val root = JSONObject(json)
        require(root.optString("app") == "Alap") { "Invalid Alap backup file." }

        val chatArr = root.optJSONArray("chats") ?: JSONArray()
        val chats = mutableListOf<LocalChatEntity>()
        for (i in 0 until chatArr.length()) {
            val c = chatArr.getJSONObject(i)
            chats.add(
                LocalChatEntity(
                    id = c.getString("id"),
                    chatType = c.optString("chatType", "direct"),
                    title = c.optString("title", ""),
                    description = c.optString("description", ""),
                    avatarId = c.optString("avatarId", "avatar_emerald"),
                    participantUidsJson = c.optString("participantUidsJson", "[]"),
                    adminUidsJson = c.optString("adminUidsJson", "[]"),
                    createdBy = c.optString("createdBy", ""),
                    lastMessageText = c.optString("lastMessageText", ""),
                    lastMessageSenderId = c.optString("lastMessageSenderId", ""),
                    lastMessageType = c.optString("lastMessageType", "text"),
                    lastMessageAtEpochMs = c.optLong("lastMessageAtEpochMs", 0L),
                    disappearingHours = c.optLong("disappearingHours", 0L),
                    isStudyGroup = c.optBoolean("isStudyGroup", false),
                    studyLockToAdmin = c.optBoolean("studyLockToAdmin", false),
                    inviteCode = c.optString("inviteCode", ""),
                    pinnedByUidsJson = c.optString("pinnedByUidsJson", "[]"),
                    mutedByUidsJson = c.optString("mutedByUidsJson", "[]"),
                    archivedByUidsJson = c.optString("archivedByUidsJson", "[]"),
                    unreadCountsJson = c.optString("unreadCountsJson", "{}"),
                    createdAtEpochMs = c.optLong("createdAtEpochMs", 0L),
                    updatedAtEpochMs = c.optLong("updatedAtEpochMs", 0L)
                )
            )
        }

        val msgArr = root.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<LocalMessageEntity>()
        for (i in 0 until msgArr.length()) {
            val m = msgArr.getJSONObject(i)
            fun optNullable(key: String): String? =
                if (m.isNull(key)) null else m.optString(key).takeIf { it.isNotEmpty() }

            val correctIdx = if (m.isNull("correctOptionIndex") || !m.has("correctOptionIndex")) {
                null
            } else {
                m.optLong("correctOptionIndex")
            }

            messages.add(
                LocalMessageEntity(
                    id = m.getString("id"),
                    chatId = m.getString("chatId"),
                    senderId = m.getString("senderId"),
                    senderName = m.optString("senderName", "Alap User"),
                    text = m.optString("text", ""),
                    messageType = m.optString("messageType", "text"),
                    stickerId = optNullable("stickerId"),
                    pollQuestion = optNullable("pollQuestion"),
                    pollOptionsJson = optNullable("pollOptionsJson"),
                    pollVotesJson = optNullable("pollVotesJson"),
                    correctOptionIndex = correctIdx,
                    quizExplanation = optNullable("quizExplanation"),
                    flashcardFront = optNullable("flashcardFront"),
                    flashcardBack = optNullable("flashcardBack"),
                    replyToMessageId = optNullable("replyToMessageId"),
                    replyToSenderName = optNullable("replyToSenderName"),
                    replyToPreview = optNullable("replyToPreview"),
                    isForwarded = m.optBoolean("isForwarded", false),
                    isEdited = m.optBoolean("isEdited", false),
                    isDeleted = m.optBoolean("isDeleted", false),
                    participantUidsJson = m.optString("participantUidsJson", "[]"),
                    readByUidsJson = m.optString("readByUidsJson", "[]"),
                    starredByUidsJson = m.optString("starredByUidsJson", "[]"),
                    reactionsJson = m.optString("reactionsJson", "{}"),
                    createdAtEpochMs = m.optLong("createdAtEpochMs", 0L),
                    updatedAtEpochMs = m.optLong("updatedAtEpochMs", 0L)
                )
            )
        }

        val studyArr = root.optJSONArray("studyItems") ?: JSONArray()
        val studyItems = mutableListOf<LocalStudyItemEntity>()
        for (i in 0 until studyArr.length()) {
            val s = studyArr.getJSONObject(i)
            studyItems.add(
                LocalStudyItemEntity(
                    id = s.getString("id"),
                    chatId = s.getString("chatId"),
                    itemType = s.optString("itemType", "note"),
                    title = s.optString("title", ""),
                    details = s.optString("details", ""),
                    scheduleText = s.optString("scheduleText", ""),
                    examDateEpochMs = s.optLong("examDateEpochMs", 0L),
                    createdBy = s.optString("createdBy", ""),
                    createdByName = s.optString("createdByName", "Student"),
                    participantUidsJson = s.optString("participantUidsJson", "[]"),
                    createdAtEpochMs = s.optLong("createdAtEpochMs", 0L),
                    updatedAtEpochMs = s.optLong("updatedAtEpochMs", 0L)
                )
            )
        }

        if (chats.isNotEmpty()) dao.upsertChats(chats)
        if (messages.isNotEmpty()) dao.upsertMessages(messages)
        if (studyItems.isNotEmpty()) dao.upsertStudyItems(studyItems)

        return BackupRestoreSummary(
            chatCount = chats.size,
            messageCount = messages.size,
            exportedAtEpochMs = root.optLong("exportedAtEpochMs", System.currentTimeMillis())
        )
    }
}
