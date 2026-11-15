package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminApiKey
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminEntry
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.Chat
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserReport
import com.example.data.repository.AlapRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: AlapRepository
) : ViewModel() {

    private val _statusFeedback = MutableStateFlow<String?>(null)
    val statusFeedback: StateFlow<String?> = _statusFeedback.asStateFlow()

    private val _userEmails = MutableStateFlow<Map<String, String>>(emptyMap())
    val userEmails: StateFlow<Map<String, String>> = _userEmails.asStateFlow()

    val allUsers: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsersForAdmin()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Admin permission required"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allChats: StateFlow<List<Chat>> = repository.observeAllChatsForAdmin()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = repository.observeAllAnnouncementsForAdmin()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val admins: StateFlow<List<AdminEntry>> = repository.observeAdmins()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val auditLogs: StateFlow<List<AdminAuditLog>> = repository.observeAuditLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val reports: StateFlow<List<UserReport>> = repository.observeReports()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppConfig())

    val apiKeys: StateFlow<List<AdminApiKey>> = repository.observeAdminApiKeys()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun clearFeedback() {
        _statusFeedback.value = null
    }

    fun publishAnnouncement(
        title: String,
        message: String,
        versionTag: String,
        priority: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.publishAnnouncement(title, message, versionTag, priority)
            if (res.isSuccess) {
                repository.writeAuditLog("PUBLISH_ANNOUNCEMENT", res.getOrNull().orEmpty(), title)
                onSuccess()
                _statusFeedback.value = "Update banner & push notification published!"
            } else {
                _statusFeedback.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish announcement"
            }
        }
    }

    fun toggleAnnouncementActive(announcement: Announcement) {
        viewModelScope.launch {
            repository.toggleAnnouncementActive(announcement)
            repository.writeAuditLog("TOGGLE_ANNOUNCEMENT", announcement.id, "active=${!announcement.isActive}")
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            repository.writeAuditLog("DELETE_ANNOUNCEMENT", id, "Removed announcement")
            _statusFeedback.value = "Announcement removed"
        }
    }

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            val res = repository.updateAppConfig(config)
            _statusFeedback.value = if (res.isSuccess) {
                "App configuration saved"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update config"
            }
        }
    }

    fun toggleUserVerified(user: UserProfile) {
        viewModelScope.launch {
            repository.adminToggleUserVerified(user)
        }
    }

    fun toggleUserSuspended(user: UserProfile) {
        viewModelScope.launch {
            repository.adminToggleUserSuspended(user)
        }
    }

    fun loadUserEmail(userId: String) {
        if (_userEmails.value.containsKey(userId)) return
        viewModelScope.launch {
            val res = repository.adminGetUserEmail(userId)
            res.getOrNull()?.let { email ->
                _userEmails.value = _userEmails.value + (userId to email)
            }
        }
    }

    fun grantSecondaryAdmin(user: UserProfile, email: String) {
        viewModelScope.launch {
            val res = repository.adminGrantSecondaryAdmin(user.userId, email)
            _statusFeedback.value = if (res.isSuccess) {
                "${user.displayName} granted Admin access"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Only primary admin can grant admin roles"
            }
        }
    }

    fun revokeSecondaryAdmin(uid: String) {
        viewModelScope.launch {
            val res = repository.adminRevokeSecondaryAdmin(uid)
            if (res.isSuccess) _statusFeedback.value = "Admin access revoked"
        }
    }

    fun updateReportStatus(report: UserReport, status: String) {
        viewModelScope.launch {
            val res = repository.updateReportStatus(report, status)
            _statusFeedback.value = if (res.isSuccess) "Report marked $status" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun cleanupExpiredStatuses() {
        viewModelScope.launch {
            val res = repository.cleanupExpiredStatuses()
            res.onSuccess { count ->
                _statusFeedback.value = "Cleaned up $count expired status update(s)"
            }.onFailure {
                _statusFeedback.value = it.localizedMessage ?: "Cleanup failed"
            }
        }
    }

    fun createApiKey(label: String, scopes: List<String>) {
        viewModelScope.launch {
            val res = repository.createAdminApiKey(label, scopes)
            res.onSuccess {
                _statusFeedback.value = "API Key '${it.label}' created"
            }.onFailure {
                _statusFeedback.value = it.localizedMessage ?: "Only primary admin can create API keys"
            }
        }
    }

    fun toggleApiKeyActive(key: AdminApiKey) {
        viewModelScope.launch {
            val res = repository.toggleAdminApiKeyActive(key)
            if (res.isFailure) {
                _statusFeedback.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update API key"
            }
        }
    }

    fun deleteApiKey(keyId: String) {
        viewModelScope.launch {
            val res = repository.deleteAdminApiKey(keyId)
            _statusFeedback.value = if (res.isSuccess) "API Key deleted" else res.exceptionOrNull()?.localizedMessage
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
