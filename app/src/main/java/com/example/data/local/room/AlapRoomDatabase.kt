package com.example.data.local.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.UiMessage
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

@Entity(tableName = "local_messages")
data class LocalMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val messageType: String,
    val stickerId: String?,
    val pollQuestion: String?,
    val pollOptionsJson: String?,
    val pollVotesJson: String?,
    val replyToMessageId: String?,
    val replyToSenderName: String?,
    val replyToPreview: String?,
    val isForwarded: Boolean,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val participantUidsJson: String,
    val readByUidsJson: String,
    val starredByUidsJson: String,
    val reactionsJson: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "local_chats")
data class LocalChatEntity(
    @PrimaryKey val id: String,
    val chatType: String,
    val title: String,
    val description: String,
    val avatarId: String,
    val participantUidsJson: String,
    val adminUidsJson: String,
    val createdBy: String,
    val lastMessageText: String,
    val lastMessageSenderId: String,
    val lastMessageType: String,
    val lastMessageAtEpochMs: Long,
    val disappearingHours: Long,
    val pinnedByUidsJson: String,
    val mutedByUidsJson: String,
    val archivedByUidsJson: String,
    val unreadCountsJson: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Dao
interface AlapLocalDao {
    @Query("SELECT * FROM local_messages WHERE chatId = :chatId ORDER BY createdAtEpochMs ASC")
    fun observeMessagesForChat(chatId: String): Flow<List<LocalMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<LocalMessageEntity>)

    @Query("SELECT * FROM local_messages ORDER BY createdAtEpochMs ASC")
    suspend fun getAllMessagesSnapshot(): List<LocalMessageEntity>

    @Query("SELECT COUNT(*) FROM local_messages")
    fun observeMessageCount(): Flow<Int>

    @Query("SELECT * FROM local_chats ORDER BY lastMessageAtEpochMs DESC")
    fun observeChats(): Flow<List<LocalChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChats(chats: List<LocalChatEntity>)

    @Query("SELECT * FROM local_chats ORDER BY lastMessageAtEpochMs DESC")
    suspend fun getAllChatsSnapshot(): List<LocalChatEntity>

    @Query("DELETE FROM local_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("DELETE FROM local_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM local_chats")
    suspend fun clearAllChats()
}

@Database(
    entities = [LocalMessageEntity::class, LocalChatEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlapRoomDatabase : RoomDatabase() {
    abstract fun alapLocalDao(): AlapLocalDao

    companion object {
        private const val DB_NAME = "alap_local.db"

        @Volatile
        private var INSTANCE: AlapRoomDatabase? = null

        fun getInstance(context: Context): AlapRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlapRoomDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object LocalRoomMappers {
    private fun stringListToJson(list: List<String>?): String =
        JSONArray(list ?: emptyList<String>()).toString()

    private fun jsonToStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun stringMapToJson(map: Map<String, String>?): String {
        val obj = JSONObject()
        map?.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun jsonToStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optString(k)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun longMapToJson(map: Map<String, Long>?): String {
        val obj = JSONObject()
        map?.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun jsonToLongMap(json: String?): Map<String, Long> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optLong(k)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun ChatMessage.toLocalEntity(): LocalMessageEntity = LocalMessageEntity(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        messageType = messageType,
        stickerId = stickerId,
        pollQuestion = pollQuestion,
        pollOptionsJson = pollOptions?.let { stringListToJson(it) },
        pollVotesJson = pollVotes?.let { longMapToJson(it) },
        replyToMessageId = replyToMessageId,
        replyToSenderName = replyToSenderName,
        replyToPreview = replyToPreview,
        isForwarded = isForwarded,
        isEdited = isEdited,
        isDeleted = isDeleted,
        participantUidsJson = stringListToJson(participantUids),
        readByUidsJson = stringListToJson(readByUids),
        starredByUidsJson = stringListToJson(starredByUids),
        reactionsJson = stringMapToJson(reactions),
        createdAtEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAtEpochMs = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )

    fun LocalMessageEntity.toUiMessage(): UiMessage = UiMessage(
        message = ChatMessage(
            id = id,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            messageType = messageType,
            stickerId = stickerId,
            pollQuestion = pollQuestion,
            pollOptions = pollOptionsJson?.let { jsonToStringList(it) },
            pollVotes = pollVotesJson?.let { jsonToLongMap(it) },
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToPreview = replyToPreview,
            isForwarded = isForwarded,
            isEdited = isEdited,
            isDeleted = isDeleted,
            participantUids = jsonToStringList(participantUidsJson),
            readByUids = jsonToStringList(readByUidsJson),
            starredByUids = jsonToStringList(starredByUidsJson),
            reactions = jsonToStringMap(reactionsJson),
            createdAt = Timestamp(Date(createdAtEpochMs)),
            updatedAt = Timestamp(Date(updatedAtEpochMs))
        ),
        isPendingSync = false
    )

    fun Chat.toLocalEntity(): LocalChatEntity = LocalChatEntity(
        id = id,
        chatType = chatType,
        title = title,
        description = description,
        avatarId = avatarId,
        participantUidsJson = stringListToJson(participantUids),
        adminUidsJson = stringListToJson(adminUids),
        createdBy = createdBy,
        lastMessageText = lastMessageText,
        lastMessageSenderId = lastMessageSenderId,
        lastMessageType = lastMessageType,
        lastMessageAtEpochMs = lastMessageAt?.toDate()?.time ?: System.currentTimeMillis(),
        disappearingHours = disappearingHours,
        pinnedByUidsJson = stringListToJson(pinnedByUids),
        mutedByUidsJson = stringListToJson(mutedByUids),
        archivedByUidsJson = stringListToJson(archivedByUids),
        unreadCountsJson = longMapToJson(unreadCounts),
        createdAtEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAtEpochMs = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )

    fun LocalChatEntity.toChat(): Chat = Chat(
        id = id,
        chatType = chatType,
        title = title,
        description = description,
        avatarId = avatarId,
        participantUids = jsonToStringList(participantUidsJson),
        adminUids = jsonToStringList(adminUidsJson),
        createdBy = createdBy,
        lastMessageText = lastMessageText,
        lastMessageSenderId = lastMessageSenderId,
        lastMessageType = lastMessageType,
        lastMessageAt = Timestamp(Date(lastMessageAtEpochMs)),
        disappearingHours = disappearingHours,
        typingUids = emptyList(),
        pinnedByUids = jsonToStringList(pinnedByUidsJson),
        mutedByUids = jsonToStringList(mutedByUidsJson),
        archivedByUids = jsonToStringList(archivedByUidsJson),
        unreadCounts = jsonToLongMap(unreadCountsJson),
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )
}
