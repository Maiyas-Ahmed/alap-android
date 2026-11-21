package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val about: String = "Hey there! I am using Alap.",
    val avatarId: String = "avatar_emerald",
    val publicPhoneNumber: String = "",
    val publicUserId: String = "",
    val hidePhoneNumber: Boolean = true,
    val hideUserId: Boolean = false,
    val hideOnlineStatus: Boolean = false,
    val hideAbout: Boolean = false,
    val hideReadReceipts: Boolean = false,
    val blockedUids: List<String> = emptyList(),
    @get:PropertyName("isProfileCompleted") @field:PropertyName("isProfileCompleted")
    val isProfileCompleted: Boolean = false,
    @get:PropertyName("isDiscoverable") @field:PropertyName("isDiscoverable")
    val isDiscoverable: Boolean = true,
    @get:PropertyName("isOnline") @field:PropertyName("isOnline")
    val isOnline: Boolean = false,
    @get:PropertyName("isVerified") @field:PropertyName("isVerified")
    val isVerified: Boolean = false,
    @get:PropertyName("isSuspended") @field:PropertyName("isSuspended")
    val isSuspended: Boolean = false,
    @get:PropertyName("isStudent") @field:PropertyName("isStudent")
    val isStudent: Boolean = false,
    val studentClass: String = "",
    val studentSubject: String = "",
    val accountType: String = "personal", // "student", "personal", "teacher"
    @get:PropertyName("isTeacher") @field:PropertyName("isTeacher")
    val isTeacher: Boolean = false,
    val teacherSubject: String = "",
    val teacherPlanId: String = "",
    val teacherPlanName: String = "",
    val teacherBillingCycle: String = "none", // "none", "free", "monthly", "yearly"
    val teacherSubscriptionStatus: String = "none", // "none", "free_launch", "active", "grace", "expired"
    val teacherPlanExpiresAtEpochMs: Long = 0L,
    val guestEmail: String = "",
    val guestDeviceInfo: String = "",
    val guestJoinedAtEpochMs: Long = 0L,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isLockedStudentAccount(): Boolean = isProfileCompleted && accountType == "student"
    fun isGuestAccount(): Boolean = accountType == "guest"
}

