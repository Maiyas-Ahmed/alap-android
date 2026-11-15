package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.ui.components.VerifiedBadge
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    myPrivateInfo: UserPrivateInfo?,
    currentEmail: String,
    isAdmin: Boolean,
    notificationsEnabled: Boolean,
    floatingGlassEnabled: Boolean,
    screenSecurityEnabled: Boolean,
    hasAppLockPin: Boolean,
    hasAdminPin: Boolean,
    localOnlyRelayMode: Boolean = true,
    localStoredMessageCount: Int = 0,
    usersMap: Map<String, UserProfile>,
    starredCount: Int,
    onBack: () -> Unit,
    onOpenAdminPanel: () -> Unit,
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
        isDiscoverable: Boolean
    ) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onFloatingGlassToggle: (Boolean) -> Unit,
    onScreenSecurityToggle: (Boolean) -> Unit,
    onLocalOnlyRelayToggle: (Boolean) -> Unit = {},
    onExportBackup: (Uri) -> Unit = {},
    onRestoreBackup: (Uri) -> Unit = {},
    onRestoreAutoSnapshot: () -> Unit = {},
    onSetAppLockPin: (String) -> Unit,
    onSetAdminPin: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showEditProfile by remember { mutableStateOf(false) }
    var showAppPinDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(NotificationHelper.hasOverlayPermission(context)) }

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

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationsToggle(granted)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text("Profile, Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Crisp White Profile Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AlapAvatar(
                            avatarId = myProfile?.avatarId ?: "avatar_emerald",
                            displayName = myProfile?.displayName ?: "Alap User",
                            isOnline = !hideOnline,
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = myProfile?.displayName ?: "Alap User",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (myProfile?.isVerified == true) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    VerifiedBadge()
                                }
                            }
                            Text(
                                text = "@${myProfile?.handle ?: "alap"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = myProfile?.about ?: "Hey there! I am using Alap.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    AccountInfoLine(
                        icon = Icons.Filled.Mail,
                        label = "Verified Gmail",
                        value = currentEmail,
                        visibilityBadge = "Private",
                        onCopy = { clipboard.setText(AnnotatedString(currentEmail)) }
                    )
                    AccountInfoLine(
                        icon = Icons.Filled.Phone,
                        label = "Mobile Number",
                        value = displayPhone,
                        visibilityBadge = if (hidePhone) "Hidden" else "Public",
                        onCopy = if (displayPhone != "Not added yet") {
                            { clipboard.setText(AnnotatedString(displayPhone)) }
                        } else null
                    )
                    AccountInfoLine(
                        icon = Icons.Filled.Badge,
                        label = "Alap User ID",
                        value = displayAlapId,
                        visibilityBadge = if (hideUserId) "Hidden" else "Public",
                        onCopy = { clipboard.setText(AnnotatedString(displayAlapId)) }
                    )
                }
            }

            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdminPanel() }
                        .testTag("open_admin_panel_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Manage users, moderation reports, audit logs & live controls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Smart Privacy Controls Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Control what contacts can view on your profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

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
                            pDiscoverable
                        )
                    }

                    PrivacyToggleRow(
                        icon = Icons.Filled.Phone,
                        title = "Hide Mobile Number",
                        subtitle = if (hidePhone) "Nobody can see your mobile number" else "Visible in your contact info",
                        checked = hidePhone,
                        onCheckedChange = { saveQuickPrivacy(it, hideUserId, hideOnline, hideAbout, hideReadReceipts, isDiscoverable) },
                        testTag = "settings_hide_phone_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.Badge,
                        title = "Hide User ID & @Handle",
                        subtitle = if (hideUserId) "Hidden from contacts & search" else "Visible to contacts",
                        checked = hideUserId,
                        onCheckedChange = { saveQuickPrivacy(hidePhone, it, hideOnline, hideAbout, hideReadReceipts, isDiscoverable) },
                        testTag = "settings_hide_userid_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Hide Last Seen & Online",
                        subtitle = if (hideOnline) "Online status & last seen hidden" else "Visible to contacts",
                        checked = hideOnline,
                        onCheckedChange = { saveQuickPrivacy(hidePhone, hideUserId, it, hideAbout, hideReadReceipts, isDiscoverable) },
                        testTag = "settings_hide_online_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.Person,
                        title = "Hide About Bio",
                        subtitle = if (hideAbout) "About bio hidden from contacts" else "Visible to everyone",
                        checked = hideAbout,
                        onCheckedChange = { saveQuickPrivacy(hidePhone, hideUserId, hideOnline, it, hideReadReceipts, isDiscoverable) },
                        testTag = "settings_hide_about_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.DoneAll,
                        title = "Hide Read Receipts",
                        subtitle = if (hideReadReceipts) "Blue read ticks are off" else "Senders see when you read messages",
                        checked = hideReadReceipts,
                        onCheckedChange = { saveQuickPrivacy(hidePhone, hideUserId, hideOnline, hideAbout, it, isDiscoverable) },
                        testTag = "settings_hide_receipts_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.VerifiedUser,
                        title = "Discoverable in People Tab",
                        subtitle = if (isDiscoverable) "Others can find & message you" else "Hidden from People directory",
                        checked = isDiscoverable,
                        onCheckedChange = { saveQuickPrivacy(hidePhone, hideUserId, hideOnline, hideAbout, hideReadReceipts, it) },
                        testTag = "settings_discoverable_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBlockedUsersDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blocked Contacts", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${blockedUids.size} contact(s) blocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Manage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Security, App Lock & Admin 2-Layer Protection Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("App Security & Locks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Protect chats & Admin Console with PIN / Fingerprint",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppPinDialog = true }
                            .padding(vertical = 8.dp)
                            .testTag("app_lock_pin_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock (PIN + Biometric)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (hasAppLockPin) "Active • Required when opening Alap" else "Off • Tap to set a 4-6 digit PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (hasAppLockPin) "Edit / Off" else "Set PIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isAdmin) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdminPinDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("admin_lock_pin_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Admin 2-Layer Security Gate", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (hasAdminPin) "Active • Extra PIN/Fingerprint before Admin Console" else "Uses device Fingerprint or set dedicated Admin PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (hasAdminPin) "Change" else "Set PIN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    PrivacyToggleRow(
                        icon = Icons.Filled.ScreenshotMonitor,
                        title = "Screen Privacy (Block Screenshots)",
                        subtitle = if (screenSecurityEnabled) "Screenshots & screen recording blocked (FLAG_SECURE)" else "Screenshots allowed",
                        checked = screenSecurityEnabled,
                        onCheckedChange = onScreenSecurityToggle,
                        testTag = "screen_security_switch"
                    )
                }
            }

            // Dual Notifications & iPhone-Style Floating Glass Popups Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dual Pop-Down Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Play-safe Heads-Up alerts + optional Floating Glass Popups",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Heads-Up Message & Update Alerts", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Play-compliant top pop-down notifications over games & apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onNotificationsToggle(wantEnabled)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Glass Popup Over Apps", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (hasOverlayPermission && floatingGlassEnabled) {
                                    "Active • Translucent glass popup over games & other apps"
                                } else if (floatingGlassEnabled) {
                                    "Permission needed • Falling back to Heads-Up notifications"
                                } else {
                                    "Optional • Grant 'Display over other apps' or stay on Heads-Up"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = floatingGlassEnabled,
                            onCheckedChange = { enable ->
                                onFloatingGlassToggle(enable)
                                if (enable && !NotificationHelper.hasOverlayPermission(context)) {
                                    NotificationHelper.openOverlayPermissionSettings(context)
                                }
                            },
                            modifier = Modifier.testTag("floating_glass_switch")
                        )
                    }

                    if (!hasOverlayPermission) {
                        OutlinedButton(
                            onClick = { NotificationHelper.openOverlayPermissionSettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant 'Display over other apps' Permission")
                        }
                    }
                }
            }

            // Starred Messages
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenStarredMessages() }
                    .testTag("starred_messages_button")
            ) {
                ListItem(
                    headlineContent = { Text("Starred Messages") },
                    supportingContent = { Text("$starredCount saved offline & synced") },
                    leadingContent = {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }

            // On-Device Local Storage, Zero Server Bloat & Chat Backup Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Local Chat Storage & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$localStoredMessageCount message(s) safely stored on your phone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    PrivacyToggleRow(
                        icon = Icons.Filled.DataSaverOn,
                        title = "Zero Server Bloat (Local-Only Relay)",
                        subtitle = if (localOnlyRelayMode) {
                            "Messages save on your phone & clear from Firebase after delivery"
                        } else {
                            "Cloud sync kept active alongside on-device Room cache"
                        },
                        checked = localOnlyRelayMode,
                        onCheckedChange = onLocalOnlyRelayToggle,
                        testTag = "local_only_relay_switch"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportBackupLauncher.launch("alap_chat_backup.json") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }
                        OutlinedButton(
                            onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore File")
                        }
                    }

                    OutlinedButton(
                        onClick = onRestoreAutoSnapshot,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_snapshot_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Device Auto-Snapshot")
                    }

                    Text(
                        text = "আপডেট বা রিইনস্টলের আগে Backup Export করলে সব চ্যাট অফলাইনেই সুরক্ষিত থাকবে। অটো ব্যাকআপও চালু আছে।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Official Public Websites & Version Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Alap Official Portal & App Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Version ${AlapConstants.CURRENT_APP_VERSION} (Build ${AlapConstants.CURRENT_APP_VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AlapConstants.PUBLIC_WEBSITE_URL))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_public_website_button")
                        ) {
                            Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alap Website")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AlapConstants.UPDATE_PORTAL_URL))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_update_portal_button")
                        ) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Portal")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showEditProfile) {
        var name by remember { mutableStateOf(myProfile?.displayName ?: "") }
        var handle by remember { mutableStateOf(myProfile?.handle ?: "") }
        var phone by remember { mutableStateOf(if (displayPhone == "Not added yet") "" else displayPhone) }
        var alapId by remember { mutableStateOf(displayAlapId) }
        var about by remember { mutableStateOf(myProfile?.about ?: "") }
        var avatarId by remember { mutableStateOf(myProfile?.avatarId ?: AlapConstants.AVATAR_KEYS.first()) }

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
                            isDiscoverable
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

    if (showAdminPinDialog) {
        PinSetupDialog(
            title = "Admin 2-Layer PIN",
            subtitle = "Extra PIN required before entering the Admin Console.",
            hasExisting = hasAdminPin,
            onDismiss = { showAdminPinDialog = false },
            onSave = { pin ->
                onSetAdminPin(pin)
                showAdminPinDialog = false
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

@Composable
private fun AccountInfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    visibilityBadge: String,
    onCopy: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (visibilityBadge == "Public") MaterialTheme.colorScheme.primaryContainer else Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = visibilityBadge,
                style = MaterialTheme.typography.labelSmall,
                color = if (visibilityBadge == "Public") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
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
