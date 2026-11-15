package com.example.data.repository

import android.content.Context
import com.example.data.firestore.AlapFirestoreProvider
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.AdminApiKey
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminEntry
import com.example.data.model.AlapConstants
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.EmailVerificationOtp
import com.example.data.model.StatusUpdate
import com.example.data.model.UiMessage
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.model.UserReport
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class AlapRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(AlapFirestoreProvider.getDatabase(context))

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeIdSegment(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(58)

    private fun buildDirectChatId(uid1: String, uid2: String): String {
        val sorted = listOf(sanitizeIdSegment(uid1), sanitizeIdSegment(uid2)).sorted()
        return "dm_${sorted[0]}_${sorted[1]}"
    }

    private fun generateDefaultAlapId(uid: String): String {
        val clean = uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
        val suffix = if (clean.length >= 6) clean.take(6) else clean.padEnd(6, '0')
        return "ALAP-$suffix"
    }

    // --- User Profile & PII Isolation ---

    suspend fun ensureUserProfileAndPrivateInfo(
        uid: String,
        displayName: String,
        email: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val safeName = displayName.trim().ifBlank { "Alap User" }.take(50)
        val safeEmail = email.trim().ifBlank { "user@alap.local" }.take(160)
        val rawHandle = safeName.lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank { "alapuser" }
        val safeHandle = (if (rawHandle.length < 2) "${rawHandle}01" else rawHandle).take(30)
        val defaultAlapId = generateDefaultAlapId(uid)

        val userRef = db.collection("users").document(uid)
        val existing = userRef.get().await()
        if (!existing.exists()) {
            val profilePayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "handle" to safeHandle,
                "about" to "Hey there! I am using Alap.",
                "avatarId" to AlapConstants.AVATAR_KEYS.first(),
                "publicPhoneNumber" to "",
                "publicUserId" to defaultAlapId,
                "hidePhoneNumber" to true,
                "hideUserId" to false,
                "hideOnlineStatus" to false,
                "hideAbout" to false,
                "hideReadReceipts" to false,
                "blockedUids" to emptyList<String>(),
                "isProfileCompleted" to false,
                "isDiscoverable" to true,
                "isOnline" to true,
                "isVerified" to false,
                "isSuspended" to false,
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(profilePayload).await()
        } else {
            val hideOnline = existing.getBoolean("hideOnlineStatus") ?: false
            val updates = mutableMapOf<String, Any>(
                "isOnline" to !hideOnline,
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.contains("publicPhoneNumber")) updates["publicPhoneNumber"] = ""
            if (!existing.contains("publicUserId")) updates["publicUserId"] = defaultAlapId
            if (!existing.contains("hidePhoneNumber")) updates["hidePhoneNumber"] = true
            if (!existing.contains("hideUserId")) updates["hideUserId"] = false
            if (!existing.contains("hideOnlineStatus")) updates["hideOnlineStatus"] = false
            if (!existing.contains("hideAbout")) updates["hideAbout"] = false
            if (!existing.contains("hideReadReceipts")) updates["hideReadReceipts"] = false
            if (!existing.contains("blockedUids")) updates["blockedUids"] = emptyList<String>()
            if (!existing.contains("isProfileCompleted")) updates["isProfileCompleted"] = false
            userRef.update(updates).await()
        }

        val privateRef = userRef.collection("private").document("info")
        val existingPrivate = privateRef.get().await()
        if (!existingPrivate.exists()) {
            val privatePayload = mapOf(
                "userId" to uid,
                "email" to safeEmail,
                "phoneNumber" to "",
                "alapId" to defaultAlapId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            privateRef.set(privatePayload).await()
        } else {
            val privateUpdates = mutableMapOf<String, Any>()
            if (existingPrivate.getString("email") != safeEmail) {
                privateUpdates["email"] = safeEmail
            }
            if (!existingPrivate.contains("phoneNumber")) {
                privateUpdates["phoneNumber"] = ""
            }
            if (!existingPrivate.contains("alapId")) {
                privateUpdates["alapId"] = defaultAlapId
            }
            if (privateUpdates.isNotEmpty()) {
                privateUpdates["updatedAt"] = FieldValue.serverTimestamp()
                privateRef.update(privateUpdates).await()
            }
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$uid")
    }

    suspend fun updateProfileAndPrivacy(
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
        markProfileCompleted: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Alap User" }.take(50)
        val cleanHandle = handle.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").let {
            if (it.length < 2) "alap_$it" else it
        }.take(30)
        val safePhone = phoneNumber.trim().take(24)
        val safeAlapId = alapId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank {
            generateDefaultAlapId(uid)
        }.take(32)
        val safeAbout = about.trim().take(140)
        val safeAvatar = avatarId.ifBlank { AlapConstants.AVATAR_KEYS.first() }.take(40)

        val userRef = db.collection("users").document(uid)
        val privateRef = userRef.collection("private").document("info")

        val batch = db.batch()
        val profileUpdates = mutableMapOf<String, Any>(
            "displayName" to safeName,
            "handle" to cleanHandle,
            "about" to safeAbout,
            "avatarId" to safeAvatar,
            "publicPhoneNumber" to if (hidePhoneNumber) "" else safePhone,
            "publicUserId" to if (hideUserId) "" else safeAlapId,
            "hidePhoneNumber" to hidePhoneNumber,
            "hideUserId" to hideUserId,
            "hideOnlineStatus" to hideOnlineStatus,
            "hideAbout" to hideAbout,
            "hideReadReceipts" to hideReadReceipts,
            "isProfileCompleted" to markProfileCompleted,
            "isDiscoverable" to isDiscoverable,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (hideOnlineStatus) {
            profileUpdates["isOnline"] = false
        }
        batch.update(userRef, profileUpdates)
        batch.update(
            privateRef,
            mapOf(
                "phoneNumber" to safePhone,
                "alapId" to safeAlapId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    suspend fun toggleBlockUser(targetUid: String, currentBlocked: List<String>): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val willBlock = targetUid !in currentBlocked
        val updated = if (willBlock) (currentBlocked + targetUid).distinct() else currentBlocked - targetUid
        db.collection("users").document(uid).update(
            mapOf(
                "blockedUids" to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        willBlock
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    suspend fun sendFreeFirebaseInboxVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        user.sendEmailVerification().await()
        Unit
    }

    suspend fun checkFirebaseEmailVerified(): Result<Boolean> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
        user.reload().await()
        user.isEmailVerified
    }

    suspend fun setOnlineStatus(isOnline: Boolean, hideOnlineStatus: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val effectiveOnline = if (hideOnlineStatus) false else isOnline
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to effectiveOnline,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        }
    }

    fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleListenerError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeMyPrivateInfo(uid: String): Flow<UserPrivateInfo?> = flow {
        val docRef = db.collection("users").document(uid).collection("private").document("info")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserPrivateInfo::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleListenerError(error, OperationType.GET, docRef.path, uid)
                    emit(null)
                }
        )
    }

    // --- Gmail Verification Code (6-Digit OTP) ---

    suspend fun issueEmailVerificationCode(uid: String, email: String): Result<String> = runCatching {
        requireUserId()
        val randomCode = (100000 + SecureRandom().nextInt(900000)).toString()
        val docRef = db.collection("users").document(uid).collection("private").document("verification")
        val existing = docRef.get().await()
        val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
        val safeEmail = email.trim().ifBlank { "user@gmail.com" }.take(160)

        if (!existing.exists()) {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to safeEmail,
                    "code" to randomCode,
                    "expiresAtEpochMs" to expiresAt,
                    "attempts" to 0L,
                    "verified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "email" to safeEmail,
                    "code" to randomCode,
                    "expiresAtEpochMs" to expiresAt,
                    "attempts" to 0L,
                    "verified" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        randomCode
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/private/verification")
    }

    suspend fun verifyEmailCode(uid: String, enteredCode: String): Result<Boolean> = runCatching {
        requireUserId()
        val clean = enteredCode.trim()
        require(clean.length == 6 && clean.all { it.isDigit() }) {
            "Please enter the 6-digit verification code."
        }
        val docRef = db.collection("users").document(uid).collection("private").document("verification")
        val snapshot = docRef.get().await()
        val otp = snapshot.toObject(EmailVerificationOtp::class.java)
            ?: throw IllegalStateException("No verification code found. Tap Resend Code.")

        if (otp.expiresAtEpochMs < System.currentTimeMillis()) {
            throw IllegalStateException("Verification code expired. Please tap Resend Code.")
        }
        if (otp.attempts >= 5L) {
            throw IllegalStateException("Too many failed attempts. Please tap Resend Code.")
        }
        if (otp.code != clean) {
            val nextAttempts = otp.attempts + 1L
            docRef.update(
                mapOf(
                    "attempts" to nextAttempts,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val remaining = (5L - nextAttempts).coerceAtLeast(0L)
            throw IllegalArgumentException("Incorrect code. $remaining attempts remaining.")
        }
        docRef.update(
            mapOf(
                "verified" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        true
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e is FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/private/verification")
        }
    }

    fun observeDiscoverableUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .whereEqualTo("isDiscoverable", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    // --- Admin & Global Config & Announcements ---

    fun observeIsAdmin(uid: String, email: String?, emailVerified: Boolean): Flow<Boolean> {
        val isPrimary = email.equals(AlapConstants.PRIMARY_ADMIN_EMAIL, ignoreCase = true) && emailVerified
        if (isPrimary) {
            return flow { emit(true) }
        }
        return db.collection("admins").document(uid)
            .snapshots()
            .map { it.exists() }
            .catch { emit(false) }
    }

    fun observeAppConfig(): Flow<AppConfig> = flow {
        val uid = requireUserId()
        val docRef = db.collection("app_config").document("global")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AppConfig::class.java, ServerTimestampBehavior.ESTIMATE) ?: AppConfig()
                    } else {
                        AppConfig()
                    }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.GET, docRef.path, uid)
                    emit(AppConfig())
                }
        )
    }

    fun observeActiveAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllAnnouncementsForAdmin(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllChatsForAdmin(): Flow<List<Chat>> = flow {
        val uid = requireUserId()
        val path = "chats"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Chat::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAdmins(): Flow<List<AdminEntry>> = flow {
        val uid = requireUserId()
        val path = "admins"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminEntry::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    emit(emptyList())
                }
        )
    }

    suspend fun publishAnnouncement(
        title: String,
        message: String,
        versionTag: String,
        priority: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("announcements").document()
        val safePriority = if (priority in listOf("update", "info", "urgent")) priority else "update"
        val payload = mapOf(
            "title" to title.trim().take(100),
            "message" to message.trim().take(500),
            "versionTag" to versionTag.trim().take(24),
            "priority" to safePriority,
            "isActive" to true,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "announcements")
    }

    suspend fun toggleAnnouncementActive(announcement: Announcement): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("announcements").document(announcement.id)
        docRef.update(
            mapOf(
                "isActive" to !announcement.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "announcements/${announcement.id}")
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("announcements").document(announcementId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "announcements/$announcementId")
    }

    suspend fun updateAppConfig(config: AppConfig): Result<Unit> = runCatching {
        val uid = requireUserId()
        val payload = mapOf(
            "appTagline" to config.appTagline.trim().take(100),
            "maintenanceBanner" to config.maintenanceBanner.trim().take(300),
            "isReadOnlyMode" to config.isReadOnlyMode,
            "allowStatusUpdates" to config.allowStatusUpdates,
            "allowGroupCreation" to config.allowGroupCreation,
            "forceUpdateRequired" to config.forceUpdateRequired,
            "maxMessageLength" to config.maxMessageLength.coerceIn(50L, 4000L),
            "minAppVersion" to config.minAppVersion.trim().ifBlank { "1.0" }.take(20),
            "latestApkUrl" to config.latestApkUrl.trim().take(500),
            "latestVersionCode" to config.latestVersionCode.coerceIn(1L, 100000L),
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("app_config").document("global").set(payload).await()
        writeAuditLog("UPDATE_CONFIG", "global", "ReadOnly=${config.isReadOnlyMode}, ForceUpdate=${config.forceUpdateRequired}, MinVer=${config.minAppVersion}")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "app_config/global")
    }

    // --- Admin API Keys (Primary Admin Exclusive) ---

    fun observeAdminApiKeys(): Flow<List<AdminApiKey>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("admin_api_keys")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminApiKey::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, "admin_api_keys", uid)
                    emit(emptyList())
                }
        )
    }

    suspend fun createAdminApiKey(label: String, scopes: List<String>): Result<AdminApiKey> = runCatching {
        val uid = requireUserId()
        val randomBytes = ByteArray(18)
        SecureRandom().nextBytes(randomBytes)
        val generatedKey = "alap_live_" + randomBytes.joinToString("") { "%02x".format(it) }
        val cleanLabel = label.trim().ifBlank { "External Web App" }.take(80)
        val cleanScopes = scopes.filter { it in AlapConstants.API_KEY_SCOPES }.distinct().take(12)
        val docRef = db.collection("admin_api_keys").document()
        val payload = mapOf(
            "label" to cleanLabel,
            "apiKey" to generatedKey,
            "scopes" to cleanScopes,
            "isActive" to true,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        writeAuditLog("CREATE_API_KEY", docRef.id, "Created API key '$cleanLabel'")
        AdminApiKey(
            id = docRef.id,
            label = cleanLabel,
            apiKey = generatedKey,
            scopes = cleanScopes,
            isActive = true,
            createdBy = uid
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "admin_api_keys")
    }

    suspend fun toggleAdminApiKeyActive(key: AdminApiKey): Result<Unit> = runCatching {
        requireUserId()
        db.collection("admin_api_keys").document(key.id).update(
            mapOf(
                "isActive" to !key.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        writeAuditLog("TOGGLE_API_KEY", key.id, "${key.label} active=${!key.isActive}")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "admin_api_keys/${key.id}")
    }

    suspend fun deleteAdminApiKey(keyId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("admin_api_keys").document(keyId).delete().await()
        writeAuditLog("DELETE_API_KEY", keyId, "Deleted API key")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "admin_api_keys/$keyId")
    }

    // --- Device FCM Push Tokens ---

    suspend fun saveDevicePushToken(token: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanToken = token.trim().take(512)
        require(cleanToken.length >= 10)
        db.collection("device_tokens").document(uid).set(
            mapOf(
                "userId" to uid,
                "token" to cleanToken,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "device_tokens/${auth.currentUser?.uid}")
    }

    suspend fun adminToggleUserVerified(user: UserProfile): Result<Unit> = runCatching {
        requireUserId()
        val next = !user.isVerified
        db.collection("users").document(user.userId).update(
            mapOf(
                "isVerified" to next,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        writeAuditLog("TOGGLE_VERIFIED", user.userId, "${user.displayName} verified=$next")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${user.userId}")
    }

    suspend fun adminToggleUserSuspended(user: UserProfile): Result<Unit> = runCatching {
        requireUserId()
        val next = !user.isSuspended
        db.collection("users").document(user.userId).update(
            mapOf(
                "isSuspended" to next,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        writeAuditLog("TOGGLE_SUSPENDED", user.userId, "${user.displayName} suspended=$next")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${user.userId}")
    }

    suspend fun adminGetUserEmail(userId: String): Result<String?> = runCatching {
        requireUserId()
        val doc = db.collection("users").document(userId).collection("private").document("info").get().await()
        doc.getString("email")
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, "users/$userId/private/info")
    }

    suspend fun adminGrantSecondaryAdmin(targetUid: String, targetEmail: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val payload = mapOf(
            "uid" to targetUid,
            "email" to targetEmail.trim().take(160),
            "addedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("admins").document(targetUid).set(payload).await()
        writeAuditLog("GRANT_ADMIN", targetUid, "Granted admin to $targetEmail")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "admins/$targetUid")
    }

    suspend fun adminRevokeSecondaryAdmin(targetUid: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("admins").document(targetUid).delete().await()
        writeAuditLog("REVOKE_ADMIN", targetUid, "Revoked delegated admin")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "admins/$targetUid")
    }

    suspend fun writeAuditLog(action: String, targetId: String, details: String) {
        val user = auth.currentUser ?: return
        try {
            val docRef = db.collection("audit_logs").document()
            docRef.set(
                mapOf(
                    "adminUid" to user.uid,
                    "adminEmail" to (user.email ?: "admin@alap").take(160),
                    "action" to action.take(60),
                    "targetId" to targetId.take(160),
                    "details" to details.take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Non-fatal if audit log fails on offline cache
        }
    }

    fun observeAuditLogs(): Flow<List<AdminAuditLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("audit_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminAuditLog::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .take(100)
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, "audit_logs", uid)
                    emit(emptyList())
                }
        )
    }

    suspend fun submitUserReport(
        targetUid: String,
        targetName: String,
        chatId: String,
        reason: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("reports").document()
        docRef.set(
            mapOf(
                "reporterUid" to uid,
                "targetUid" to targetUid.take(128),
                "targetName" to targetName.trim().ifBlank { "User" }.take(60),
                "chatId" to chatId.take(128),
                "reason" to reason.trim().ifBlank { "Spam or inappropriate behavior" }.take(300),
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "reports")
    }

    fun observeReports(): Flow<List<UserReport>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reports")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserReport::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, "reports", uid)
                    emit(emptyList())
                }
        )
    }

    suspend fun updateReportStatus(report: UserReport, status: String): Result<Unit> = runCatching {
        requireUserId()
        val safeStatus = if (status in listOf("open", "resolved", "dismissed")) status else "resolved"
        db.collection("reports").document(report.id).update(
            mapOf(
                "status" to safeStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        writeAuditLog("REPORT_$safeStatus", report.targetUid, "Marked report ${report.id} as $safeStatus")
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "reports/${report.id}")
    }

    suspend fun cleanupExpiredStatuses(): Result<Int> = runCatching {
        requireUserId()
        val now = System.currentTimeMillis()
        val snapshot = db.collection("statuses").get().await()
        val expired = snapshot.documents.filter { doc ->
            val exp = doc.getLong("expiresAtEpochMs") ?: 0L
            exp in 1 until now
        }
        val batch = db.batch()
        expired.take(100).forEach { batch.delete(it.reference) }
        if (expired.isNotEmpty()) batch.commit().await()
        writeAuditLog("CLEANUP_STATUSES", "statuses", "Deleted ${expired.size} expired statuses")
        expired.size
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "statuses")
    }

    // --- Chats & Messages ---

    fun observeMyChats(uid: String): Flow<List<Chat>> = flow {
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Chat::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<Chat> { it.isPinnedBy(uid) }
                                .thenByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeChat(chatId: String): Flow<Chat?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("chats").document(chatId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Chat::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleListenerError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeMessages(chatId: String, currentUserId: String): Flow<List<UiMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantUids", currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        msg?.let {
                            UiMessage(
                                message = it,
                                isPendingSync = doc.metadata.hasPendingWrites()
                            )
                        }
                    }.sortedBy { it.message.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, currentUserId)
                    throw error
                }
        )
    }

    suspend fun getOrCreateDirectChat(
        currentUser: UserProfile,
        otherUser: UserProfile
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(uid == currentUser.userId)
        val chatId = buildDirectChatId(currentUser.userId, otherUser.userId)
        val chatRef = db.collection("chats").document(chatId)
        val existing = chatRef.get().await()
        if (!existing.exists()) {
            val participants = listOf(currentUser.userId, otherUser.userId).distinct()
            val payload = mapOf(
                "chatType" to "direct",
                "title" to "",
                "description" to "",
                "avatarId" to otherUser.avatarId.ifBlank { "avatar_emerald" },
                "participantUids" to participants,
                "adminUids" to emptyList<String>(),
                "createdBy" to uid,
                "lastMessageText" to "Started an encrypted conversation",
                "lastMessageSenderId" to uid,
                "lastMessageType" to "system",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "typingUids" to emptyList<String>(),
                "pinnedByUids" to emptyList<String>(),
                "mutedByUids" to emptyList<String>(),
                "archivedByUids" to emptyList<String>(),
                "unreadCounts" to emptyMap<String, Long>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(payload).await()
        }
        chatId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "chats")
    }

    suspend fun createGroupChat(
        currentUser: UserProfile,
        title: String,
        description: String,
        memberUids: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val participants = (listOf(uid) + memberUids).distinct().take(50)
        require(participants.size >= 2) { "Select at least one member to create a group." }
        val docRef = db.collection("chats").document()
        val safeTitle = title.trim().ifBlank { "Alap Group" }.take(60)
        val safeDesc = description.trim().take(200)
        val unreadMap = participants.filter { it != uid }.associateWith { 1L }

        val chatPayload = mapOf(
            "chatType" to "group",
            "title" to safeTitle,
            "description" to safeDesc,
            "avatarId" to "group_default",
            "participantUids" to participants,
            "adminUids" to listOf(uid),
            "createdBy" to uid,
            "lastMessageText" to "${currentUser.displayName} created group \"$safeTitle\"",
            "lastMessageSenderId" to uid,
            "lastMessageType" to "system",
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "typingUids" to emptyList<String>(),
            "pinnedByUids" to emptyList<String>(),
            "mutedByUids" to emptyList<String>(),
            "archivedByUids" to emptyList<String>(),
            "unreadCounts" to unreadMap,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(chatPayload).await()

        val systemMsgRef = docRef.collection("messages").document()
        val systemMsgPayload = mapOf(
            "chatId" to docRef.id,
            "senderId" to uid,
            "senderName" to currentUser.displayName.ifBlank { "Alap User" }.take(60),
            "text" to "${currentUser.displayName} created \"$safeTitle\"",
            "messageType" to "system",
            "isForwarded" to false,
            "isEdited" to false,
            "isDeleted" to false,
            "participantUids" to participants,
            "readByUids" to listOf(uid),
            "starredByUids" to emptyList<String>(),
            "reactions" to emptyMap<String, String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        systemMsgRef.set(systemMsgPayload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "chats")
    }

    suspend fun sendMessage(
        chat: Chat,
        senderName: String,
        text: String,
        messageType: String = "text",
        stickerId: String? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
        replyTo: ChatMessage? = null,
        isForwarded: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
        val cleanText = text.trim().take(4000)
        val preview = when (messageType) {
            "sticker" -> AlapConstants.STICKER_OPTIONS.firstOrNull { it.first == stickerId }?.second ?: "Sticker"
            "poll" -> "Poll: ${pollQuestion.orEmpty().ifBlank { cleanText }}"
            else -> cleanText
        }.take(300)

        val messagePayload = mutableMapOf<String, Any?>(
            "chatId" to chat.id,
            "senderId" to uid,
            "senderName" to senderName.ifBlank { "Alap User" }.take(60),
            "text" to cleanText,
            "messageType" to messageType,
            "stickerId" to stickerId,
            "pollQuestion" to pollQuestion?.trim()?.take(200),
            "pollOptions" to pollOptions?.map { it.trim().take(60) }?.filter { it.isNotBlank() }?.take(6),
            "pollVotes" to if (messageType == "poll") emptyMap<String, Long>() else null,
            "replyToMessageId" to replyTo?.id?.takeIf { it.isNotBlank() },
            "replyToSenderName" to replyTo?.senderName?.take(60)?.takeIf { it.isNotBlank() },
            "replyToPreview" to replyTo?.let {
                if (it.isDeleted) "Deleted message" else it.text.ifBlank { "Sticker" }
            }?.take(160),
            "isForwarded" to isForwarded,
            "isEdited" to false,
            "isDeleted" to false,
            "participantUids" to chat.participantUids,
            "readByUids" to listOf(uid),
            "starredByUids" to emptyList<String>(),
            "reactions" to emptyMap<String, String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        val updatedUnread = chat.unreadCounts.toMutableMap()
        chat.participantUids.forEach { participantUid ->
            if (participantUid == uid) {
                updatedUnread[participantUid] = 0L
            } else {
                val prev = (updatedUnread[participantUid] as? Number)?.toLong() ?: 0L
                updatedUnread[participantUid] = prev + 1L
            }
        }
        val updatedTyping = chat.typingUids.filter { it != uid }

        val batch = db.batch()
        batch.set(msgRef, messagePayload)
        batch.update(
            db.collection("chats").document(chat.id),
            mapOf(
                "lastMessageText" to preview,
                "lastMessageSenderId" to uid,
                "lastMessageType" to messageType,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadCounts" to updatedUnread,
                "typingUids" to updatedTyping,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        msgRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}/messages")
    }

    suspend fun votePoll(chatId: String, message: ChatMessage, optionIndex: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updated = (message.pollVotes ?: emptyMap()).toMutableMap()
        updated[uid] = optionIndex.toLong()
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "pollVotes" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun setChatDisappearingHours(chatId: String, hours: Long): Result<Unit> = runCatching {
        requireUserId()
        val safeHours = if (hours in listOf(0L, 24L, 168L)) hours else 0L
        db.collection("chats").document(chatId).update(
            mapOf(
                "disappearingHours" to safeHours,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
    }

    suspend fun markChatAndMessagesRead(
        chat: Chat,
        messages: List<ChatMessage>,
        hideReadReceipts: Boolean = false
    ) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val unreadCount = chat.unreadCountFor(uid)
            val unreadMessages = if (hideReadReceipts) emptyList()
            else messages.filter { uid !in it.readByUids && it.senderId != uid }
            if (unreadCount == 0 && unreadMessages.isEmpty()) return

            val batch = db.batch()
            if (unreadCount > 0) {
                val updatedUnread = chat.unreadCounts.toMutableMap()
                updatedUnread[uid] = 0L
                batch.update(
                    db.collection("chats").document(chat.id),
                    mapOf(
                        "unreadCounts" to updatedUnread,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            unreadMessages.take(25).forEach { msg ->
                val newReaders = (msg.readByUids + uid).distinct()
                batch.update(
                    db.collection("chats").document(chat.id).collection("messages").document(msg.id),
                    mapOf(
                        "readByUids" to newReaders,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
        }
    }

    /**
     * Zero-server-bloat relay cleanup:
     * Once a message has been saved into local Room DB and read by all chat participants,
     * its cloud relay copy can be safely deleted from Firestore to keep server usage minimal.
     */
    suspend fun purgeDeliveredRelayMessages(chat: Chat, messages: List<ChatMessage>): Int {
        if (auth.currentUser == null || chat.participantUids.isEmpty()) return 0
        val delivered = messages.filter { msg ->
            msg.id.isNotBlank() &&
                msg.participantUids.isNotEmpty() &&
                msg.readByUids.containsAll(msg.participantUids) &&
                msg.starredByUids.isEmpty()
        }
        if (delivered.isEmpty()) return 0
        return try {
            val batch = db.batch()
            val toDelete = delivered.take(20)
            toDelete.forEach { msg ->
                batch.delete(
                    db.collection("chats").document(chat.id).collection("messages").document(msg.id)
                )
            }
            batch.commit().await()
            toDelete.size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updatedReactions = message.reactions.toMutableMap()
        if (updatedReactions[uid] == emoji) {
            updatedReactions.remove(uid)
        } else {
            updatedReactions[uid] = emoji
        }
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "reactions" to updatedReactions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun toggleStarMessage(chatId: String, message: ChatMessage): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updatedStars = if (uid in message.starredByUids) {
            message.starredByUids - uid
        } else {
            (message.starredByUids + uid).distinct()
        }
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "starredByUids" to updatedStars,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun editMessage(chatId: String, message: ChatMessage, newText: String): Result<Unit> = runCatching {
        requireUserId()
        val clean = newText.trim().take(4000)
        require(clean.isNotBlank())
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "text" to clean,
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun softDeleteMessage(chatId: String, message: ChatMessage): Result<Unit> = runCatching {
        requireUserId()
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "text" to "This message was deleted",
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun togglePinChat(chat: Chat): Result<Unit> = toggleChatUidList(chat, "pinnedByUids", chat.pinnedByUids)
    suspend fun toggleMuteChat(chat: Chat): Result<Unit> = toggleChatUidList(chat, "mutedByUids", chat.mutedByUids)
    suspend fun toggleArchiveChat(chat: Chat): Result<Unit> = toggleChatUidList(chat, "archivedByUids", chat.archivedByUids)

    private suspend fun toggleChatUidList(chat: Chat, field: String, currentList: List<String>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updated = if (uid in currentList) currentList - uid else (currentList + uid).distinct()
        db.collection("chats").document(chat.id).update(
            mapOf(
                field to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
    }

    suspend fun setTyping(chat: Chat, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val alreadyTyping = uid in chat.typingUids
        if (alreadyTyping == isTyping) return
        val updated = if (isTyping) (chat.typingUids + uid).distinct() else chat.typingUids - uid
        try {
            db.collection("chats").document(chat.id).update(
                mapOf(
                    "typingUids" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Non-critical ephemeral typing indicator
        }
    }

    suspend fun updateGroupInfo(chat: Chat, title: String, description: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("chats").document(chat.id).update(
            mapOf(
                "title" to title.trim().ifBlank { chat.title }.take(60),
                "description" to description.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
    }

    suspend fun addGroupMember(chat: Chat, memberUid: String): Result<Unit> = runCatching {
        requireUserId()
        if (memberUid in chat.participantUids) return@runCatching
        val updated = (chat.participantUids + memberUid).distinct().take(50)
        db.collection("chats").document(chat.id).update(
            mapOf(
                "participantUids" to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
    }

    suspend fun leaveGroup(chat: Chat): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updatedParticipants = chat.participantUids - uid
        val updatedAdmins = chat.adminUids - uid
        db.collection("chats").document(chat.id).update(
            mapOf(
                "participantUids" to updatedParticipants,
                "adminUids" to updatedAdmins,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}")
    }

    // --- 24h Status Updates ---

    fun observeStatuses(): Flow<List<StatusUpdate>> = flow {
        val uid = requireUserId()
        val path = "statuses"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(StatusUpdate::class.java, ServerTimestampBehavior.ESTIMATE)
                        .filter { it.expiresAtEpochMs > now }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleListenerError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun postStatus(author: UserProfile, text: String, styleKey: String): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("statuses").document()
        val safeStyle = if (styleKey in AlapConstants.STATUS_STYLES) styleKey else "emerald"
        val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        val payload = mapOf(
            "userId" to uid,
            "authorName" to author.displayName.ifBlank { "Alap User" }.take(60),
            "authorAvatarId" to author.avatarId.ifBlank { "avatar_emerald" }.take(40),
            "text" to text.trim().take(500),
            "styleKey" to safeStyle,
            "isPublic" to true,
            "viewerUids" to emptyList<String>(),
            "expiresAtEpochMs" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "statuses")
    }

    suspend fun markStatusViewed(status: StatusUpdate) {
        val uid = auth.currentUser?.uid ?: return
        if (uid in status.viewerUids || status.userId == uid) return
        try {
            db.collection("statuses").document(status.id).update(
                mapOf(
                    "viewerUids" to (status.viewerUids + uid).distinct(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Non-critical
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("statuses").document(statusId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "statuses/$statusId")
    }

    // --- Starred Messages across user's chats ---

    fun observeStarredMessagesForChats(chats: List<Chat>, uid: String): Flow<List<ChatMessage>> {
        if (chats.isEmpty()) return flow { emit(emptyList()) }
        val flows = chats.take(15).map { chat ->
            observeMessages(chat.id, uid).map { list ->
                list.map { it.message }.filter { uid in it.starredByUids && !it.isDeleted }
            }
        }
        return combine(flows) { arrays ->
            arrays.flatMap { it }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    private fun handleListenerError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
