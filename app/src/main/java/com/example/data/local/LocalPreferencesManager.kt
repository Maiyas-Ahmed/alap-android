package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.alapDataStore: DataStore<Preferences> by preferencesDataStore(name = "alap_prefs")

class LocalPreferencesManager(private val context: Context) {

    val verifiedOtpUid: Flow<String> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_VERIFIED_OTP_UID] ?: ""
    }

    val notificationsEnabled: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val floatingGlassEnabled: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_FLOATING_GLASS_ENABLED] ?: false
    }

    val wakeScreenOnNotification: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_WAKE_SCREEN_ON_NOTIF] ?: true
    }

    val pendingGuestLogin: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_PENDING_GUEST_LOGIN] ?: false
    }

    val screenSecurityEnabled: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_SCREEN_SECURITY_ENABLED] ?: false
    }

    val appLockPinHash: Flow<String> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_PIN_HASH] ?: ""
    }

    val studyFocusMinutesTotal: Flow<Long> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_STUDY_FOCUS_MINUTES_TOTAL] ?: 0L
    }

    val studySessionsCompleted: Flow<Long> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_STUDY_SESSIONS_COMPLETED] ?: 0L
    }

    val dismissedAnnouncementId: Flow<String> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_DISMISSED_ANNOUNCEMENT_ID] ?: ""
    }

    val lastNotifiedAnnouncementId: Flow<String> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_LAST_NOTIFIED_ANNOUNCEMENT_ID] ?: ""
    }

    val permissionsOnboardingDone: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_PERMISSIONS_ONBOARDING_DONE] ?: false
    }

    val localOnlyRelayMode: Flow<Boolean> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_LOCAL_ONLY_RELAY_MODE] ?: true
    }

    val lastBgSyncEpochMs: Flow<Long> = context.alapDataStore.data.map { prefs ->
        prefs[KEY_LAST_BG_SYNC_MS] ?: 0L
    }

    suspend fun setVerifiedOtpUid(uid: String) {
        context.alapDataStore.edit { it[KEY_VERIFIED_OTP_UID] = uid }
    }

    suspend fun clearVerifiedOtpUid() {
        context.alapDataStore.edit { it.remove(KEY_VERIFIED_OTP_UID) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.alapDataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setFloatingGlassEnabled(enabled: Boolean) {
        context.alapDataStore.edit { it[KEY_FLOATING_GLASS_ENABLED] = enabled }
    }

    suspend fun setWakeScreenOnNotification(enabled: Boolean) {
        context.alapDataStore.edit { it[KEY_WAKE_SCREEN_ON_NOTIF] = enabled }
    }

    suspend fun setPendingGuestLogin(pending: Boolean) {
        context.alapDataStore.edit { it[KEY_PENDING_GUEST_LOGIN] = pending }
    }

    suspend fun setScreenSecurityEnabled(enabled: Boolean) {
        context.alapDataStore.edit { it[KEY_SCREEN_SECURITY_ENABLED] = enabled }
    }

    suspend fun setAppLockPin(rawPin: String) {
        context.alapDataStore.edit {
            if (rawPin.isBlank()) it.remove(KEY_APP_LOCK_PIN_HASH)
            else it[KEY_APP_LOCK_PIN_HASH] = hashPin("app", rawPin)
        }
    }

    fun verifyAppPin(rawPin: String, storedHash: String): Boolean =
        storedHash.isNotBlank() && hashPin("app", rawPin) == storedHash

    suspend fun recordCompletedStudyMinutes(minutes: Int) {
        if (minutes <= 0) return
        context.alapDataStore.edit { prefs ->
            val currentMin = prefs[KEY_STUDY_FOCUS_MINUTES_TOTAL] ?: 0L
            val currentSessions = prefs[KEY_STUDY_SESSIONS_COMPLETED] ?: 0L
            prefs[KEY_STUDY_FOCUS_MINUTES_TOTAL] = currentMin + minutes.toLong()
            prefs[KEY_STUDY_SESSIONS_COMPLETED] = currentSessions + 1L
        }
    }

    suspend fun dismissAnnouncement(announcementId: String) {
        context.alapDataStore.edit { it[KEY_DISMISSED_ANNOUNCEMENT_ID] = announcementId }
    }

    suspend fun markAnnouncementNotified(announcementId: String) {
        context.alapDataStore.edit { it[KEY_LAST_NOTIFIED_ANNOUNCEMENT_ID] = announcementId }
    }

    suspend fun setPermissionsOnboardingDone(done: Boolean) {
        context.alapDataStore.edit { it[KEY_PERMISSIONS_ONBOARDING_DONE] = done }
    }

    suspend fun setLocalOnlyRelayMode(enabled: Boolean) {
        context.alapDataStore.edit { it[KEY_LOCAL_ONLY_RELAY_MODE] = enabled }
    }

    suspend fun setLastBgSyncEpochMs(epochMs: Long) {
        context.alapDataStore.edit { it[KEY_LAST_BG_SYNC_MS] = epochMs }
    }

    fun observeDraft(chatId: String): Flow<String> {
        val key = stringPreferencesKey("draft_$chatId")
        return context.alapDataStore.data.map { prefs -> prefs[key] ?: "" }
    }

    suspend fun saveDraft(chatId: String, text: String) {
        val key = stringPreferencesKey("draft_$chatId")
        context.alapDataStore.edit { prefs ->
            if (text.isBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = text
            }
        }
    }

    private fun hashPin(scope: String, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("alap:$scope:${pin.trim()}".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val KEY_VERIFIED_OTP_UID = stringPreferencesKey("verified_otp_uid")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_FLOATING_GLASS_ENABLED = booleanPreferencesKey("floating_glass_enabled")
        val KEY_SCREEN_SECURITY_ENABLED = booleanPreferencesKey("screen_security_enabled")
        val KEY_APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val KEY_STUDY_FOCUS_MINUTES_TOTAL = longPreferencesKey("study_focus_minutes_total")
        val KEY_STUDY_SESSIONS_COMPLETED = longPreferencesKey("study_sessions_completed")
        val KEY_DISMISSED_ANNOUNCEMENT_ID = stringPreferencesKey("dismissed_announcement_id")
        val KEY_LAST_NOTIFIED_ANNOUNCEMENT_ID = stringPreferencesKey("last_notified_announcement_id")
        val KEY_PERMISSIONS_ONBOARDING_DONE = booleanPreferencesKey("permissions_onboarding_done")
        val KEY_LOCAL_ONLY_RELAY_MODE = booleanPreferencesKey("local_only_relay_mode")
        val KEY_LAST_BG_SYNC_MS = longPreferencesKey("last_bg_sync_ms")
        val KEY_WAKE_SCREEN_ON_NOTIF = booleanPreferencesKey("wake_screen_on_notif")
        val KEY_PENDING_GUEST_LOGIN = booleanPreferencesKey("pending_guest_login")
    }
}
