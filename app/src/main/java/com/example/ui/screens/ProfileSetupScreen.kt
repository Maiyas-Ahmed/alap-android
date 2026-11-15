package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AlapConstants
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar

@Composable
fun ProfileSetupScreen(
    uid: String,
    email: String,
    existingProfile: UserProfile?,
    existingPrivateInfo: UserPrivateInfo?,
    isSaving: Boolean,
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
    ) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val defaultAlapId = remember(uid, existingPrivateInfo) {
        existingPrivateInfo?.alapId?.takeIf { it.isNotBlank() }
            ?: "ALAP-${uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase().take(6).padEnd(6, '0')}"
    }

    var displayName by remember(existingProfile?.displayName) {
        mutableStateOf(existingProfile?.displayName?.ifBlank { "" } ?: "Alap User")
    }
    var handle by remember(existingProfile?.handle) {
        mutableStateOf(existingProfile?.handle ?: "")
    }
    var phoneNumber by remember(existingPrivateInfo?.phoneNumber) {
        mutableStateOf(existingPrivateInfo?.phoneNumber ?: "")
    }
    var alapId by remember(defaultAlapId) {
        mutableStateOf(defaultAlapId)
    }
    var about by remember(existingProfile?.about) {
        mutableStateOf(existingProfile?.about ?: "Hey there! I am using Alap.")
    }
    var avatarId by remember(existingProfile?.avatarId) {
        mutableStateOf(existingProfile?.avatarId ?: AlapConstants.AVATAR_KEYS.first())
    }

    // Smart Privacy Controls
    var hidePhoneNumber by remember(existingProfile?.hidePhoneNumber) {
        mutableStateOf(existingProfile?.hidePhoneNumber ?: true)
    }
    var hideUserId by remember(existingProfile?.hideUserId) {
        mutableStateOf(existingProfile?.hideUserId ?: false)
    }
    var hideOnlineStatus by remember(existingProfile?.hideOnlineStatus) {
        mutableStateOf(existingProfile?.hideOnlineStatus ?: false)
    }
    var hideAbout by remember(existingProfile?.hideAbout) {
        mutableStateOf(existingProfile?.hideAbout ?: false)
    }
    var hideReadReceipts by remember(existingProfile?.hideReadReceipts) {
        mutableStateOf(existingProfile?.hideReadReceipts ?: false)
    }
    var isDiscoverable by remember(existingProfile?.isDiscoverable) {
        mutableStateOf(existingProfile?.isDiscoverable ?: true)
    }

    var copiedId by remember { mutableStateOf(false) }

    val aboutPresets = remember {
        listOf("Available", "Busy", "At work", "Hey there! I am using Alap.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "STEP 2 OF 2 • PROFILE & PRIVACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Set Up Your Alap Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add your details and choose what contacts can see.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Verified Identity Preview Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AlapAvatar(
                            avatarId = avatarId,
                            displayName = displayName.ifBlank { "Alap" },
                            isOnline = !hideOnlineStatus,
                            size = 62.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName.ifBlank { "Your Name" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "Verified Gmail",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "ID: $alapId • ${if (phoneNumber.isBlank()) "No mobile set" else phoneNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
                                AlapAvatar(avatarId = key, displayName = displayName.ifBlank { "A" }, size = 40.dp)
                            }
                        }
                    }
                }
            }

            // Personal Information Form Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Personal Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { if (it.length <= 50) displayName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_display_name_input")
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { if (it.length <= 20) phoneNumber = it },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("+880 17XX-XXXXXX") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_phone_input")
                    )

                    OutlinedTextField(
                        value = alapId,
                        onValueChange = { raw ->
                            val clean = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(24)
                            alapId = clean
                        },
                        label = { Text("User ID") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(alapId))
                                copiedId = true
                            }) {
                                Icon(
                                    imageVector = if (copiedId) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                    contentDescription = "Copy User ID",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_user_id_input")
                    )

                    OutlinedTextField(
                        value = handle,
                        onValueChange = { raw ->
                            handle = raw.lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
                        },
                        label = { Text("Username (@handle)") },
                        leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_handle_input")
                    )

                    OutlinedTextField(
                        value = about,
                        onValueChange = { if (it.length <= 140) about = it },
                        label = { Text("About") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_about_input")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aboutPresets.forEach { preset ->
                            FilterChip(
                                selected = about == preset,
                                onClick = { about = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Smart Privacy Controls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Choose what to hide from other people on Alap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    PrivacyToggleRow(
                        icon = Icons.Filled.Phone,
                        title = "Hide Mobile Number",
                        subtitle = if (hidePhoneNumber) "Nobody can see your mobile number" else "Visible to contacts in Profile Info",
                        checked = hidePhoneNumber,
                        onCheckedChange = { hidePhoneNumber = it },
                        testTag = "privacy_hide_phone_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    PrivacyToggleRow(
                        icon = Icons.Filled.Badge,
                        title = "Hide User ID & @Handle",
                        subtitle = if (hideUserId) "Hidden from contacts & directory search" else "Contacts can see your User ID",
                        checked = hideUserId,
                        onCheckedChange = { hideUserId = it },
                        testTag = "privacy_hide_userid_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    PrivacyToggleRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Hide Last Seen & Online",
                        subtitle = if (hideOnlineStatus) "Nobody can see when you are online" else "Contacts see your Online / Last seen",
                        checked = hideOnlineStatus,
                        onCheckedChange = { hideOnlineStatus = it },
                        testTag = "privacy_hide_online_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    PrivacyToggleRow(
                        icon = Icons.Filled.Person,
                        title = "Hide About Bio",
                        subtitle = if (hideAbout) "About line hidden from contacts" else "Everyone can see your About bio",
                        checked = hideAbout,
                        onCheckedChange = { hideAbout = it },
                        testTag = "privacy_hide_about_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    PrivacyToggleRow(
                        icon = Icons.Filled.DoneAll,
                        title = "Hide Read Receipts",
                        subtitle = if (hideReadReceipts) "Senders won't see blue read ticks" else "Senders see when you read messages",
                        checked = hideReadReceipts,
                        onCheckedChange = { hideReadReceipts = it },
                        testTag = "privacy_hide_receipts_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    PrivacyToggleRow(
                        icon = Icons.Filled.VerifiedUser,
                        title = "Show in People Directory",
                        subtitle = if (isDiscoverable) "People can find you to start a chat" else "Only existing chats can message you",
                        checked = isDiscoverable,
                        onCheckedChange = { isDiscoverable = it },
                        testTag = "privacy_discoverable_switch"
                    )
                }
            }

            Button(
                onClick = {
                    onSaveProfileAndPrivacy(
                        displayName,
                        handle,
                        phoneNumber,
                        alapId,
                        about,
                        avatarId,
                        hidePhoneNumber,
                        hideUserId,
                        hideOnlineStatus,
                        hideAbout,
                        hideReadReceipts,
                        isDiscoverable
                    )
                },
                enabled = displayName.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save Profile & Enter Alap",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PrivacyToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
