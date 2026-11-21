package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.Chat
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.ui.components.GuestBadge
import com.example.ui.components.StudentBadge
import com.example.ui.components.TeacherBadge
import com.example.ui.components.UpdateBannerCard
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalAlapColors
import com.example.util.ApkUpdateManager

enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    GROUPS("Groups"),
    STUDY("Study"),
    ARCHIVED("Archived")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    currentUserId: String,
    chatsState: UiState<List<Chat>>,
    usersMap: Map<String, UserProfile>,
    blockedUids: Set<String> = emptySet(),
    activeAnnouncement: Announcement?,
    latestApkUrl: String = "",
    searchQuery: String,
    onChatClick: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onTogglePin: (Chat) -> Unit,
    onToggleMute: (Chat) -> Unit,
    onToggleArchive: (Chat) -> Unit,
    onDismissAnnouncement: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var actionChat by remember { mutableStateOf<Chat?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (activeAnnouncement != null) {
            UpdateBannerCard(
                announcement = activeAnnouncement,
                onViewDetails = { showAnnouncementDialog = true },
                onDismiss = { onDismissAnnouncement(activeAnnouncement.id) }
            )
        }

        val unreadChatCount = remember(chatsState) {
            ((chatsState as? UiState.Success)?.data ?: emptyList())
                .count { !it.isArchivedBy(currentUserId) && it.unreadCountFor(currentUserId) > 0 }
        }

        // Filter Chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatFilter.entries.forEach { filter ->
                val chipLabel = if (filter == ChatFilter.UNREAD && unreadChatCount > 0) {
                    "Unread ($unreadChatCount)"
                } else {
                    filter.label
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(chipLabel, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("chat_filter_${filter.name.lowercase()}")
                )
            }
        }

        when (chatsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chatsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Success -> {
                val filteredChats = remember(chatsState.data, selectedFilter, searchQuery, usersMap) {
                    chatsState.data.filter { chat ->
                        val isArchived = chat.isArchivedBy(currentUserId)
                        val otherUser = chat.otherParticipantUid(currentUserId)?.let { usersMap[it] }
                        val filterMatches = when (selectedFilter) {
                            ChatFilter.ALL -> !isArchived
                            ChatFilter.UNREAD -> !isArchived && chat.unreadCountFor(currentUserId) > 0
                            ChatFilter.GROUPS -> !isArchived && chat.chatType == "group"
                            ChatFilter.STUDY -> !isArchived && (chat.isStudyGroup || otherUser?.isStudent == true)
                            ChatFilter.ARCHIVED -> isArchived
                        }
                        if (!filterMatches) return@filter false
                        if (searchQuery.isBlank()) return@filter true

                        val q = searchQuery.trim().lowercase()
                        val title = if (chat.chatType == "group") chat.title else otherUser?.displayName ?: "Alap User"
                        title.lowercase().contains(q) || chat.lastMessageText.lowercase().contains(q)
                    }
                }

                if (filteredChats.isEmpty()) {
                    EmptyChatsState(
                        filter = selectedFilter,
                        hasSearch = searchQuery.isNotBlank(),
                        onStartNewChat = onStartNewChat
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(filteredChats, key = { it.id }) { chat ->
                            val otherUid = chat.otherParticipantUid(currentUserId)
                            val otherUser = otherUid?.let { usersMap[it] }
                            ChatListRow(
                                chat = chat,
                                currentUserId = currentUserId,
                                otherUser = otherUser,
                                isBlocked = otherUid != null && otherUid in blockedUids,
                                onClick = { onChatClick(chat.id) },
                                onLongClick = { actionChat = chat }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = LocalAlapColors.current.subtleDivider.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    actionChat?.let { target ->
        ModalBottomSheet(onDismissRequest = { actionChat = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = {
                        Text(if (target.isPinnedBy(currentUserId)) "Unpin chat" else "Pin chat")
                    },
                    leadingContent = {
                        Icon(Icons.Filled.PushPin, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onTogglePin(target)
                        actionChat = null
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(if (target.isMutedBy(currentUserId)) "Unmute notifications" else "Mute notifications")
                    },
                    leadingContent = {
                        Icon(Icons.Filled.NotificationsOff, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onToggleMute(target)
                        actionChat = null
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(if (target.isArchivedBy(currentUserId)) "Unarchive chat" else "Archive chat")
                    },
                    leadingContent = {
                        Icon(
                            if (target.isArchivedBy(currentUserId)) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onToggleArchive(target)
                        actionChat = null
                    }
                )
            }
        }
    }

    if (showAnnouncementDialog && activeAnnouncement != null) {
        val canDirectUpdate = latestApkUrl.isNotBlank() &&
            (activeAnnouncement.priority == "update" || activeAnnouncement.versionTag.isNotBlank())
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            title = {
                Text(
                    text = buildString {
                        append(activeAnnouncement.title)
                        if (activeAnnouncement.versionTag.isNotBlank()) {
                            append(" (${activeAnnouncement.versionTag})")
                        }
                    }
                )
            },
            text = { Text(activeAnnouncement.message) },
            confirmButton = {
                if (canDirectUpdate) {
                    Button(
                        onClick = {
                            ApkUpdateManager.startUpdateDownload(
                                context = context,
                                apkUrl = latestApkUrl,
                                versionTag = activeAnnouncement.versionTag
                            )
                            Toast.makeText(
                                context,
                                "আপডেট ডাউনলোড শুরু হয়েছে...",
                                Toast.LENGTH_SHORT
                            ).show()
                            showAnnouncementDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("আপডেট করুন")
                    }
                } else {
                    TextButton(onClick = { showAnnouncementDialog = false }) {
                        Text("Got it")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDismissAnnouncement(activeAnnouncement.id)
                            showAnnouncementDialog = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                    if (canDirectUpdate) {
                        TextButton(onClick = { showAnnouncementDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListRow(
    chat: Chat,
    currentUserId: String,
    otherUser: UserProfile?,
    isBlocked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isGroup = chat.chatType == "group"
    val displayTitle = if (isGroup) {
        chat.title.ifBlank { "Group Chat" }
    } else {
        otherUser?.displayName ?: "Alap Contact"
    }
    val avatarId = if (isGroup) "group_default" else (otherUser?.avatarId ?: chat.avatarId)
    val unread = chat.unreadCountFor(currentUserId)
    val isPinned = chat.isPinnedBy(currentUserId)
    val isMuted = chat.isMutedBy(currentUserId)
    val isTyping = !isBlocked && chat.typingUids.any { it != currentUserId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_row_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlapAvatar(
            avatarId = avatarId,
            displayName = displayTitle,
            isGroup = isGroup,
            isOnline = !isBlocked && otherUser?.isOnline == true && otherUser.hideOnlineStatus != true,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isOtherGuest = !isGroup && otherUser?.isGuestAccount() == true
                    Text(
                        text = if (isOtherGuest) "Guest" else displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isOtherGuest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GuestBadge()
                    }
                    if (!isGroup && !isOtherGuest && otherUser?.isVerified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge()
                    }
                    if (!isGroup && !isOtherGuest && otherUser?.isTeacher == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TeacherBadge()
                    }
                    if (!isGroup && !isOtherGuest && otherUser?.isStudent == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StudentBadge()
                    } else if (isGroup && chat.isStudyGroup) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StudentBadge(label = "STUDY")
                    }
                    if (isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AlapDateFormatter.formatChatTime(chat.lastMessageAt ?: chat.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread > 0) MaterialTheme.colorScheme.primary else LocalAlapColors.current.mutedText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTyping) {
                        Text(
                            text = "typing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        if (chat.lastMessageSenderId == currentUserId && chat.lastMessageType != "system") {
                            Icon(
                                imageVector = if (unread == 0) Icons.Filled.DoneAll else Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (unread == 0) LocalAlapColors.current.mutedText else LocalAlapColors.current.mutedText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = chat.lastMessageText.ifBlank { "No messages yet" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalAlapColors.current.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = "Muted",
                            tint = LocalAlapColors.current.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = LocalAlapColors.current.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (unread > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsState(
    filter: ChatFilter,
    hasSearch: Boolean,
    onStartNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.size(150.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_chats),
                contentDescription = "No chats yet",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = when {
                hasSearch -> "No matching conversations"
                filter == ChatFilter.ARCHIVED -> "No archived chats"
                filter == ChatFilter.UNREAD -> "All caught up!"
                filter == ChatFilter.GROUPS -> "No group chats yet"
                filter == ChatFilter.STUDY -> "No study chats yet"
                else -> "Start your first Alap"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasSearch) {
                "Try searching another contact name or message keyword."
            } else {
                "Connect with people on Alap using ultra-light offline-friendly messaging."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasSearch && filter == ChatFilter.ALL) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onStartNewChat) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Find People to Message")
            }
        }
    }
}
