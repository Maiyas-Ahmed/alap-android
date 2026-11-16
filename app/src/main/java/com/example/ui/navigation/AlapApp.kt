package com.example.ui.navigation

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.firestore.AlapFirestoreProvider
import com.example.data.local.LocalBackupManager
import com.example.data.local.LocalPreferencesManager
import com.example.data.local.room.AlapRoomDatabase
import com.example.data.model.AlapConstants
import com.example.data.model.UiState
import com.example.data.repository.AlapRepository
import com.example.service.GlassOverlayService
import com.example.ui.components.GlassNotificationBanner
import com.example.ui.components.InAppGlassBannerData
import com.example.ui.components.OfflineAndMaintenanceStrip
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.OtpVerificationScreen
import com.example.ui.screens.PeopleTabScreen
import com.example.ui.screens.PermissionsOnboardingScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StarredMessagesScreen
import com.example.ui.screens.UpdatesTabScreen
import com.example.ui.theme.AlapTheme
import com.example.ui.viewmodel.AlapViewModel
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.util.ApkUpdateManager
import com.example.util.NetworkMonitor
import com.example.util.NotificationHelper
import com.example.util.UpdateDownloadState
import com.example.util.WebsiteUpdateChecker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private object AlapRoutes {
    const val HOME = "home"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"
    const val STARRED = "starred"

    fun chatRoute(chatId: String) = "chat/$chatId"
}

