package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalBackupManager
import com.example.data.local.LocalPreferencesManager
import com.example.data.local.room.AlapLocalDao
import com.example.data.local.room.LocalRoomMappers.toLocalEntity
import com.example.data.local.room.LocalRoomMappers.toUiMessage
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.UiMessage
import com.example.data.model.UiState
import com.example.data.repository.AlapRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val repository: AlapRepository,
    private val prefs: LocalPreferencesManager,
    private val currentUserId: String,
    private val chatId: String,
    private val localDao: AlapLocalDao? = null,
    private val backupManager: LocalBackupManager? = null
) : ViewModel() {

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private var typingTimeoutJob: Job? = null

    val chat: StateFlow<Chat?> = repository.observeChat(chatId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val localMessagesFlow = (localDao?.observeMessagesForChat(chatId) ?: flowOf(emptyList()))
        .map { entities -> entities.map { it.toUiMessage() } }

    val messages: StateFlow<UiState<List<UiMessage>>> = repository.observeMessages(chatId, currentUserId)
        .onEach { remoteList ->
            if (localDao != null && remoteList.isNotEmpty()) {
                val synced = remoteList.filter { !it.isPendingSync && it.message.id.isNotBlank() }
                if (synced.isNotEmpty()) {
                    localDao.upsertMessages(synced.map { it.message.toLocalEntity() })
                    backupManager?.saveAutoSnapshot(currentUserId)
                }
            }
        }
        .combine(localMessagesFlow) { remoteList, localList ->
            val merged = LinkedHashMap<String, UiMessage>()
            localList.forEach { merged[it.message.id] = it }
            remoteList.forEach { merged[it.message.id] = it }
            val sorted = merged.values.sortedBy { it.message.createdAt ?: Timestamp(0, 0) }
            UiState.Success(sorted) as UiState<List<UiMessage>>
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w("ChatDetailVM", "Error observing messages", root)
            emit(UiState.Error(root.message ?: "Failed to load messages"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            _draftText.value = prefs.observeDraft(chatId).first()
        }
    }

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun onDraftChanged(text: String) {
        _draftText.value = text
        viewModelScope.launch {
            prefs.saveDraft(chatId, text)
            val currentChat = chat.value ?: return@launch
            if (text.isNotBlank()) {
                repository.setTyping(currentChat, true)
                typingTimeoutJob?.cancel()
                typingTimeoutJob = viewModelScope.launch {
                    delay(2500L)
                    chat.value?.let { repository.setTyping(it, false) }
                }
            } else {
                typingTimeoutJob?.cancel()
                repository.setTyping(currentChat, false)
            }
        }
    }

    fun sendTextMessage(senderName: String, replyTo: ChatMessage?) {
        val currentChat = chat.value ?: return
        val textToSend = _draftText.value.trim()
        if (textToSend.isBlank()) return

        _draftText.value = ""
        typingTimeoutJob?.cancel()
        viewModelScope.launch {
            prefs.saveDraft(chatId, "")
            val res = repository.sendMessage(
                chat = currentChat,
                senderName = senderName,
                text = textToSend,
                messageType = "text",
                replyTo = replyTo
            )
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun sendStickerMessage(senderName: String, stickerId: String, stickerLabel: String, replyTo: ChatMessage?) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            val res = repository.sendMessage(
                chat = currentChat,
                senderName = senderName,
                text = stickerLabel,
                messageType = "sticker",
                stickerId = stickerId,
                replyTo = replyTo
            )
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send sticker"
            }
        }
    }

    fun sendPollMessage(senderName: String, question: String, options: List<String>) {
        val currentChat = chat.value ?: return
        val cleanQuestion = question.trim().take(200)
        val cleanOptions = options.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(6)
        if (cleanQuestion.isBlank() || cleanOptions.size < 2) {
            _errorBanner.value = "Enter a poll question and at least 2 choices."
            return
        }
        viewModelScope.launch {
            val res = repository.sendMessage(
                chat = currentChat,
                senderName = senderName,
                text = cleanQuestion,
                messageType = "poll",
                pollQuestion = cleanQuestion,
                pollOptions = cleanOptions
            )
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create poll"
            }
        }
    }

    fun voteInPoll(message: ChatMessage, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(chatId, message, optionIndex)
        }
    }

    fun setDisappearingHours(hours: Long) {
        viewModelScope.launch {
            val res = repository.setChatDisappearingHours(chatId, hours)
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update timer"
            }
        }
    }

    fun forwardMessage(targetChat: Chat, senderName: String, original: ChatMessage) {
        viewModelScope.launch {
            repository.sendMessage(
                chat = targetChat,
                senderName = senderName,
                text = original.text,
                messageType = original.messageType,
                stickerId = original.stickerId,
                isForwarded = true
            )
        }
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(chatId, message, emoji)
        }
    }

    fun toggleStar(message: ChatMessage) {
        viewModelScope.launch {
            repository.toggleStarMessage(chatId, message)
        }
    }

    fun editMessage(message: ChatMessage, newText: String) {
        viewModelScope.launch {
            val res = repository.editMessage(chatId, message, newText)
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Could not edit message"
            }
        }
    }

    fun softDeleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.softDeleteMessage(chatId, message)
        }
    }

    fun markVisibleMessagesRead(visibleMessages: List<ChatMessage>, hideReadReceipts: Boolean = false) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.markChatAndMessagesRead(currentChat, visibleMessages, hideReadReceipts)
            if (prefs.localOnlyRelayMode.first() && localDao != null) {
                val alreadySavedLocally = visibleMessages.filter { msg ->
                    msg.id.isNotBlank() &&
                        msg.participantUids.isNotEmpty() &&
                        msg.readByUids.containsAll(msg.participantUids) &&
                        msg.starredByUids.isEmpty()
                }
                if (alreadySavedLocally.isNotEmpty()) {
                    localDao.upsertMessages(alreadySavedLocally.map { it.toLocalEntity() })
                    delay(1200L)
                    repository.purgeDeliveredRelayMessages(currentChat, alreadySavedLocally)
                }
            }
        }
    }

    fun updateGroupInfo(title: String, description: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            val res = repository.updateGroupInfo(currentChat, title, description)
            if (res.isFailure) {
                _errorBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update group"
            }
        }
    }

    fun addGroupMember(memberUid: String) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            repository.addGroupMember(currentChat, memberUid)
        }
    }

    fun leaveGroup(onLeft: () -> Unit) {
        val currentChat = chat.value ?: return
        viewModelScope.launch {
            val res = repository.leaveGroup(currentChat)
            if (res.isSuccess) onLeft()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
