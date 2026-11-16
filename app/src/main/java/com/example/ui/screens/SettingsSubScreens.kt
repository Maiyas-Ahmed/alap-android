package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AlapConstants
import com.example.ui.components.AlapAvatar
import com.example.util.NotificationHelper

enum class SettingsSection(val title: String) {
    ACCOUNT("Account"),
    PRIVACY("Privacy"),
    SECURITY("Security & App Lock"),
    NOTIFICATIONS("Notifications"),
    STORAGE("Storage and Data"),
    APP_INFO("App Info & Portal")
}

@Composable
internal fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
    trailingText: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun SettingsCategoryCard(
    header: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = header.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun SettingsSectionContainer(
    content: @Composable ColumnScope.() -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
internal fun AccountSettingsSection(
    currentEmail: String,
    displayPhone: String,
    displayAlapId: String,
    hidePhone: Boolean,
    hideUserId: Boolean,
    onEditProfile: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    SettingsSectionContainer {
        SettingsCategoryCard(header = "Identity & Contact Info") {
            AccountInfoLine(
                icon = Icons.Filled.Mail,
                label = "Verified Gmail",
                value = currentEmail,
                visibilityBadge = "Private",
                onCopy = { clipboard.setText(AnnotatedString(currentEmail)) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            AccountInfoLine(
                icon = Icons.Filled.Phone,
                label = "Mobile Number",
                value = displayPhone,
                visibilityBadge = if (hidePhone) "Hidden" else "Public",
                onCopy = if (displayPhone != "Not added yet") {
                    { clipboard.setText(AnnotatedString(displayPhone)) }
                } else null
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            AccountInfoLine(
                icon = Icons.Filled.Badge,
                label = "Alap User ID",
                value = displayAlapId,
                visibilityBadge = if (hideUserId) "Hidden" else "Public",
                onCopy = { clipboard.setText(AnnotatedString(displayAlapId)) }
            )
        }

        SettingsCategoryCard(header = "Profile Actions") {
            Button(
                onClick = onEditProfile,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_button")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Name, Handle, Phone & Bio")
            }
            Text(
                text = "Who can see your mobile number or User ID can be changed anytime inside Settings → Privacy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun PrivacySettingsSection(
    hidePhone: Boolean,
    hideUserId: Boolean,
    hideOnline: Boolean,
    hideAbout: Boolean,
    hideReadReceipts: Boolean,
    isDiscoverable: Boolean,
    blockedCount: Int,
    onUpdatePrivacy: (
        hidePhone: Boolean,
        hideUserId: Boolean,
        hideOnline: Boolean,
        hideAbout: Boolean,
        hideReadReceipts: Boolean,
        isDiscoverable: Boolean
    ) -> Unit,
    onOpenBlockedUsers: () -> Unit
) {
    SettingsSectionContainer {
        SettingsCategoryCard(header = "Who Can See My Personal Info") {
            PrivacyToggleRow(
                icon = Icons.Filled.Phone,
                title = "Hide Mobile Number",
                subtitle = if (hidePhone) "Nobody can see your mobile number" else "Visible in your contact info",
                checked = hidePhone,
                onCheckedChange = {
                    onUpdatePrivacy(it, hideUserId, hideOnline, hideAbout, hideReadReceipts, isDiscoverable)
                },
                testTag = "settings_hide_phone_switch"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            PrivacyToggleRow(
                icon = Icons.Filled.Badge,
                title = "Hide User ID & @Handle",
                subtitle = if (hideUserId) "Hidden from contacts & search" else "Visible to contacts",
                checked = hideUserId,
                onCheckedChange = {
                    onUpdatePrivacy(hidePhone, it, hideOnline, hideAbout, hideReadReceipts, isDiscoverable)
                },
                testTag = "settings_hide_userid_switch"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            PrivacyToggleRow(
                icon = Icons.Filled.Person,
                title = "Hide About Bio",
                subtitle = if (hideAbout) "About bio hidden from contacts" else "Visible to everyone",
                checked = hideAbout,
                onCheckedChange = {
                    onUpdatePrivacy(hidePhone, hideUserId, hideOnline, it, hideReadReceipts, isDiscoverable)
                },
                testTag = "settings_hide_about_switch"
            )
        }

        SettingsCategoryCard(header = "Activity & Discovery") {
            PrivacyToggleRow(
                icon = Icons.Filled.VisibilityOff,
                title = "Hide Last Seen & Online",
                subtitle = if (hideOnline) "Online status & last seen hidden" else "Visible to contacts",
                checked = hideOnline,
                onCheckedChange = {
                    onUpdatePrivacy(hidePhone, hideUserId, it, hideAbout, hideReadReceipts, isDiscoverable)
                },
                testTag = "settings_hide_online_switch"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            PrivacyToggleRow(
                icon = Icons.Filled.DoneAll,
                title = "Hide Read Receipts",
                subtitle = if (hideReadReceipts) "Blue read ticks are off" else "Senders see when you read messages",
                checked = hideReadReceipts,
                onCheckedChange = {
                    onUpdatePrivacy(hidePhone, hideUserId, hideOnline, hideAbout, it, isDiscoverable)
                },
                testTag = "settings_hide_receipts_switch"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            PrivacyToggleRow(
                icon = Icons.Filled.VerifiedUser,
                title = "Discoverable in People Tab",
                subtitle = if (isDiscoverable) "Others can find & message you" else "Hidden from People directory",
                checked = isDiscoverable,
                onCheckedChange = {
                    onUpdatePrivacy(hidePhone, hideUserId, hideOnline, hideAbout, hideReadReceipts, it)
                },
                testTag = "settings_discoverable_switch"
            )
        }

        SettingsCategoryCard(header = "Blocked Contacts") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBlockedUsers)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Blocked Contacts", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$blockedCount contact(s) blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun SecuritySettingsSection(
    isAdmin: Boolean,
    hasAppLockPin: Boolean,
    hasAdminPin: Boolean,
    screenSecurityEnabled: Boolean,
    onOpenAppPinDialog: () -> Unit,
    onOpenAdminPinDialog: () -> Unit,
    onScreenSecurityToggle: (Boolean) -> Unit
) {
    SettingsSectionContainer {
        SettingsCategoryCard(header = "App Lock & Biometrics") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppPinDialog)
                    .padding(vertical = 6.dp)
                    .testTag("app_lock_pin_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
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
                        .clickable(onClick = onOpenAdminPinDialog)
                        .padding(vertical = 6.dp)
                        .testTag("admin_lock_pin_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
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
        }

        SettingsCategoryCard(header = "Screen Protection") {
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
}

@Composable
internal fun NotificationsSettingsSection(
    notificationsEnabled: Boolean,
    floatingGlassEnabled: Boolean,
    hasOverlayPermission: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onFloatingGlassToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationsToggle(granted)
    }

    SettingsSectionContainer {
        SettingsCategoryCard(header = "Message & Update Alerts") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
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
        }

        SettingsCategoryCard(header = "Floating Glass Popups") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
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
}

@Composable
internal fun StorageSettingsSection(
    localOnlyRelayMode: Boolean,
    localStoredMessageCount: Int,
    onLocalOnlyRelayToggle: (Boolean) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onRestoreAutoSnapshot: () -> Unit
) {
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

    SettingsSectionContainer {
        SettingsCategoryCard(header = "On-Device Storage & Relay") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Local Cache", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$localStoredMessageCount message(s) safely stored on your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
        }

        SettingsCategoryCard(header = "Offline Chat Backup") {
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
}

@Composable
internal fun AppInfoSettingsSection() {
    val context = LocalContext.current
    SettingsSectionContainer {
        SettingsCategoryCard(header = "Version & Build") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlapAvatar(
                    avatarId = "avatar_emerald",
                    displayName = "Alap",
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Alap Messenger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version ${AlapConstants.CURRENT_APP_VERSION} (Build ${AlapConstants.CURRENT_APP_VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SettingsCategoryCard(header = "Official Websites") {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Offline-first cache • Direct updates via Alap Portal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun AccountInfoLine(
    icon: ImageVector,
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
