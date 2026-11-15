package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlapConstants
import com.example.data.model.Announcement
import com.example.data.model.StatusUpdate
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.util.ApkUpdateManager
import com.example.util.UpdateDownloadState
import com.example.util.WebsiteUpdateChecker

@Composable
fun UpdatesTabScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    statusesState: UiState<List<StatusUpdate>>,
    allowStatusUpdates: Boolean,
    isReadOnlyMode: Boolean,
    visibleAnnouncement: Announcement?,
    latestApkUrl: String = "",
    latestVersionName: String = AlapConstants.CURRENT_APP_VERSION,
    latestVersionCode: Long = AlapConstants.CURRENT_APP_VERSION_CODE,
    isCheckingUpdate: Boolean = false,
    releaseNotes: List<String> = emptyList(),
    onCheckForUpdates: () -> Unit = {},
    onPostStatus: (text: String, styleKey: String) -> Unit,
    onMarkViewed: (StatusUpdate) -> Unit,
    onDeleteStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeStatus by remember { mutableStateOf<StatusUpdate?>(null) }
    val downloadState by ApkUpdateManager.state.collectAsStateWithLifecycle()

    val hasNewerUpdate = latestVersionCode > AlapConstants.CURRENT_APP_VERSION_CODE ||
        WebsiteUpdateChecker.isVersionLower(AlapConstants.CURRENT_APP_VERSION, latestVersionName) ||
        (visibleAnnouncement != null && latestApkUrl.isNotBlank() &&
            (visibleAnnouncement.priority == "update" || visibleAnnouncement.versionTag.isNotBlank()))

    Column(modifier = Modifier.fillMaxSize()) {
        // App Version & Direct In-App Update Card (connected to Admin Console + alap-updates.netlify.app/version.json)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (hasNewerUpdate) MaterialTheme.colorScheme.primaryContainer else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (hasNewerUpdate) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (hasNewerUpdate) {
                                    "নতুন আপডেট পাওয়া গেছে: v${latestVersionName.ifBlank { "Latest" }}"
                                } else {
                                    "Alap v${AlapConstants.CURRENT_APP_VERSION} (Build ${AlapConstants.CURRENT_APP_VERSION_CODE})"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasNewerUpdate) {
                                    "বর্তমান ভার্সন: v${AlapConstants.CURRENT_APP_VERSION} • সরাসরি ইন-অ্যাপ আপডেট উপলব্ধ"
                                } else {
                                    "সর্বশেষ ভার্সন চালু আছে"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("check_updates_button")
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Check updates",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (hasNewerUpdate && releaseNotes.isNotEmpty()) {
                    Text(
                        text = releaseNotes.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (val state = downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "ডাউনলোড হচ্ছে... ${state.progressPercent}% (শেষে অটো ইনস্টল শুরু হবে)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Button(
                            onClick = { ApkUpdateManager.installApkFile(context, state.apkFile) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("এখনই ইনস্টল করুন (Install Update)", fontWeight = FontWeight.Bold)
                        }
                    }
                    is UpdateDownloadState.NeedsInstallPermission -> {
                        Text(
                            text = "অটো আপডেট ইনস্টল করতে 'Install unknown apps' অনুমতি দিন।",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { ApkUpdateManager.openInstallPermissionSettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Install Permission দিন", fontWeight = FontWeight.Bold)
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = {
                                ApkUpdateManager.openInBrowserFallback(
                                    context = context,
                                    url = state.fallbackUrl.ifBlank { AlapConstants.UPDATE_PORTAL_URL }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ব্রাউজার দিয়ে ডাউনলোড করুন")
                        }
                    }
                    UpdateDownloadState.Idle -> Unit
                }

                if (hasNewerUpdate && latestApkUrl.isNotBlank() && downloadState !is UpdateDownloadState.Downloading) {
                    Button(
                        onClick = {
                            ApkUpdateManager.startUpdateDownload(
                                context = context,
                                apkUrl = latestApkUrl,
                                versionTag = latestVersionName
                            )
                            Toast.makeText(
                                context,
                                "আপডেট ডাউনলোড হচ্ছে... শেষ হলেই অটো ইনস্টলার চালু হবে।",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_now_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("আপডেট করুন (Update Now)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Admin Announcement Card if present
        if (visibleAnnouncement != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = visibleAnnouncement.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = visibleAnnouncement.priority.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = visibleAnnouncement.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // My Status Header Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(enabled = allowStatusUpdates && !isReadOnlyMode) {
                    showCreateDialog = true
                }
                .testTag("create_status_card")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AlapAvatar(
                        avatarId = myProfile?.avatarId ?: "avatar_emerald",
                        displayName = myProfile?.displayName ?: "Me",
                        size = 50.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Post status",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            isReadOnlyMode -> "Read-only mode active"
                            !allowStatusUpdates -> "Status updates paused by Admin"
                            else -> "Tap to share a lightweight 24h text update"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "RECENT UPDATES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        when (statusesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(statusesState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val itemsList = statusesState.data
                if (itemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent status updates yet.\nShare a quick note that disappears in 24 hours.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(itemsList, key = { it.id }) { status ->
                            val isViewed = currentUserId in status.viewerUids || status.userId == currentUserId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMarkViewed(status)
                                        activeStatus = status
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("status_item_${status.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.5.dp,
                                            color = if (isViewed) {
                                                MaterialTheme.colorScheme.outline
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                ) {
                                    AlapAvatar(
                                        avatarId = status.authorAvatarId,
                                        displayName = status.authorName,
                                        size = 48.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (status.userId == currentUserId) "You (${status.authorName})" else status.authorName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = status.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AlapDateFormatter.formatChatTime(status.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var statusText by remember { mutableStateOf("") }
        var selectedStyle by remember { mutableStateOf("emerald") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Share Status Update") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { if (it.length <= 300) statusText = it },
                        label = { Text("What's on your mind?") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_text_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlapConstants.STATUS_STYLES.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (statusText.isNotBlank()) {
                            onPostStatus(statusText, selectedStyle)
                            showCreateDialog = false
                        }
                    },
                    enabled = statusText.isNotBlank(),
                    modifier = Modifier.testTag("post_status_button")
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    activeStatus?.let { status ->
        val isLightCard = status.styleKey == "sand" || status.styleKey == "slate"
        val bg = when (status.styleKey) {
            "slate" -> Color(0xFFEAF1F5)
            "charcoal" -> Color(0xFF0F5247)
            "sand" -> Color(0xFFF6EFE6)
            else -> Color(0xFF008069)
        }
        val contentColor = if (isLightCard) Color(0xFF111B21) else Color.White
        Dialog(
            onDismissRequest = { activeStatus = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlapAvatar(
                        avatarId = status.authorAvatarId,
                        displayName = status.authorName,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AlapDateFormatter.formatMessageTime(status.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.78f)
                        )
                    }
                    if (status.userId == currentUserId) {
                        IconButton(onClick = {
                            onDeleteStatus(status.id)
                            activeStatus = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete status", tint = contentColor)
                        }
                    }
                    IconButton(onClick = { activeStatus = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close status", tint = contentColor)
                    }
                }

                Text(
                    text = status.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLightCard) Color.White else Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${status.viewerUids.size} views",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