@Composable
fun AlapRoot(
    initialOpenChatId: String? = null,
    onConsumeInitialOpenChatId: () -> Unit = {},
    onScreenSecurityChanged: (Boolean) -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Permanent pure White Mode
    AlapTheme {
        val user = currentUser
        if (user == null) {
            LaunchedEffect(Unit) { onScreenSecurityChanged(false) }
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedAlapShell(
                    uid = user.uid,
                    email = user.email ?: "",
                    isEmailVerified = user.isEmailVerified,
                    displayName = user.displayName ?: "Alap User",
                    initialOpenChatId = initialOpenChatId,
                    onConsumeInitialOpenChatId = onConsumeInitialOpenChatId,
                    onScreenSecurityChanged = onScreenSecurityChanged
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAlapShell(
    uid: String,
    email: String,
    isEmailVerified: Boolean,
    displayName: String,
    initialOpenChatId: String?,
    onConsumeInitialOpenChatId: () -> Unit,
    onScreenSecurityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val credentialManager = remember { CredentialManager.create(context) }

    val mainViewModel: AlapViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = AlapFirestoreProvider.getDatabase(app)
                val dao = AlapRoomDatabase.getInstance(app).alapLocalDao()
                AlapViewModel(
                    repository = AlapRepository(db),
                    prefs = LocalPreferencesManager(app),
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = uid,
                    currentUserEmail = email,
                    isEmailVerified = isEmailVerified,
                    initialDisplayName = displayName,
                    localDao = dao,
                    backupManager = LocalBackupManager(app, dao)
                )
            }
        }
    )

    LaunchedEffect(uid) {
        mainViewModel.registerPushNotifications(context)
    }

    var isAppInForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uid) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isAppInForeground = true
                    mainViewModel.setOnlineStatus(true)
                }
                Lifecycle.Event.ON_RESUME -> {
                    ApkUpdateManager.onHostResumed(context)
                }
                Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                    mainViewModel.setOnlineStatus(false)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val verifiedOtpUid by mainViewModel.verifiedOtpUid.collectAsStateWithLifecycle()
    val otpSending by mainViewModel.otpSending.collectAsStateWithLifecycle()
    val otpVerifying by mainViewModel.otpVerifying.collectAsStateWithLifecycle()
    val otpError by mainViewModel.otpError.collectAsStateWithLifecycle()
    val fallbackCode by mainViewModel.fallbackDeliveryCode.collectAsStateWithLifecycle()
    val profileSaving by mainViewModel.profileSaving.collectAsStateWithLifecycle()

    val myProfileState by mainViewModel.myProfileState.collectAsStateWithLifecycle()
    val myProfile by mainViewModel.myProfile.collectAsStateWithLifecycle()
    val myPrivateInfo by mainViewModel.myPrivateInfo.collectAsStateWithLifecycle()
    val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()
    val appConfig by mainViewModel.appConfig.collectAsStateWithLifecycle()
    val remoteVersionInfo by mainViewModel.remoteVersionInfo.collectAsStateWithLifecycle()
    val checkingForUpdate by mainViewModel.checkingForUpdate.collectAsStateWithLifecycle()
    val announcements by mainViewModel.activeAnnouncements.collectAsStateWithLifecycle()
    val dismissedId by mainViewModel.dismissedAnnouncementId.collectAsStateWithLifecycle()
    val lastNotifiedId by mainViewModel.lastNotifiedAnnouncementId.collectAsStateWithLifecycle()
    val notificationsEnabled by mainViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val floatingGlassEnabled by mainViewModel.floatingGlassEnabled.collectAsStateWithLifecycle()
    val screenSecurityEnabled by mainViewModel.screenSecurityEnabled.collectAsStateWithLifecycle()
    val permissionsOnboardingDone by mainViewModel.permissionsOnboardingDone.collectAsStateWithLifecycle()
    val localOnlyRelayMode by mainViewModel.localOnlyRelayMode.collectAsStateWithLifecycle()
    val localStoredMessageCount by mainViewModel.localStoredMessageCount.collectAsStateWithLifecycle()
    val appLockPinHash by mainViewModel.appLockPinHash.collectAsStateWithLifecycle()
    val studyFocusMinutesTotal by mainViewModel.studyFocusMinutesTotal.collectAsStateWithLifecycle()
    val studySessionsCompleted by mainViewModel.studySessionsCompleted.collectAsStateWithLifecycle()
    val chatsState by mainViewModel.myChats.collectAsStateWithLifecycle()
    val usersState by mainViewModel.discoverableUsers.collectAsStateWithLifecycle()
    val statusesState by mainViewModel.statuses.collectAsStateWithLifecycle()
    val starredMessages by mainViewModel.starredMessages.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(uid, isOnline) {
        if (isOnline) {
            mainViewModel.registerPushNotifications(context)
        }
    }

    val remoteInfo = remoteVersionInfo
    val effectiveLatestVersionCode = maxOf(
        appConfig.latestVersionCode,
        remoteInfo?.latestVersionCode ?: 0L
    )
    val effectiveLatestVersionName = WebsiteUpdateChecker.maxVersionString(
        appConfig.minAppVersion,
        remoteInfo?.latestVersionName ?: ""
    ).ifBlank { AlapConstants.CURRENT_APP_VERSION }
    val remoteIsNewer = remoteInfo != null &&
        remoteInfo.apkUrl.isNotBlank() &&
        (remoteInfo.latestVersionCode > appConfig.latestVersionCode ||
            WebsiteUpdateChecker.isVersionLower(appConfig.minAppVersion, remoteInfo.latestVersionName))
    val effectiveLatestApkUrl = if (remoteIsNewer) {
        remoteInfo?.apkUrl.orEmpty()
    } else {
        appConfig.latestApkUrl.ifBlank { remoteInfo?.apkUrl ?: "" }
    }
    val effectiveForceUpdate = appConfig.forceUpdateRequired ||
        (remoteInfo?.forceUpdateRequired == true)

    var isAppUnlocked by remember(uid) { mutableStateOf(false) }
    var inAppBanner by remember { mutableStateOf<InAppGlassBannerData?>(null) }

    LaunchedEffect(screenSecurityEnabled) {
        onScreenSecurityChanged(screenSecurityEnabled)
    }

    LaunchedEffect(floatingGlassEnabled) {
        if (floatingGlassEnabled && NotificationHelper.hasOverlayPermission(context)) {
            GlassOverlayService.startServiceIfPermitted(context)
        } else {
            GlassOverlayService.stopService(context)
        }
    }

    // Gate 1: Wait for DataStore OTP check, then enforce Gmail OTP or Inbox Link verification
    val currentOtpUid = verifiedOtpUid
    if (currentOtpUid == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (currentOtpUid != uid) {
        OtpVerificationScreen(
            email = email,
            isSending = otpSending,
            isVerifying = otpVerifying,
            errorMessage = otpError,
            infoMessage = actionMessage,
            fallbackCode = fallbackCode,
            onIssueCode = { mainViewModel.issueOtpCode(context) },
            onVerifyCode = { code -> mainViewModel.verifyOtpCode(code) },
            onSendFreeVerificationLink = { mainViewModel.sendFreeInboxVerificationEmail() },
            onCheckEmailLinkVerified = { mainViewModel.completeWithFirebaseEmailVerified() },
            onSwitchAccount = {
                mainViewModel.clearOtpSessionSync()
                signOut(credentialManager, scope)
            }
        )
        return
    }

    // Gate 2: Suspended user guard
    if (myProfile?.isSuspended == true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .safeDrawingPadding()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Account Suspended", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                Text(
                    "Your Alap account has been suspended by an administrator.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Button(onClick = {
                    mainViewModel.clearOtpSessionSync()
                    signOut(credentialManager, scope)
                }) {
                    Text("Sign Out")
                }
            }
        }
        return
    }

    // Gate 3: Force App Update Gate with Direct One-Tap APK Installer
    val needsForceUpdate = effectiveForceUpdate &&
        (WebsiteUpdateChecker.isVersionLower(AlapConstants.CURRENT_APP_VERSION, effectiveLatestVersionName) ||
            AlapConstants.CURRENT_APP_VERSION_CODE < effectiveLatestVersionCode)
    if (needsForceUpdate) {
        val updateState by ApkUpdateManager.state.collectAsStateWithLifecycle()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .safeDrawingPadding()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 440.dp)
            ) {
                Text("Update Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "নতুন ভার্সন ($effectiveLatestVersionName) পাওয়া গেছে। অ্যাপটি ব্যবহার চালিয়ে যেতে সরাসরি এখান থেকেই আপডেট করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                when (val s = updateState) {
                    is UpdateDownloadState.Downloading -> {
                        LinearProgressIndicator(progress = { s.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("ডাউনলোড হচ্ছে... ${s.progressPercent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Button(
                            onClick = { ApkUpdateManager.installApkFile(context, s.apkFile) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("এখনই ইনস্টল করুন (Install APK)") }
                    }
                    is UpdateDownloadState.NeedsInstallPermission -> {
                        Text(
                            text = "অটো আপডেট ইনস্টলের জন্য সেটিংস থেকে 'Allow from this source' অন করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { ApkUpdateManager.openInstallPermissionSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Install Permission দিন") }
                    }
                    is UpdateDownloadState.Error -> {
                        Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                ApkUpdateManager.openInBrowserFallback(
                                    context = context,
                                    url = s.fallbackUrl.ifBlank { AlapConstants.UPDATE_PORTAL_URL }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ব্রাউজার দিয়ে ডাউনলোড করুন")
                        }
                    }
                    UpdateDownloadState.Idle -> Unit
                }
                if (effectiveLatestApkUrl.isNotBlank() && updateState !is UpdateDownloadState.Downloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            ApkUpdateManager.startUpdateDownload(
                                context = context,
                                apkUrl = effectiveLatestApkUrl,
                                versionTag = effectiveLatestVersionName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("force_update_now_button")
                    ) {
                        Text("আপডেট করুন (Update Now)", fontWeight = FontWeight.Bold)
                    }
                } else if (effectiveLatestApkUrl.isBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { mainViewModel.refreshRemoteVersionInfo(silent = false) },
                        enabled = !checkingForUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (checkingForUpdate) "Checking..." else "Check Update Server Again")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    mainViewModel.clearOtpSessionSync()
                    signOut(credentialManager, scope)
                }) {
                    Text("Sign Out")
                }
            }
        }
        return
    }

    // Gate 4: Initial Profile & Privacy Setup
    when (val profileRes = myProfileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        is UiState.Success -> {
            val loadedProfile = profileRes.data
            if (loadedProfile == null || !loadedProfile.isProfileCompleted) {
                ProfileSetupScreen(
                    uid = uid,
                    email = email,
                    existingProfile = loadedProfile,
                    existingPrivateInfo = myPrivateInfo,
                    isSaving = profileSaving,
                    onSaveProfileAndPrivacy = { name, handle, phone, alapId, about, avatarId, hidePhone, hideUserId, hideOnline, hideAbout, hideReceipts, discoverable, isStudent, studentClass, studentSubject, birthYear, accountType, teacherSubject ->
                        mainViewModel.saveProfileAndPrivacy(
                            displayName = name,
                            handle = handle,
                            phoneNumber = phone,
                            alapId = alapId,
                            about = about,
                            avatarId = avatarId,
                            hidePhoneNumber = hidePhone,
                            hideUserId = hideUserId,
                            hideOnlineStatus = hideOnline,
                            hideAbout = hideAbout,
                            hideReadReceipts = hideReceipts,
                            isDiscoverable = discoverable,
                            isStudent = isStudent,
                            studentClass = studentClass,
                            studentSubject = studentSubject,
                            birthYear = birthYear,
                            accountType = accountType,
                            teacherSubject = teacherSubject,
                            markProfileCompleted = true
                        )
                    }
                )
                return
            }
        }

        is UiState.Error -> {
            // Fall through cleanly if offline cache unavailable on first boot
        }
    }

    // Gate 4b: Unified First-Launch Permissions Onboarding
    when (permissionsOnboardingDone) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }
        false -> {
            PermissionsOnboardingScreen(
                onContinue = { notifGranted, overlayGranted ->
                    mainViewModel.completePermissionsOnboarding(notifGranted, overlayGranted)
                }
            )
            return
        }
        true -> Unit
    }

    // Gate 5: Optional App Lock PIN / Biometric Gate
    if (appLockPinHash.isNotBlank() && !isAppUnlocked) {
        AppLockScreen(
            title = "Unlock Alap",
            subtitle = "Verify with your PIN or fingerprint to view chats.",
            hasPinConfigured = true,
            onVerifyPin = mainViewModel::verifyAppPin,
            onUnlocked = { isAppUnlocked = true }
        )
        return
    }

    val usersMap = remember(usersState) {
        ((usersState as? UiState.Success)?.data ?: emptyList()).associateBy { it.userId }
    }
    val allChats = (chatsState as? UiState.Success)?.data ?: emptyList()
    val blockedUids = myProfile?.blockedUids ?: emptyList()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activeChatId = navBackStackEntry?.arguments?.getString("chatId")

    // Deep-link navigation from notification or floating glass popup tap
    LaunchedEffect(initialOpenChatId) {
        if (!initialOpenChatId.isNullOrBlank()) {
            navController.navigate(AlapRoutes.chatRoute(initialOpenChatId)) {
                launchSingleTop = true
            }
            onConsumeInitialOpenChatId()
        }
    }

    // Dual Notification System: Incoming Messages Observer (Glass Banner / Floating Overlay / Heads-Up)
    var baselineChatTimes by remember(uid) { mutableStateOf<Map<String, Long>?>(null) }
    LaunchedEffect(allChats, activeChatId, isAppInForeground, notificationsEnabled, floatingGlassEnabled, blockedUids) {
        val currentTimes = allChats.associate { c ->
            c.id to (c.lastMessageAt?.toDate()?.time ?: 0L)
        }
        val prev = baselineChatTimes
        if (prev == null) {
            baselineChatTimes = currentTimes
            return@LaunchedEffect
        }

        val updatedChat = allChats.firstOrNull { c ->
            val oldTime = prev[c.id] ?: 0L
            val newTime = currentTimes[c.id] ?: 0L
            val otherUid = c.otherParticipantUid(uid)
            val isBlocked = otherUid != null && otherUid in blockedUids
            newTime > oldTime &&
                c.lastMessageSenderId != uid &&
                c.lastMessageSenderId.isNotBlank() &&
                !c.isMutedBy(uid) &&
                !isBlocked
        }
        baselineChatTimes = currentTimes

        if (updatedChat != null) {
            val senderName = if (updatedChat.chatType == "group") {
                updatedChat.title.ifBlank { "Group Chat" }
            } else {
                usersMap[updatedChat.lastMessageSenderId]?.displayName ?: "New Message"
            }
            val preview = updatedChat.lastMessageText.ifBlank { "Sent you a message" }

            if (isAppInForeground) {
                if (updatedChat.id != activeChatId) {
                    inAppBanner = InAppGlassBannerData(
                        id = "${updatedChat.id}_${System.currentTimeMillis()}",
                        badge = "Alap",
                        title = senderName,
                        body = preview,
                        chatId = updatedChat.id
                    )
                }
            } else {
                if (floatingGlassEnabled && NotificationHelper.hasOverlayPermission(context)) {
                    GlassOverlayService.showPopup(
                        context = context,
                        title = senderName,
                        body = preview,
                        badge = "Alap Message",
                        chatId = updatedChat.id
                    )
                } else if (notificationsEnabled) {
                    NotificationHelper.showIncomingMessageNotification(
                        context = context,
                        chatId = updatedChat.id,
                        senderName = senderName,
                        preview = preview
                    )
                }
            }
        }
    }

    // Trigger Dual Notification whenever Admin publishes a new active announcement for a newer/unmatched version
    val topAnnouncement = announcements.firstOrNull { ann ->
        WebsiteUpdateChecker.shouldShowAnnouncement(
            announcement = ann,
            effectiveLatestVersionName = effectiveLatestVersionName,
            effectiveLatestVersionCode = effectiveLatestVersionCode
        )
    }
    LaunchedEffect(topAnnouncement?.id, notificationsEnabled, floatingGlassEnabled, lastNotifiedId, isAppInForeground) {
        val savedNotifiedId = lastNotifiedId ?: return@LaunchedEffect
        if (topAnnouncement != null &&
            topAnnouncement.id.isNotBlank() &&
            topAnnouncement.id != savedNotifiedId
        ) {
            if (isAppInForeground) {
                inAppBanner = InAppGlassBannerData(
                    id = topAnnouncement.id,
                    badge = if (topAnnouncement.priority == "urgent") "Urgent Update" else "Alap Update",
                    title = topAnnouncement.title,
                    body = topAnnouncement.message
                )
            } else if (floatingGlassEnabled && NotificationHelper.hasOverlayPermission(context)) {
                GlassOverlayService.showPopup(
                    context = context,
                    title = topAnnouncement.title,
                    body = topAnnouncement.message,
                    badge = topAnnouncement.versionTag.ifBlank { "Update" }
                )
            }
            if (notificationsEnabled) {
                NotificationHelper.showAnnouncementNotification(
                    context = context,
                    announcementId = topAnnouncement.id,
                    title = topAnnouncement.title,
                    message = topAnnouncement.message,
                    versionTag = topAnnouncement.versionTag
                )
            }
            mainViewModel.markAnnouncementNotified(topAnnouncement.id)
        }
    }

    val visibleBannerAnnouncement = topAnnouncement?.takeIf {
        it.id != dismissedId || it.priority == "urgent"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AlapRoutes.HOME
        ) {
            composable(AlapRoutes.HOME) {
                HomeScaffoldScreen(
                    currentUserId = uid,
                    myProfile = myProfile,
                    isOnline = isOnline,
                    appConfig = appConfig,
                    latestApkUrl = effectiveLatestApkUrl,
                    latestVersionName = effectiveLatestVersionName,
                    latestVersionCode = effectiveLatestVersionCode,
                    isCheckingUpdate = checkingForUpdate,
                    releaseNotes = remoteVersionInfo?.changelog ?: emptyList(),
                    onCheckForUpdates = { mainViewModel.refreshRemoteVersionInfo(silent = false) },
                    chatsState = chatsState,
                    usersState = usersState,
                    usersMap = usersMap,
                    blockedUids = blockedUids.toSet(),
                    statusesState = statusesState,
                    visibleAnnouncement = visibleBannerAnnouncement,
                    actionMessage = actionMessage,
                    onClearActionMessage = mainViewModel::clearActionMessage,
                    onOpenChat = { chatId -> navController.navigate(AlapRoutes.chatRoute(chatId)) },
                    onStartDirectChat = { other ->
                        mainViewModel.startDirectChat(other) { chatId ->
                            navController.navigate(AlapRoutes.chatRoute(chatId))
                        }
                    },
                    onCreateGroup = { title, desc, memberUids, isStudyGroup, studyLockToAdmin ->
                        mainViewModel.createGroupChat(title, desc, memberUids, isStudyGroup, studyLockToAdmin) { chatId ->
                            navController.navigate(AlapRoutes.chatRoute(chatId))
                        }
                    },
                    onJoinStudyCode = { code ->
                        mainViewModel.joinGroupByStudyCode(code) { chatId ->
                            navController.navigate(AlapRoutes.chatRoute(chatId))
                        }
                    },
                    onTogglePin = mainViewModel::togglePin,
                    onToggleMute = mainViewModel::toggleMute,
                    onToggleArchive = mainViewModel::toggleArchive,
                    onPostStatus = { text, style -> mainViewModel.postStatus(text, style) },
                    onMarkStatusViewed = mainViewModel::markStatusViewed,
                    onDeleteStatus = mainViewModel::deleteStatus,
                    onDismissAnnouncement = mainViewModel::dismissAnnouncement,
                    onOpenSettings = { navController.navigate(AlapRoutes.SETTINGS) },
                    onOpenStarred = { navController.navigate(AlapRoutes.STARRED) }
                )
            }

            composable(
                route = AlapRoutes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                val chatDetailVm: ChatDetailViewModel = viewModel(
                    key = "${uid}_$chatId",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) as Application
                            val db = AlapFirestoreProvider.getDatabase(app)
                            val dao = AlapRoomDatabase.getInstance(app).alapLocalDao()
                            ChatDetailViewModel(
                                repository = AlapRepository(db),
                                prefs = LocalPreferencesManager(app),
                                currentUserId = uid,
                                chatId = chatId,
                                localDao = dao,
                                backupManager = LocalBackupManager(app, dao)
                            )
                        }
                    }
                )
                val chat by chatDetailVm.chat.collectAsStateWithLifecycle()
                val messagesState by chatDetailVm.messages.collectAsStateWithLifecycle()
                val studyItems by chatDetailVm.studyItems.collectAsStateWithLifecycle()
                val draftText by chatDetailVm.draftText.collectAsStateWithLifecycle()
                val errorBanner by chatDetailVm.errorBanner.collectAsStateWithLifecycle()
                val senderName = myProfile?.displayName ?: displayName

                ChatDetailScreen(
                    currentUserId = uid,
                    currentUserName = senderName,
                    chat = chat,
                    messagesState = messagesState,
                    draftText = draftText,
                    allChats = allChats,
                    usersMap = usersMap,
                    appConfig = appConfig,
                    isOnline = isOnline,
                    hideReadReceipts = myProfile?.hideReadReceipts ?: false,
                    blockedUids = blockedUids,
                    studyItems = studyItems,
                    studyFocusMinutesTotal = studyFocusMinutesTotal,
                    studySessionsCompleted = studySessionsCompleted,
                    errorBanner = errorBanner,
                    onBack = { navController.popBackStack() },
                    onDraftChanged = chatDetailVm::onDraftChanged,
                    onSendText = { replyTo ->
                        chatDetailVm.sendTextMessage(senderName, replyTo)
                    },
                    onSendSticker = { stickerId, label, replyTo ->
                        chatDetailVm.sendStickerMessage(senderName, stickerId, label, replyTo)
                    },
                    onSendPoll = { q, opts ->
                        chatDetailVm.sendPollMessage(senderName, q, opts)
                    },
                    onSendQuiz = { q, opts, correctIdx, explanation ->
                        chatDetailVm.sendQuizMessage(senderName, q, opts, correctIdx, explanation)
                    },
                    onSendFlashcard = { front, back ->
                        chatDetailVm.sendFlashcardMessage(senderName, front, back)
                    },
                    onVotePoll = chatDetailVm::voteInPoll,
                    onAddStudyItem = { itemType, title, details, scheduleText, examEpoch ->
                        chatDetailVm.addStudyItem(itemType, title, details, scheduleText, examEpoch, senderName)
                    },
                    onDeleteStudyItem = chatDetailVm::deleteStudyItem,
                    onEnableStudyInvite = { chatDetailVm.enableStudyInvite() },
                    onCompleteStudySession = mainViewModel::recordStudySession,
                    onShareStudySession = { mins ->
                        chatDetailVm.shareStudySessionMessage(senderName, mins)
                    },
                    onSetDisappearingHours = chatDetailVm::setDisappearingHours,
                    onToggleBlockUser = mainViewModel::toggleBlockUser,
                    onReportUser = { targetUid, targetName, reason ->
                        mainViewModel.submitReport(targetUid, targetName, chatId, reason)
                    },
                    onForwardMessage = { targetChat, orig ->
                        chatDetailVm.forwardMessage(targetChat, senderName, orig)
                    },
                    onToggleReaction = chatDetailVm::toggleReaction,
                    onToggleStar = chatDetailVm::toggleStar,
                    onEditMessage = chatDetailVm::editMessage,
                    onDeleteMessage = chatDetailVm::softDeleteMessage,
                    onMarkRead = chatDetailVm::markVisibleMessagesRead,
                    onUpdateGroup = chatDetailVm::updateGroupInfo,
                    onAddGroupMember = chatDetailVm::addGroupMember,
                    onToggleStudyLock = { chatDetailVm.toggleStudyGroupLock() },
                    onLeaveGroup = {
                        chatDetailVm.leaveGroup { navController.popBackStack() }
                    },
                    onClearError = chatDetailVm::clearErrorBanner
                )
            }

            composable(AlapRoutes.SETTINGS) {
                SettingsScreen(
                    currentUserId = uid,
                    myProfile = myProfile,
                    myPrivateInfo = myPrivateInfo,
                    currentEmail = email,
                    notificationsEnabled = notificationsEnabled,
                    floatingGlassEnabled = floatingGlassEnabled,
                    screenSecurityEnabled = screenSecurityEnabled,
                    hasAppLockPin = appLockPinHash.isNotBlank(),
                    localOnlyRelayMode = localOnlyRelayMode,
                    localStoredMessageCount = localStoredMessageCount,
                    studyFocusMinutesTotal = studyFocusMinutesTotal,
                    studySessionsCompleted = studySessionsCompleted,
                    usersMap = usersMap,
                    starredCount = starredMessages.size,
                    onBack = { navController.popBackStack() },
                    onOpenStarredMessages = { navController.navigate(AlapRoutes.STARRED) },
                    onSaveProfileAndPrivacy = { name, handle, phone, alapId, about, avatarId, hidePhone, hideUserId, hideOnline, hideAbout, hideReceipts, discoverable, isStudent, studentClass, studentSubject, birthYear, accountType, teacherSubject ->
                        mainViewModel.saveProfileAndPrivacy(
                            displayName = name,
                            handle = handle,
                            phoneNumber = phone,
                            alapId = alapId,
                            about = about,
                            avatarId = avatarId,
                            hidePhoneNumber = hidePhone,
                            hideUserId = hideUserId,
                            hideOnlineStatus = hideOnline,
                            hideAbout = hideAbout,
                            hideReadReceipts = hideReceipts,
                            isDiscoverable = discoverable,
                            isStudent = isStudent,
                            studentClass = studentClass,
                            studentSubject = studentSubject,
                            birthYear = birthYear,
                            accountType = accountType,
                            teacherSubject = teacherSubject,
                            markProfileCompleted = true
                        )
                    },
                    onNotificationsToggle = mainViewModel::setNotificationsEnabled,
                    onFloatingGlassToggle = mainViewModel::setFloatingGlassEnabled,
                    onScreenSecurityToggle = mainViewModel::setScreenSecurityEnabled,
                    onLocalOnlyRelayToggle = mainViewModel::setLocalOnlyRelayMode,
                    onExportBackup = mainViewModel::exportLocalBackupToUri,
                    onRestoreBackup = mainViewModel::restoreLocalBackupFromUri,
                    onRestoreAutoSnapshot = mainViewModel::restoreAutoSnapshot,
                    onSetAppLockPin = mainViewModel::setAppLockPin,
                    onUnblockUser = mainViewModel::toggleBlockUser,
                    onSignOut = {
                        mainViewModel.setOnlineStatus(false)
                        mainViewModel.clearOtpSessionSync()
                        GlassOverlayService.stopService(context)
                        signOut(credentialManager, scope)
                    }
                )
            }

            composable(AlapRoutes.STARRED) {
                StarredMessagesScreen(
                    messages = starredMessages,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { chatId -> navController.navigate(AlapRoutes.chatRoute(chatId)) }
                )
            }
        }

        // Translucent iPhone-style Frosted Glass In-App Banner
        GlassNotificationBanner(
            banner = inAppBanner,
            onTap = { data ->
                inAppBanner = null
                if (!data.chatId.isNullOrBlank()) {
                    navController.navigate(AlapRoutes.chatRoute(data.chatId)) {
                        launchSingleTop = true
                    }
                }
            },
            onDismiss = { inAppBanner = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffoldScreen(
    currentUserId: String,
    myProfile: com.example.data.model.UserProfile?,
    isOnline: Boolean,
    appConfig: com.example.data.model.AppConfig,
    latestApkUrl: String,
    latestVersionName: String,
    latestVersionCode: Long,
    isCheckingUpdate: Boolean,
    releaseNotes: List<String>,
    onCheckForUpdates: () -> Unit,
    chatsState: UiState<List<com.example.data.model.Chat>>,
    usersState: UiState<List<com.example.data.model.UserProfile>>,
    usersMap: Map<String, com.example.data.model.UserProfile>,
    blockedUids: Set<String>,
    statusesState: UiState<List<com.example.data.model.StatusUpdate>>,
    visibleAnnouncement: com.example.data.model.Announcement?,
    actionMessage: String?,
    onClearActionMessage: () -> Unit,
    onOpenChat: (String) -> Unit,
    onStartDirectChat: (com.example.data.model.UserProfile) -> Unit,
    onCreateGroup: (String, String, List<String>, Boolean, Boolean) -> Unit,
    onJoinStudyCode: (String) -> Unit,
    onTogglePin: (com.example.data.model.Chat) -> Unit,
    onToggleMute: (com.example.data.model.Chat) -> Unit,
    onToggleArchive: (com.example.data.model.Chat) -> Unit,
    onPostStatus: (String, String) -> Unit,
    onMarkStatusViewed: (com.example.data.model.StatusUpdate) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onDismissAnnouncement: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStarred: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage)
            onClearActionMessage()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        ),
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    1 -> "Updates"
                                    2 -> "People"
                                    else -> "Alap"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            if (selectedTab != 1) {
                                IconButton(
                                    onClick = { showSearch = !showSearch },
                                    modifier = Modifier.testTag("topbar_search_button")
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                            }
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.testTag("topbar_menu_button")
                                ) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = Color.White
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Starred Messages") },
                                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onOpenStarred()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    AnimatedVisibility(visible = showSearch && selectedTab != 1) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(if (selectedTab == 0) "Search chats..." else "Search people...")
                            },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    showSearch = false
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    OfflineAndMaintenanceStrip(
                        isOnline = isOnline,
                        isReadOnlyMode = appConfig.isReadOnlyMode,
                        maintenanceBanner = appConfig.maintenanceBanner
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = Color.White
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                                    contentDescription = "Chats"
                                )
                            },
                            label = { Text("Chats") },
                            modifier = Modifier.testTag("nav_tab_chats")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    if (selectedTab == 1) Icons.Filled.DonutLarge else Icons.Outlined.DonutLarge,
                                    contentDescription = "Updates"
                                )
                            },
                            label = { Text("Updates") },
                            modifier = Modifier.testTag("nav_tab_updates")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    if (selectedTab == 2) Icons.Filled.Group else Icons.Outlined.Group,
                                    contentDescription = "People"
                                )
                            },
                            label = { Text("People") },
                            modifier = Modifier.testTag("nav_tab_people")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { selectedTab = 2 },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_new_chat")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New Chat")
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    if (selectedTab == 0) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                                    contentDescription = "Chats"
                                )
                            },
                            label = { Text("Chats") },
                            modifier = Modifier.testTag("nav_tab_chats")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    if (selectedTab == 1) Icons.Filled.DonutLarge else Icons.Outlined.DonutLarge,
                                    contentDescription = "Updates"
                                )
                            },
                            label = { Text("Updates") },
                            modifier = Modifier.testTag("nav_tab_updates")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    if (selectedTab == 2) Icons.Filled.Group else Icons.Outlined.Group,
                                    contentDescription = "People"
                                )
                            },
                            label = { Text("People") },
                            modifier = Modifier.testTag("nav_tab_people")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            0 -> ChatsTabScreen(
                                currentUserId = currentUserId,
                                chatsState = chatsState,
                                usersMap = usersMap,
                                blockedUids = blockedUids,
                                activeAnnouncement = visibleAnnouncement,
                                latestApkUrl = latestApkUrl,
                                searchQuery = searchQuery,
                                onChatClick = onOpenChat,
                                onStartNewChat = { selectedTab = 2 },
                                onTogglePin = onTogglePin,
                                onToggleMute = onToggleMute,
                                onToggleArchive = onToggleArchive,
                                onDismissAnnouncement = onDismissAnnouncement
                            )

                            1 -> UpdatesTabScreen(
                                currentUserId = currentUserId,
                                myProfile = myProfile,
                                statusesState = statusesState,
                                allowStatusUpdates = appConfig.allowStatusUpdates,
                                isReadOnlyMode = appConfig.isReadOnlyMode,
                                visibleAnnouncement = visibleAnnouncement,
                                latestApkUrl = latestApkUrl,
                                latestVersionName = latestVersionName,
                                latestVersionCode = latestVersionCode,
                                isCheckingUpdate = isCheckingUpdate,
                                releaseNotes = releaseNotes,
                                onCheckForUpdates = onCheckForUpdates,
                                onPostStatus = onPostStatus,
                                onMarkViewed = onMarkStatusViewed,
                                onDeleteStatus = onDeleteStatus
                            )

                            2 -> PeopleTabScreen(
                                currentUserId = currentUserId,
                                usersState = usersState,
                                searchQuery = searchQuery,
                                allowGroupCreation = appConfig.allowGroupCreation,
                                isReadOnlyMode = appConfig.isReadOnlyMode,
                                onUserClick = onStartDirectChat,
                                onCreateGroup = onCreateGroup,
                                onJoinStudyCode = onJoinStudyCode
                            )
                        }
                    }
                }
            }
        }
    }
}
