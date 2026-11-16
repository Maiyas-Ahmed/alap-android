package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AlapConstants
import com.example.data.model.ChatMessage
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.ui.components.StudentBadge
import com.example.ui.components.TeacherBadge
import com.example.ui.components.VerifiedBadge
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    myPrivateInfo: UserPrivateInfo?,
    currentEmail: String,
    notificationsEnabled: Boolean,
    floatingGlassEnabled: Boolean,
    screenSecurityEnabled: Boolean,
    hasAppLockPin: Boolean,
    localOnlyRelayMode: Boolean = true,
    localStoredMessageCount: Int = 0,
    studyFocusMinutesTotal: Int = 0,
    studySessionsCompleted: Int = 0,
    usersMap: Map<String, UserProfile>,
    starredCount: Int,
    onBack: () -> Unit,
    onOpenStarredMessages: () -> Unit,
    onSaveProfileAndPrivacy: (
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
        hideReadReceipts: Boolean,
        isDiscoverable: Boolean,
        isStudent: Boolean,
        studentClass: String,
        studentSubject: String,
        birthYear: Long,
        accountType: String,
        teacherSubject: String
    ) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onFloatingGlassToggle: (Boolean) -> Unit,
    onScreenSecurityToggle: (Boolean) -> Unit,
    onLocalOnlyRelayToggle: (Boolean) -> Unit = {},
    onExportBackup: (Uri) -> Unit = {},
    onRestoreBackup: (Uri) -> Unit = {},
    onRestoreAutoSnapshot: () -> Unit = {},
    onSetAppLockPin: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeSection by remember { mutableStateOf<SettingsSection?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showAppPinDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(NotificationHelper.hasOverlayPermission(context)) }

    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = NotificationHelper.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val displayAlapId = myPrivateInfo?.alapId?.takeIf { it.isNotBlank() }
        ?: myProfile?.publicUserId?.takeIf { it.isNotBlank() }
        ?: "ALAP-${currentUserId.take(6).uppercase()}"
    val displayPhone = myPrivateInfo?.phoneNumber?.takeIf { it.isNotBlank() }
        ?: myProfile?.publicPhoneNumber?.takeIf { it.isNotBlank() }
        ?: "Not added yet"

    val hidePhone = myProfile?.hidePhoneNumber ?: true
    val hideUserId = myProfile?.hideUserId ?: false
    val hideOnline = myProfile?.hideOnlineStatus ?: false
    val hideAbout = myProfile?.hideAbout ?: false
    val hideReadReceipts = myProfile?.hideReadReceipts ?: false
    val isDiscoverable = myProfile?.isDiscoverable ?: true
    val blockedUids = myProfile?.blockedUids ?: emptyList()

    val saveQuickPrivacy = {
            pHidePhone: Boolean,
            pHideUserId: Boolean,
            pHideOnline: Boolean,
            pHideAbout: Boolean,
            pHideReceipts: Boolean,
            pDiscoverable: Boolean ->
        onSaveProfileAndPrivacy(
            myProfile?.displayName ?: "Alap User",
            myProfile?.handle ?: "alap",
            myPrivateInfo?.phoneNumber ?: myProfile?.publicPhoneNumber ?: "",
            displayAlapId,
            myProfile?.about ?: "Hey there! I am using Alap.",
            myProfile?.avatarId ?: AlapConstants.AVATAR_KEYS.first(),
            pHidePhone,
            pHideUserId,
            pHideOnline,
            pHideAbout,
            pHideReceipts,
            pDiscoverable,
            myProfile?.isStudent ?: false,
            myProfile?.studentClass ?: "",
            myProfile?.studentSubject ?: "",
            myPrivateInfo?.birthYear ?: 0L,
            myProfile?.accountType ?: "personal",
            myProfile?.teacherSubject ?: ""
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = activeSection?.title ?: "Settings",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (activeSection != null) activeSection = null else onBack()
                            },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeSection,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settings_section_transition",
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) { section ->
            when (section) {
                null -> {
                    SettingsHubContent(
                        myProfile = myProfile,
                        hideOnline = hideOnline,
                        blockedCount = blockedUids.size,
                        hasAppLockPin = hasAppLockPin,
                        screenSecurityEnabled = screenSecurityEnabled,
                        notificationsEnabled = notificationsEnabled,
                        floatingGlassEnabled = floatingGlassEnabled,
                        starredCount = starredCount,
                        localOnlyRelayMode = localOnlyRelayMode,
                        localStoredMessageCount = localStoredMessageCount,
                        studyFocusMinutesTotal = studyFocusMinutesTotal,
                        studySessionsCompleted = studySessionsCompleted,
                        onEditProfile = { showEditProfile = true },
                        onOpenSection = { activeSection = it },
                        onOpenStarredMessages = onOpenStarredMessages,
                        onSignOut = onSignOut
                    )
                }

                SettingsSection.ACCOUNT -> AccountSettingsSection(
                    currentEmail = currentEmail,
                    displayPhone = displayPhone,
                    displayAlapId = displayAlapId,
                    hidePhone = hidePhone,
                    hideUserId = hideUserId,
                    onEditProfile = { showEditProfile = true }
                )

                SettingsSection.PRIVACY -> PrivacySettingsSection(
                    hidePhone = hidePhone,
                    hideUserId = hideUserId,
                    hideOnline = hideOnline,
                    hideAbout = hideAbout,
                    hideReadReceipts = hideReadReceipts,
                    isDiscoverable = isDiscoverable,
                    blockedCount = blockedUids.size,
                    onUpdatePrivacy = saveQuickPrivacy,
                    onOpenBlockedUsers = { showBlockedUsersDialog = true }
                )

                SettingsSection.SECURITY -> SecuritySettingsSection(
                    hasAppLockPin = hasAppLockPin,
                    screenSecurityEnabled = screenSecurityEnabled,
                    onOpenAppPinDialog = { showAppPinDialog = true },
                    onScreenSecurityToggle = onScreenSecurityToggle
                )

                SettingsSection.NOTIFICATIONS -> NotificationsSettingsSection(
                    notificationsEnabled = notificationsEnabled,
                    floatingGlassEnabled = floatingGlassEnabled,
                    hasOverlayPermission = hasOverlayPermission,
                    onNotificationsToggle = onNotificationsToggle,
                    onFloatingGlassToggle = onFloatingGlassToggle
                )

                SettingsSection.STORAGE -> StorageSettingsSection(
                    localOnlyRelayMode = localOnlyRelayMode,
                    localStoredMessageCount = localStoredMessageCount,
                    onLocalOnlyRelayToggle = onLocalOnlyRelayToggle,
                    onExportBackup = onExportBackup,
                    onRestoreBackup = onRestoreBackup,
                    onRestoreAutoSnapshot = onRestoreAutoSnapshot
                )

                SettingsSection.APP_INFO -> AppInfoSettingsSection()
            }
        }
    }

    if (showEditProfile) {
        val isLockedStudent = myProfile?.isLockedStudentAccount() == true
        val isTeacherAccount = myProfile?.isTeacher == true || myProfile?.accountType == "teacher"
        var name by remember { mutableStateOf(myProfile?.displayName ?: "") }
        var handle by remember { mutableStateOf(myProfile?.handle ?: "") }
        var phone by remember { mutableStateOf(if (displayPhone == "Not added yet") "" else displayPhone) }
        var alapId by remember { mutableStateOf(displayAlapId) }
        var about by remember { mutableStateOf(myProfile?.about ?: "") }
        var avatarId by remember { mutableStateOf(myProfile?.avatarId ?: AlapConstants.AVATAR_KEYS.first()) }
        var isStudent by remember { mutableStateOf(if (isLockedStudent) true else (myProfile?.isStudent ?: false)) }
        var studentClass by remember { mutableStateOf(myProfile?.studentClass ?: "") }
        var studentSubject by remember { mutableStateOf(myProfile?.studentSubject ?: "") }
        var teacherSubject by remember { mutableStateOf(myProfile?.teacherSubject ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            containerColor = Color.White,
            title = { Text("Edit Profile Details") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 20) phone = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = alapId,
                        onValueChange = { raw ->
                            alapId = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(24)
                        },
                        label = { Text("Alap User ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { if (it.length <= 30) handle = it },
                        label = { Text("Username Handle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = about,
                        onValueChange = { if (it.length <= 140) about = it },
                        label = { Text("About") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isTeacherAccount) {
                        HorizontalDivider()
                        OutlinedTextField(
                            value = teacherSubject,
                            onValueChange = { if (it.length <= 80) teacherSubject = it },
                            label = { Text("Teacher Subject / Specialty") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_teacher_subject_input")
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLockedStudent) "Student Mode (ST Locked)" else "Student Mode (ST Badge)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isLockedStudent) {
                                    "Locked Student Account: cannot switch to Personal mode"
                                } else {
                                    "Show ST badge & academic batch/subjects"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLockedStudent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = if (isLockedStudent) true else isStudent,
                            enabled = !isLockedStudent,
                            onCheckedChange = { if (!isLockedStudent) isStudent = it },
                            modifier = Modifier.testTag("edit_student_switch")
                        )
                    }
                    if (isStudent || isLockedStudent) {
                        OutlinedTextField(
                            value = studentClass,
                            onValueChange = { if (it.length <= 40) studentClass = it },
                            label = { Text("Class / Batch (e.g. HSC-26)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = studentSubject,
                            onValueChange = { if (it.length <= 80) studentSubject = it },
                            label = { Text("Focus Subjects (e.g. Physics, Math)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("Avatar Accent", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AlapConstants.AVATAR_KEYS.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(
                                        width = if (avatarId == key) 2.5.dp else 1.dp,
                                        color = if (avatarId == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { avatarId = key }
                                    .padding(2.dp)
                            ) {
                                AlapAvatar(avatarId = key, displayName = name.ifBlank { "A" }, size = 38.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveProfileAndPrivacy(
                            name,
                            handle,
                            phone,
                            alapId,
                            about,
                            avatarId,
                            hidePhone,
                            hideUserId,
                            hideOnline,
                            hideAbout,
                            hideReadReceipts,
                            isDiscoverable,
                            if (isLockedStudent) true else isStudent,
                            studentClass,
                            studentSubject,
                            myPrivateInfo?.birthYear ?: 0L,
                            myProfile?.accountType ?: "personal",
                            teacherSubject
                        )
                        showEditProfile = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppPinDialog) {
        PinSetupDialog(
            title = "App Lock PIN",
            subtitle = "Set a 4 to 6 digit PIN to lock Alap whenever opened.",
            hasExisting = hasAppLockPin,
            onDismiss = { showAppPinDialog = false },
            onSave = { pin ->
                onSetAppLockPin(pin)
                showAppPinDialog = false
            }
        )
    }

    if (showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            containerColor = Color.White,
            title = { Text("Blocked Contacts") },
            text = {
                if (blockedUids.isEmpty()) {
                    Text(
                        "You haven't blocked anyone yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        blockedUids.forEach { bUid ->
                            val user = usersMap[bUid]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user?.displayName ?: "User ${bUid.take(6)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onUnblockUser(bUid) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsHubContent(
    myProfile: UserProfile?,
    hideOnline: Boolean,
    blockedCount: Int,
    hasAppLockPin: Boolean,
    screenSecurityEnabled: Boolean,
    notificationsEnabled: Boolean,
    floatingGlassEnabled: Boolean,
    starredCount: Int,
    localOnlyRelayMode: Boolean,
    localStoredMessageCount: Int,
    studyFocusMinutesTotal: Int,
    studySessionsCompleted: Int,
    onEditProfile: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
    onOpenStarredMessages: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // WhatsApp-style Profile Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditProfile)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlapAvatar(
                    avatarId = myProfile?.avatarId ?: "avatar_emerald",
                    displayName = myProfile?.displayName ?: "Alap User",
                    isOnline = !hideOnline,
                    size = 62.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = myProfile?.displayName ?: "Alap User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (myProfile?.isVerified == true) {
                            Spacer(modifier = Modifier.width(5.dp))
                            VerifiedBadge()
                        }
                        if (myProfile?.isTeacher == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TeacherBadge()
                        }
                        if (myProfile?.isStudent == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            StudentBadge()
                        }
                    }
                    Text(
                        text = buildString {
                            append("@${myProfile?.handle ?: "alap"}")
                            append(" • ")
                            append(
                                when {
                                    myProfile?.isTeacher == true -> "Verified Teacher"
                                    myProfile?.isLockedStudentAccount() == true -> "Student Account (Locked)"
                                    else -> "Personal Account (18+)"
                                }
                            )
                            if (myProfile?.isTeacher == true && myProfile.teacherSubject.isNotBlank()) {
                                append(" • ${myProfile.teacherSubject}")
                            } else if (myProfile?.isStudent == true && myProfile.studentClass.isNotBlank()) {
                                append(" • ${myProfile.studentClass}")
                            }
                            if (myProfile?.isTeacher == true && myProfile.teacherPlanName.isNotBlank()) {
                                append(" • Plan: ${myProfile.teacherPlanName}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = myProfile?.about ?: "Hey there! I am using Alap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (myProfile?.isStudent == true || studyFocusMinutesTotal > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Student stats",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Student Focus Progress (Offline)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$studyFocusMinutesTotal focus mins • $studySessionsCompleted session(s) completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            // Organized WhatsApp-style Sections
            SettingsHubRow(
                icon = Icons.Filled.Key,
                title = "Account",
                subtitle = "Verified Gmail, mobile number, Alap User ID",
                onClick = { onOpenSection(SettingsSection.ACCOUNT) },
                modifier = Modifier.testTag("settings_section_account")
            )
            SettingsHubRow(
                icon = Icons.Filled.Lock,
                title = "Privacy",
                subtitle = "Number, User ID, last seen, read receipts, blocked ($blockedCount)",
                onClick = { onOpenSection(SettingsSection.PRIVACY) },
                modifier = Modifier.testTag("settings_section_privacy")
            )
            SettingsHubRow(
                icon = Icons.Filled.Security,
                title = "Security & App Lock",
                subtitle = buildString {
                    append(if (hasAppLockPin) "App Lock active" else "PIN & biometric off")
                    append(" • ")
                    append(if (screenSecurityEnabled) "Screenshots blocked" else "Screen privacy")
                },
                onClick = { onOpenSection(SettingsSection.SECURITY) },
                modifier = Modifier.testTag("settings_section_security")
            )
            SettingsHubRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = when {
                    notificationsEnabled && floatingGlassEnabled -> "Heads-up alerts & floating glass popups on"
                    notificationsEnabled -> "Heads-up alerts on • Floating popup optional"
                    else -> "Alerts paused • Tap to configure"
                },
                onClick = { onOpenSection(SettingsSection.NOTIFICATIONS) },
                modifier = Modifier.testTag("settings_section_notifications")
            )
            SettingsHubRow(
                icon = Icons.Filled.Star,
                title = "Starred Messages",
                subtitle = "$starredCount saved offline & synced",
                onClick = onOpenStarredMessages,
                modifier = Modifier.testTag("starred_messages_button")
            )
            SettingsHubRow(
                icon = Icons.Filled.Storage,
                title = "Storage and Data",
                subtitle = "${if (localOnlyRelayMode) "Local-only relay" else "Cloud + local"} • $localStoredMessageCount cached • Backup",
                onClick = { onOpenSection(SettingsSection.STORAGE) },
                modifier = Modifier.testTag("settings_section_storage")
            )
            SettingsHubRow(
                icon = Icons.Filled.Info,
                title = "App Info & Portal",
                subtitle = "Alap v${AlapConstants.CURRENT_APP_VERSION} (Build ${AlapConstants.CURRENT_APP_VERSION_CODE}) • Official websites",
                onClick = { onOpenSection(SettingsSection.APP_INFO) },
                modifier = Modifier.testTag("settings_section_app_info")
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("sign_out_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Sign out from this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "from\nAlap • v${AlapConstants.CURRENT_APP_VERSION}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun PinSetupDialog(
    title: String,
    subtitle: String,
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { raw -> pin = raw.filter { it.isDigit() }.take(6) },
                    label = { Text("4-6 digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(pin) },
                enabled = pin.length in 4..6
            ) { Text("Save PIN") }
        },
        dismissButton = {
            Row {
                if (hasExisting) {
                    TextButton(onClick = { onSave("") }) {
                        Text("Disable", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text("Starred Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No starred messages yet.\nTap any message in a chat to star it for quick offline access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { "${it.chatId}_${it.id}" }) { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(msg.chatId) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = AlapDateFormatter.formatChatTime(msg.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