@IgnoreExtraProperties
data class UserPrivateInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val alapId: String = "",
    val birthYear: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EmailVerificationOtp(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val code: String = "",
    val expiresAtEpochMs: Long = 0L,
    val attempts: Long = 0L,
    val verified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val appTagline: String = "Fast, minimal & offline-ready messaging",
    val maintenanceBanner: String = "",
    @get:PropertyName("isReadOnlyMode") @field:PropertyName("isReadOnlyMode")
    val isReadOnlyMode: Boolean = false,
    val allowStatusUpdates: Boolean = true,
    val allowGroupCreation: Boolean = true,
    val forceUpdateRequired: Boolean = false,
    val maxMessageLength: Long = 1000L,
    val minAppVersion: String = "1.0",
    val latestApkUrl: String = "",
    val latestVersionCode: Long = 1L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DevicePushToken(
    @DocumentId val id: String = "",
    val userId: String = "",
    val token: String = "",
    val platform: String = "android",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val versionTag: String = "",
    val priority: String = "update", // "update", "info", "urgent"
    @get:PropertyName("isActive") @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Chat(
    @DocumentId val id: String = "",
    val chatType: String = "direct", // "direct", "group"
    val title: String = "",
    val description: String = "",
    val avatarId: String = "avatar_emerald",
    val participantUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageType: String = "text", // "text", "sticker", "poll", "quiz", "flashcard", "system"
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val disappearingHours: Long = 0L, // 0 = Off, 24 = 24h, 168 = 7d
    @get:PropertyName("isStudyGroup") @field:PropertyName("isStudyGroup")
    val isStudyGroup: Boolean = false,
    @get:PropertyName("studyLockToAdmin") @field:PropertyName("studyLockToAdmin")
    val studyLockToAdmin: Boolean = false,
    val inviteCode: String = "",
    val typingUids: List<String> = emptyList(),
    val pinnedByUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val archivedByUids: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun otherParticipantUid(currentUserId: String): String? =
        if (chatType == "direct") participantUids.firstOrNull { it != currentUserId } else null

    fun unreadCountFor(uid: String): Int =
        (unreadCounts[uid] as? Number)?.toInt() ?: 0

    fun isPinnedBy(uid: String): Boolean = uid in pinnedByUids
    fun isMutedBy(uid: String): Boolean = uid in mutedByUids
    fun isArchivedBy(uid: String): Boolean = uid in archivedByUids
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = "text", // "text", "sticker", "poll", "quiz", "flashcard", "system"
    val stickerId: String? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollVotes: Map<String, Long>? = null,
    val correctOptionIndex: Long? = null,
    val quizExplanation: String? = null,
    val flashcardFront: String? = null,
    val flashcardBack: String? = null,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    @get:PropertyName("isForwarded") @field:PropertyName("isForwarded")
    val isForwarded: Boolean = false,
    @get:PropertyName("isEdited") @field:PropertyName("isEdited")
    val isEdited: Boolean = false,
    @get:PropertyName("isDeleted") @field:PropertyName("isDeleted")
    val isDeleted: Boolean = false,
    val participantUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val starredByUids: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class UiMessage(
    val message: ChatMessage,
    val isPendingSync: Boolean = false
)

@IgnoreExtraProperties
data class StatusUpdate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarId: String = "avatar_emerald",
    val text: String = "",
    val styleKey: String = "emerald", // "emerald", "slate", "charcoal", "sand"
    @get:PropertyName("isPublic") @field:PropertyName("isPublic")
    val isPublic: Boolean = true,
    val viewerUids: List<String> = emptyList(),
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val chatId: String = "",
    val title: String = "",
    val description: String = "",
    val createdBy: String = "",
    @get:PropertyName("isActive") @field:PropertyName("isActive")
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyItem(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val itemType: String = "note", // "note", "routine", "exam"
    val title: String = "",
    val details: String = "",
    val scheduleText: String = "",
    val examDateEpochMs: Long = 0L,
    val createdBy: String = "",
    val createdByName: String = "",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val chatId: String = "",
    val reason: String = "",
    val status: String = "open", // "open", "resolved", "dismissed"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RemoteVersionInfo(
    val latestVersionName: String = "",
    val latestVersionCode: Long = 0L,
    val forceUpdateRequired: Boolean = false,
    val apkUrl: String = "",
    val releaseDate: String = "",
    val changelog: List<String> = emptyList()
)

object AlapConstants {
    const val CURRENT_APP_VERSION = "1.0.2"
    const val CURRENT_APP_VERSION_CODE = 3L
    const val PUBLIC_WEBSITE_URL = "https://alap-chat.netlify.app"
    const val UPDATE_PORTAL_URL = "https://alap-updates.netlify.app"
    const val UPDATE_VERSION_JSON_URL = "https://alap-updates.netlify.app/version.json"
    val STUDY_POMODORO_PRESETS = listOf(15, 25, 45, 5)
    val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    val AVATAR_KEYS = listOf(
        "avatar_emerald",
        "avatar_sage",
        "avatar_slate",
        "avatar_charcoal",
        "avatar_sand",
        "avatar_teal"
    )
    val STICKER_OPTIONS = listOf(
        "sticker_wave" to "👋 Salam / Hello!",
        "sticker_thumbs" to "👍 Agreed",
        "sticker_heart" to "💚 Thank you",
        "sticker_spark" to "✨ Awesome",
        "sticker_clock" to "⏳ On my way",
        "sticker_check" to "✅ Done"
    )
    val STATUS_STYLES = listOf("emerald", "slate", "charcoal", "sand")
}
