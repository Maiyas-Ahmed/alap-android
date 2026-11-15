package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminApiKey
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminEntry
import com.example.data.model.AlapConstants
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.Chat
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserReport
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.ui.components.VerifiedBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentUserEmail: String,
    usersState: UiState<List<UserProfile>>,
    allChats: List<Chat>,
    announcements: List<Announcement>,
    admins: List<AdminEntry>,
    auditLogs: List<AdminAuditLog>,
    reports: List<UserReport>,
    apiKeys: List<AdminApiKey> = emptyList(),
    appConfig: AppConfig,
    userEmails: Map<String, String>,
    feedbackMessage: String?,
    onBack: () -> Unit,
    onClearFeedback: () -> Unit,
    onPublishAnnouncement: (title: String, message: String, versionTag: String, priority: String) -> Unit,
    onToggleAnnouncementActive: (Announcement) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onUpdateAppConfig: (AppConfig) -> Unit,
    onToggleUserVerified: (UserProfile) -> Unit,
    onToggleUserSuspended: (UserProfile) -> Unit,
    onLoadUserEmail: (String) -> Unit,
    onGrantSecondaryAdmin: (UserProfile, String) -> Unit,
    onRevokeSecondaryAdmin: (String) -> Unit,
    onUpdateReportStatus: (UserReport, String) -> Unit,
    onCleanupExpiredStatuses: () -> Unit,
    onCreateApiKey: (label: String, scopes: List<String>) -> Unit = { _, _ -> },
    onToggleApiKeyActive: (AdminApiKey) -> Unit = {},
    onDeleteApiKey: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isPrimaryAdmin = currentUserEmail.equals(AlapConstants.PRIMARY_ADMIN_EMAIL, ignoreCase = true)
    val openReportCount = reports.count { it.status == "open" }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(feedbackMessage)
            onClearFeedback()
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Column {
                        Text(
                            text = "Admin Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUserEmail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview & Quota", fontWeight = FontWeight.SemiBold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Broadcast & Updates", fontWeight = FontWeight.SemiBold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("App Controls", fontWeight = FontWeight.SemiBold) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("User Management", fontWeight = FontWeight.SemiBold) })
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text(if (openReportCount > 0) "Reports ($openReportCount)" else "Reports", fontWeight = FontWeight.SemiBold) }
                )
                Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Audit Log", fontWeight = FontWeight.SemiBold) })
                if (isPrimaryAdmin) {
                    Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("API Keys", fontWeight = FontWeight.SemiBold) })
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    usersState = usersState,
                    allChats = allChats,
                    announcements = announcements,
                    openReports = openReportCount,
                    appConfig = appConfig,
                    onCleanupExpiredStatuses = onCleanupExpiredStatuses
                )
                1 -> AdminBroadcastTab(
                    announcements = announcements,
                    onPublish = onPublishAnnouncement,
                    onToggleActive = onToggleAnnouncementActive,
                    onDelete = onDeleteAnnouncement
                )
                2 -> AdminControlsTab(
                    appConfig = appConfig,
                    onSave = onUpdateAppConfig
                )
                3 -> AdminUsersTab(
                    usersState = usersState,
                    admins = admins,
                    userEmails = userEmails,
                    isPrimaryAdmin = isPrimaryAdmin,
                    onLoadEmail = onLoadUserEmail,
                    onToggleVerified = onToggleUserVerified,
                    onToggleSuspended = onToggleUserSuspended,
                    onGrantAdmin = onGrantSecondaryAdmin,
                    onRevokeAdmin = onRevokeSecondaryAdmin
                )
                4 -> AdminReportsTab(
                    reports = reports,
                    usersState = usersState,
                    onUpdateStatus = onUpdateReportStatus,
                    onToggleSuspended = onToggleUserSuspended
                )
                5 -> AdminAuditLogTab(auditLogs = auditLogs)
                6 -> if (isPrimaryAdmin) {
                    AdminApiKeysTab(
                        apiKeys = apiKeys,
                        onCreateKey = onCreateApiKey,
                        onToggleActive = onToggleApiKeyActive,
                        onDeleteKey = onDeleteApiKey
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    usersState: UiState<List<UserProfile>>,
    allChats: List<Chat>,
    announcements: List<Announcement>,
    openReports: Int,
    appConfig: AppConfig,
    onCleanupExpiredStatuses: () -> Unit
) {
    val users = (usersState as? UiState.Success)?.data ?: emptyList()
    val groupCount = allChats.count { it.chatType == "group" }
    val estReads = users.size * 24 + allChats.size * 12
    val estWrites = allChats.size * 8 + announcements.size * 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Platform Telemetry & Quota Analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total Users", users.size.toString(), Modifier.weight(1f))
            StatCard("Online Now", users.count { it.isOnline }.toString(), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Active Chats", allChats.size.toString(), Modifier.weight(1f))
            StatCard("Group Chats", groupCount.toString(), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Est. Reads (24h)", "$estReads docs", Modifier.weight(1f))
            StatCard("Est. Writes (24h)", "$estWrites docs", Modifier.weight(1f))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Server Configuration & Protection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Tagline: ${appConfig.appTagline}", style = MaterialTheme.typography.bodyMedium)
                Text("Min Version: ${appConfig.minAppVersion} (Force Update: ${if (appConfig.forceUpdateRequired) "Enabled" else "Disabled"})", style = MaterialTheme.typography.bodyMedium)
                Text("Max Message Length: ${appConfig.maxMessageLength} chars", style = MaterialTheme.typography.bodyMedium)
                Text("Read-Only Mode: ${if (appConfig.isReadOnlyMode) "Active" else "Normal"}", style = MaterialTheme.typography.bodyMedium)
                Text("Open Reports Pending Action: $openReports", style = MaterialTheme.typography.bodyMedium, color = if (openReports > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }

        Button(
            onClick = onCleanupExpiredStatuses,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_cleanup_statuses_button")
        ) {
            Icon(Icons.Filled.CleaningServices, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quota Saver: Purge Expired Statuses", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdminBroadcastTab(
    announcements: List<Announcement>,
    onPublish: (title: String, message: String, versionTag: String, priority: String) -> Unit,
    onToggleActive: (Announcement) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var versionTag by remember { mutableStateOf("v1.1") }
    var priority by remember { mutableStateOf("update") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broadcast Update & In-App Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 100) title = it },
                        label = { Text("Update Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_announcement_title_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= 500) message = it },
                        label = { Text("Update Details & Release Notes") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_announcement_body_input")
                    )
                    OutlinedTextField(
                        value = versionTag,
                        onValueChange = { if (it.length <= 24) versionTag = it },
                        label = { Text("Version Tag (e.g. v1.1)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("update", "info", "urgent").forEach { item ->
                            FilterChip(
                                selected = priority == item,
                                onClick = { priority = item },
                                label = { Text(item.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onPublish(title, message, versionTag, priority)
                            title = ""
                            message = ""
                        },
                        enabled = title.isNotBlank() && message.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_announcement_button")
                    ) {
                        Text("Publish Update & Notify All Users", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Broadcasts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(announcements, key = { it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${item.title} ${if (item.versionTag.isNotBlank()) "(${item.versionTag})" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { onToggleActive(item) }
                    )
                    IconButton(onClick = { onDelete(item.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminControlsTab(
    appConfig: AppConfig,
    onSave: (AppConfig) -> Unit
) {
    var tagline by remember(appConfig) { mutableStateOf(appConfig.appTagline) }
    var banner by remember(appConfig) { mutableStateOf(appConfig.maintenanceBanner) }
    var minVersion by remember(appConfig) { mutableStateOf(appConfig.minAppVersion) }
    var latestApkUrl by remember(appConfig) { mutableStateOf(appConfig.latestApkUrl) }
    var latestVersionCodeText by remember(appConfig) { mutableStateOf(appConfig.latestVersionCode.toString()) }
    var maxLenText by remember(appConfig) { mutableStateOf(appConfig.maxMessageLength.toString()) }
    var readOnly by remember(appConfig) { mutableStateOf(appConfig.isReadOnlyMode) }
    var allowStatus by remember(appConfig) { mutableStateOf(appConfig.allowStatusUpdates) }
    var allowGroups by remember(appConfig) { mutableStateOf(appConfig.allowGroupCreation) }
    var forceUpdate by remember(appConfig) { mutableStateOf(appConfig.forceUpdateRequired) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Live App Controls & Maintenance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = tagline,
            onValueChange = { if (it.length <= 100) tagline = it },
            label = { Text("App Tagline") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = banner,
            onValueChange = { if (it.length <= 300) banner = it },
            label = { Text("Global Maintenance Notice (blank = hidden)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = latestApkUrl,
            onValueChange = { if (it.length <= 500) latestApkUrl = it },
            label = { Text("Direct APK Update URL (Netlify HTTPS .apk)") },
            placeholder = { Text("https://updates.yourdomain.com/alap-latest.apk") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_latest_apk_url_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = minVersion,
                onValueChange = { if (it.length <= 20) minVersion = it },
                label = { Text("Min Version") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = latestVersionCodeText,
                onValueChange = { input -> latestVersionCodeText = input.filter { it.isDigit() }.take(5) },
                label = { Text("Ver Code") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.75f)
            )
            OutlinedTextField(
                value = maxLenText,
                onValueChange = { input -> maxLenText = input.filter { it.isDigit() }.take(4) },
                label = { Text("Max Chars") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.85f)
            )
        }

        ControlSwitchRow("Read-Only Maintenance Mode", "Pause sending new messages across app", readOnly) { readOnly = it }
        ControlSwitchRow("Force App Update Gate", "Block outdated versions below Min Version", forceUpdate) { forceUpdate = it }
        ControlSwitchRow("Allow 24h Status Updates", "Enable Status tab posting for users", allowStatus) { allowStatus = it }
        ControlSwitchRow("Allow Group Creation", "Let users create new group conversations", allowGroups) { allowGroups = it }

        Button(
            onClick = {
                val maxLen = (maxLenText.toLongOrNull() ?: 1000L).coerceIn(50L, 4000L)
                val latestCode = (latestVersionCodeText.toLongOrNull() ?: 1L).coerceIn(1L, 100000L)
                onSave(
                    appConfig.copy(
                        appTagline = tagline,
                        maintenanceBanner = banner,
                        minAppVersion = minVersion.ifBlank { "1.0" },
                        latestApkUrl = latestApkUrl.trim(),
                        latestVersionCode = latestCode,
                        maxMessageLength = maxLen,
                        isReadOnlyMode = readOnly,
                        forceUpdateRequired = forceUpdate,
                        allowStatusUpdates = allowStatus,
                        allowGroupCreation = allowGroups
                    )
                )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_app_config_button")
        ) {
            Text("Save App Configuration", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AdminUsersTab(
    usersState: UiState<List<UserProfile>>,
    admins: List<AdminEntry>,
    userEmails: Map<String, String>,
    isPrimaryAdmin: Boolean,
    onLoadEmail: (String) -> Unit,
    onToggleVerified: (UserProfile) -> Unit,
    onToggleSuspended: (UserProfile) -> Unit,
    onGrantAdmin: (UserProfile, String) -> Unit,
    onRevokeAdmin: (String) -> Unit
) {
    when (usersState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(usersState.message, color = MaterialTheme.colorScheme.error)
        }

        is UiState.Success -> {
            val adminUids = admins.map { it.uid }.toSet()
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(usersState.data, key = { it.userId }) { user ->
                    LaunchedEffect(user.userId) { onLoadEmail(user.userId) }
                    val email = userEmails[user.userId] ?: ""
                    val isDelegatedAdmin = user.userId in adminUids
                    val isPrimaryUser = email.equals(AlapConstants.PRIMARY_ADMIN_EMAIL, ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AlapAvatar(
                                    avatarId = user.avatarId,
                                    displayName = user.displayName,
                                    isOnline = user.isOnline,
                                    size = 48.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (user.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge()
                                        }
                                    }
                                    Text(
                                        text = "@${user.handle} ${if (email.isNotBlank()) "• $email" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onToggleVerified(user) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (user.isVerified) "Unverify" else "Verify")
                                }
                                if (!isPrimaryUser) {
                                    OutlinedButton(
                                        onClick = { onToggleSuspended(user) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (user.isSuspended) "Unsuspend" else "Suspend",
                                            color = if (user.isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (isPrimaryAdmin && !isPrimaryUser && email.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        if (isDelegatedAdmin) onRevokeAdmin(user.userId)
                                        else onGrantAdmin(user, email)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isDelegatedAdmin) "Revoke Secondary Admin" else "Grant Secondary Admin")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportsTab(
    reports: List<UserReport>,
    usersState: UiState<List<UserProfile>>,
    onUpdateStatus: (UserReport, String) -> Unit,
    onToggleSuspended: (UserProfile) -> Unit
) {
    val usersMap = ((usersState as? UiState.Success)?.data ?: emptyList()).associateBy { it.userId }
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Report, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No user reports submitted.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports, key = { it.id }) { report ->
            val targetUser = usersMap[report.targetUid]
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reported: ${report.targetName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (report.status == "open") MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = report.status.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (report.status == "open") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Reason: ${report.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Submitted ${AlapDateFormatter.formatChatTime(report.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(report, "resolved") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Resolve") }
                        OutlinedButton(
                            onClick = { onUpdateStatus(report, "dismissed") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Dismiss") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(report, "resolved") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Send Warning") }
                        if (targetUser != null && !targetUser.isSuspended) {
                            OutlinedButton(
                                onClick = {
                                    onToggleSuspended(targetUser)
                                    onUpdateStatus(report, "resolved")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Suspend User", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditLogTab(auditLogs: List<AdminAuditLog>) {
    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Immutable admin audit log is empty.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(auditLogs, key = { it.id }) { log ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = AlapDateFormatter.formatChatTime(log.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "By ${log.adminEmail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminApiKeysTab(
    apiKeys: List<AdminApiKey>,
    onCreateKey: (label: String, scopes: List<String>) -> Unit,
    onToggleActive: (AdminApiKey) -> Unit,
    onDeleteKey: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var label by remember { mutableStateOf("") }
    var selectedScopes by remember { mutableStateOf(AlapConstants.API_KEY_SCOPES.toSet()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Admin API Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Create restricted API keys strictly under Primary Admin access for your Web Admin Console, Update Server, and Netlify Functions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { if (it.length <= 80) label = it },
                        label = { Text("Key Label (e.g. Web Admin Portal)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_api_key_label_input")
                    )
                    Text("Permissions / Scopes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlapConstants.API_KEY_SCOPES.take(2).forEach { scope ->
                            FilterChip(
                                selected = scope in selectedScopes,
                                onClick = {
                                    selectedScopes = if (scope in selectedScopes) selectedScopes - scope else selectedScopes + scope
                                },
                                label = { Text(scope, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlapConstants.API_KEY_SCOPES.drop(2).forEach { scope ->
                            FilterChip(
                                selected = scope in selectedScopes,
                                onClick = {
                                    selectedScopes = if (scope in selectedScopes) selectedScopes - scope else selectedScopes + scope
                                },
                                label = { Text(scope, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onCreateKey(label.ifBlank { "Web Admin Portal" }, selectedScopes.toList())
                            label = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_api_key_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create API Key", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Issued Keys (${apiKeys.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(apiKeys, key = { it.id }) { keyItem ->
            val masked = if (keyItem.apiKey.length > 14) {
                keyItem.apiKey.take(14) + "••••••••" + keyItem.apiKey.takeLast(4)
            } else {
                keyItem.apiKey
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(keyItem.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = masked,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(keyItem.apiKey)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy API key")
                        }
                        Switch(
                            checked = keyItem.isActive,
                            onCheckedChange = { onToggleActive(keyItem) }
                        )
                        IconButton(onClick = { onDeleteKey(keyItem.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete API key", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (keyItem.scopes.isNotEmpty()) {
                        Text(
                            text = "Scopes: ${keyItem.scopes.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

