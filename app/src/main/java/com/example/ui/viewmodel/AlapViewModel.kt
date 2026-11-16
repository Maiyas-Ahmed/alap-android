package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalBackupManager
import com.example.data.local.LocalPreferencesManager
import com.example.data.local.room.AlapLocalDao
import com.example.data.local.room.LocalRoomMappers.toChat
import com.example.data.local.room.LocalRoomMappers.toLocalEntity
import com.example.data.model.AlapConstants
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.RemoteVersionInfo
import com.example.data.model.StatusUpdate
import com.example.data.model.UiState
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.repository.AlapRepository
import com.example.service.AlapBackgroundSyncWorker
import com.example.util.NetworkMonitor
import com.example.util.NotificationHelper
import com.example.util.WebsiteUpdateChecker
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AlapViewModel(
    private val repository: AlapRepository,
    private val prefs: LocalPreferencesManager,
    networkMonitor: NetworkMonitor,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    initialDisplayName: String,
    private val localDao: AlapLocalDao? = null,
    private val backupManager: LocalBackupManager? = null
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _otpSending = MutableStateFlow(false)
    val otpSending: StateFlow<Boolean> = _otpSending.asStateFlow()

    private val _otpVerifying = MutableStateFlow(false)
    val otpVerifying: StateFlow<Boolean> = _otpVerifying.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()

    private val _otpDispatched = MutableStateFlow(false)
    val otpDispatched: StateFlow<Boolean> = _otpDispatched.asStateFlow()

    private val _fallbackDeliveryCode = MutableStateFlow<String?>(null)
    val fallbackDeliveryCode: StateFlow<String?> = _fallbackDeliveryCode.asStateFlow()

    private val _profileSaving = MutableStateFlow(false)
    val profileSaving: StateFlow<Boolean> = _profileSaving.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val verifiedOtpUid: StateFlow<String?> = prefs.verifiedOtpUid
        .map<String, String?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val floatingGlassEnabled: StateFlow<Boolean> = prefs.floatingGlassEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val screenSecurityEnabled: StateFlow<Boolean> = prefs.screenSecurityEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val appLockPinHash: StateFlow<String> = prefs.appLockPinHash
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val studyFocusMinutesTotal: StateFlow<Int> = prefs.studyFocusMinutesTotal
        .map { it.toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val studySessionsCompleted: StateFlow<Int> = prefs.studySessionsCompleted
        .map { it.toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private val _remoteVersionInfo = MutableStateFlow<RemoteVersionInfo?>(null)
    val remoteVersionInfo: StateFlow<RemoteVersionInfo?> = _remoteVersionInfo.asStateFlow()

    private val _checkingForUpdate = MutableStateFlow(false)
    val checkingForUpdate: StateFlow<Boolean> = _checkingForUpdate.asStateFlow()

    val dismissedAnnouncementId: StateFlow<String> = prefs.dismissedAnnouncementId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val lastNotifiedAnnouncementId: StateFlow<String?> = prefs.lastNotifiedAnnouncementId
        .map<String, String?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionsOnboardingDone: StateFlow<Boolean?> = prefs.permissionsOnboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val localOnlyRelayMode: StateFlow<Boolean> = prefs.localOnlyRelayMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val localStoredMessageCount: StateFlow<Int> = (localDao?.observeMessageCount() ?: flowOf(0))
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val myProfileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile"))) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myProfile: StateFlow<UserProfile?> = myProfileState
        .map { (it as? UiState.Success)?.data }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val myPrivateInfo: StateFlow<UserPrivateInfo?> = repository.observeMyPrivateInfo(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppConfig())

    val activeAnnouncements: StateFlow<List<Announcement>> = repository.observeActiveAnnouncements()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val discoverableUsers: StateFlow<UiState<List<UserProfile>>> = repository.observeDiscoverableUsers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load contacts"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val cachedLocalChatsFlow = (localDao?.observeChats() ?: flowOf(emptyList()))
        .map { list -> list.map { it.toChat() } }

    val myChats: StateFlow<UiState<List<Chat>>> = repository.observeMyChats(currentUserId)
        .onEach { remoteChats ->
            if (localDao != null && remoteChats.isNotEmpty()) {
                localDao.upsertChats(remoteChats.map { it.toLocalEntity() })
            }
        }
        .combine(cachedLocalChatsFlow) { remoteChats, localChats ->
            val merged = LinkedHashMap<String, Chat>()
            localChats.forEach { merged[it.id] = it }
            remoteChats.forEach { merged[it.id] = it }
            UiState.Success(merged.values.toList()) as UiState<List<Chat>>
        }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load chats"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val statuses: StateFlow<UiState<List<StatusUpdate>>> = repository.observeStatuses()
        .map<List<StatusUpdate>, UiState<List<StatusUpdate>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load updates"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val starredMessages: StateFlow<List<ChatMessage>> = myChats
        .flatMapLatest { state ->
            val chats = (state as? UiState.Success)?.data ?: emptyList()
            repository.observeStarredMessagesForChats(chats, currentUserId)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndPrivateInfo(
                uid = currentUserId,
                displayName = initialDisplayName,
                email = currentUserEmail
            )
        }
        refreshRemoteVersionInfo(silent = true)
    }

    fun refreshRemoteVersionInfo(silent: Boolean = false) {
        if (_checkingForUpdate.value) return
        viewModelScope.launch {
            _checkingForUpdate.value = true
            val info = WebsiteUpdateChecker.fetchLatestVersion()
            if (info != null) {
                _remoteVersionInfo.value = info
            }
            _checkingForUpdate.value = false
            if (!silent) {
                _actionMessage.value = when {
                    info == null -> "Could not reach update portal right now."
                    info.latestVersionCode > AlapConstants.CURRENT_APP_VERSION_CODE ||
                        WebsiteUpdateChecker.isVersionLower(
                            AlapConstants.CURRENT_APP_VERSION,
                            info.latestVersionName
                        ) -> "নতুন ভার্সন পাওয়া গেছে: v${info.latestVersionName}"
                    else -> "আপনি সর্বশেষ ভার্সন ব্যবহার করছেন (v${AlapConstants.CURRENT_APP_VERSION})"
                }
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun issueOtpCode(context: Context) {
        if (_otpSending.value) return
        viewModelScope.launch {
            _otpSending.value = true
            _otpError.value = null
            val res = repository.issueEmailVerificationCode(currentUserId, currentUserEmail)
            _otpSending.value = false
            res.onSuccess { code ->
                _otpDispatched.value = true
                _fallbackDeliveryCode.value = code
                NotificationHelper.showOtpNotification(
                    context = context,
                    email = currentUserEmail,
                    code = code
                )
            }.onFailure { e ->
                _otpError.value = e.localizedMessage ?: "Failed to generate verification code."
            }
        }
    }

    fun sendFreeInboxVerificationEmail() {
        viewModelScope.launch {
            _otpSending.value = true
            _otpError.value = null
            val res = repository.sendFreeFirebaseInboxVerification()
            _otpSending.value = false
            res.onSuccess {
                _actionMessage.value = "Verification link sent to $currentUserEmail via Firebase Auth."
            }.onFailure { e ->
                _otpError.value = e.localizedMessage ?: "Could not send Firebase verification email."
            }
        }
    }

    fun completeWithFirebaseEmailVerified() {
        viewModelScope.launch {
            _otpVerifying.value = true
            _otpError.value = null
            val res = repository.checkFirebaseEmailVerified()
            _otpVerifying.value = false
            res.onSuccess { verified ->
                if (verified) {
                    prefs.setVerifiedOtpUid(currentUserId)
                } else {
                    _otpError.value = "Gmail link not confirmed yet. Check your Gmail inbox or enter the 6-digit code."
                }
            }.onFailure { e ->
                _otpError.value = e.localizedMessage ?: "Verification check failed."
            }
        }
    }

    fun verifyOtpCode(enteredCode: String) {
        if (_otpVerifying.value) return
        viewModelScope.launch {
            _otpVerifying.value = true
            _otpError.value = null
            val res = repository.verifyEmailCode(currentUserId, enteredCode)
            _otpVerifying.value = false
            res.onSuccess {
                prefs.setVerifiedOtpUid(currentUserId)
                _fallbackDeliveryCode.value = null
            }.onFailure { e ->
                _otpError.value = e.localizedMessage ?: "Verification failed."
            }
        }
    }

    fun clearOtpSessionSync() {
        viewModelScope.launch {
            prefs.clearVerifiedOtpUid()
        }
    }

    fun saveProfileAndPrivacy(
        displayName: String,
        handle: String,
        phoneNumber: String,
        alapId: String,
        about: String,
        avatarId: String,
        hidePhoneNumber: Boolean,
        hideUserId: Boolean,
        hideOnlineStatus: Boolean,
        hideAbout: Boolean,
        hideReadReceipts: Boolean = false,
        isDiscoverable: Boolean,
        isStudent: Boolean = false,
        studentClass: String = "",
        studentSubject: String = "",
        birthYear: Long = 0L,
        accountType: String = "personal",
        teacherSubject: String = "",
        markProfileCompleted: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _profileSaving.value = true
            val result = repository.updateProfileAndPrivacy(
                displayName = displayName,
                handle = handle,
                phoneNumber = phoneNumber,
                alapId = alapId,
                about = about,
                avatarId = avatarId,
                hidePhoneNumber = hidePhoneNumber,
                hideUserId = hideUserId,
                hideOnlineStatus = hideOnlineStatus,
                hideAbout = hideAbout,
                hideReadReceipts = hideReadReceipts,
                isDiscoverable = isDiscoverable,
                isStudent = isStudent,
                studentClass = studentClass,
                studentSubject = studentSubject,
                birthYear = birthYear,
                accountType = accountType,
                teacherSubject = teacherSubject,
                markProfileCompleted = markProfileCompleted
            )
            _profileSaving.value = false
            _actionMessage.value = if (result.isSuccess) "Profile & privacy saved" else result.exceptionOrNull()?.localizedMessage
            if (result.isSuccess) onComplete()
        }
    }

    fun toggleBlockUser(targetUid: String) {
        val currentBlocked = myProfile.value?.blockedUids ?: emptyList()
        viewModelScope.launch {
            val res = repository.toggleBlockUser(targetUid, currentBlocked)
            res.onSuccess { blocked ->
                _actionMessage.value = if (blocked) "User blocked" else "User unblocked"
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Unable to update block status"
            }
        }
    }

    fun submitReport(targetUid: String, targetName: String, chatId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.submitUserReport(targetUid, targetName, chatId, reason)
            _actionMessage.value = if (res.isSuccess) "Report submitted for admin review" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun startDirectChat(otherUser: UserProfile, onChatReady: (String) -> Unit) {
        val current = myProfile.value ?: UserProfile(
            userId = currentUserId,
            displayName = "Alap User"
        )
        viewModelScope.launch {
            val result = repository.getOrCreateDirectChat(current, otherUser)
            result.onSuccess { chatId -> onChatReady(chatId) }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Unable to open chat" }
        }
    }

    fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        isStudyGroup: Boolean = false,
        studyLockToAdmin: Boolean = false,
        onChatReady: (String) -> Unit
    ) {
        val current = myProfile.value ?: UserProfile(
            userId = currentUserId,
            displayName = "Alap User"
        )
        viewModelScope.launch {
            val result = repository.createGroupChat(
                currentUser = current,
                title = title,
                description = description,
                memberUids = memberUids,
                isStudyGroup = isStudyGroup,
                studyLockToAdmin = studyLockToAdmin
            )
            result.onSuccess { chatId -> onChatReady(chatId) }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Unable to create group" }
        }
    }

    fun joinGroupByStudyCode(inviteCode: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.joinGroupByStudyCode(inviteCode)
            result.onSuccess { chatId ->
                _actionMessage.value = "Joined Study Group!"
                onChatReady(chatId)
            }.onFailure { e ->
                _actionMessage.value = e.localizedMessage ?: "Could not join with that Class Code"
            }
        }
    }

    fun recordStudySession(minutes: Int) {
        viewModelScope.launch {
            prefs.recordCompletedStudyMinutes(minutes)
        }
    }

    fun togglePin(chat: Chat) {
        viewModelScope.launch { repository.togglePinChat(chat) }
    }

    fun toggleMute(chat: Chat) {
        viewModelScope.launch { repository.toggleMuteChat(chat) }
    }

    fun toggleArchive(chat: Chat) {
        viewModelScope.launch { repository.toggleArchiveChat(chat) }
    }

    fun postStatus(text: String, styleKey: String, onDone: () -> Unit = {}) {
        val author = myProfile.value ?: UserProfile(userId = currentUserId, displayName = "Alap User")
        viewModelScope.launch {
            val result = repository.postStatus(author, text, styleKey)
            _actionMessage.value = if (result.isSuccess) "Status update posted" else result.exceptionOrNull()?.localizedMessage
            if (result.isSuccess) onDone()
        }
    }

    fun markStatusViewed(status: StatusUpdate) {
        viewModelScope.launch { repository.markStatusViewed(status) }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            val res = repository.deleteStatus(statusId)
            if (res.isSuccess) _actionMessage.value = "Status deleted"
        }
    }

    fun dismissAnnouncement(announcementId: String) {
        viewModelScope.launch { prefs.dismissAnnouncement(announcementId) }
    }

    fun markAnnouncementNotified(announcementId: String) {
        viewModelScope.launch { prefs.markAnnouncementNotified(announcementId) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setFloatingGlassEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setFloatingGlassEnabled(enabled) }
    }

    fun setScreenSecurityEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setScreenSecurityEnabled(enabled) }
    }

    fun completePermissionsOnboarding(notificationsGranted: Boolean, overlayGranted: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationsEnabled(notificationsGranted)
            if (overlayGranted) {
                prefs.setFloatingGlassEnabled(true)
            }
            prefs.setPermissionsOnboardingDone(true)
        }
    }

    fun setLocalOnlyRelayMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setLocalOnlyRelayMode(enabled)
            _actionMessage.value = if (enabled) {
                "Local-First Relay enabled: messages save on device & clear from server after delivery"
            } else {
                "Standard Cloud Sync mode active"
            }
        }
    }

    fun exportLocalBackupToUri(uri: Uri) {
        val manager = backupManager ?: return
        viewModelScope.launch {
            val res = manager.exportToUri(uri, currentUserId)
            res.onSuccess { summary ->
                _actionMessage.value = "Backed up ${summary.messageCount} messages (${summary.chatCount} chats) locally!"
            }.onFailure { e ->
                _actionMessage.value = e.localizedMessage ?: "Backup export failed"
            }
        }
    }

    fun restoreLocalBackupFromUri(uri: Uri) {
        val manager = backupManager ?: return
        viewModelScope.launch {
            val res = manager.restoreFromUri(uri)
            res.onSuccess { summary ->
                _actionMessage.value = "Restored ${summary.messageCount} messages across ${summary.chatCount} chats!"
            }.onFailure { e ->
                _actionMessage.value = e.localizedMessage ?: "Backup restore failed"
            }
        }
    }

    fun restoreAutoSnapshot() {
        val manager = backupManager ?: return
        viewModelScope.launch {
            val res = manager.restoreFromAutoSnapshot()
            res.onSuccess { summary ->
                _actionMessage.value = "Auto-snapshot restored (${summary.messageCount} messages)!"
            }.onFailure { e ->
                _actionMessage.value = e.localizedMessage ?: "No auto-snapshot available yet"
            }
        }
    }

    fun registerPushNotifications(context: Context) {
        AlapBackgroundSyncWorker.schedule(context)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("alap_updates")
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    viewModelScope.launch {
                        repository.saveDevicePushToken(token)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AlapViewModel", "FCM token registration deferred", e)
        }
    }

    fun setAppLockPin(rawPin: String) {
        viewModelScope.launch {
            prefs.setAppLockPin(rawPin)
            _actionMessage.value = if (rawPin.isBlank()) "App Lock disabled" else "App Lock PIN saved"
        }
    }

    fun verifyAppPin(rawPin: String): Boolean = prefs.verifyAppPin(rawPin, appLockPinHash.value)

    fun setOnlineStatus(online: Boolean) {
        val hideOnline = myProfile.value?.hideOnlineStatus == true
        viewModelScope.launch { repository.setOnlineStatus(online, hideOnline) }
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("AlapViewModel", fallback, rootError)
        return rootError.message ?: fallback
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
