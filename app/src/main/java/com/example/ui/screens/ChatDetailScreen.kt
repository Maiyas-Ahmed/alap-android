package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlapConstants
import com.example.data.model.AppConfig
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.StudyItem
import com.example.data.model.UiMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.ui.components.FlashcardMessageContent
import com.example.ui.components.MessageStatusTick
import com.example.ui.components.OfflineAndMaintenanceStrip
import com.example.ui.components.QuizMessageContent
import com.example.ui.components.StudentBadge
import com.example.ui.components.TeacherBadge
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalAlapColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    currentUserId: String,
    currentUserName: String,
    chat: Chat?,
    messagesState: UiState<List<UiMessage>>,
    draftText: String,
    allChats: List<Chat>,
    usersMap: Map<String, UserProfile>,
    appConfig: AppConfig,
    isOnline: Boolean,
    hideReadReceipts: Boolean,
    blockedUids: List<String>,
    studyItems: List<StudyItem> = emptyList(),
    studyFocusMinutesTotal: Int = 0,
    studySessionsCompleted: Int = 0,
    errorBanner: String?,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendText: (replyTo: ChatMessage?) -> Unit,
    onSendSticker: (stickerId: String, stickerLabel: String, replyTo: ChatMessage?) -> Unit,
    onSendPoll: (question: String, options: List<String>) -> Unit,
    onSendQuiz: (question: String, options: List<String>, correctIdx: Int, explanation: String) -> Unit = { _, _, _, _ -> },
    onSendFlashcard: (front: String, back: String) -> Unit = { _, _ -> },
    onVotePoll: (ChatMessage, Int) -> Unit,
    onAddStudyItem: (itemType: String, title: String, details: String, scheduleText: String, examDateEpochMs: Long) -> Unit = { _, _, _, _, _ -> },
    onDeleteStudyItem: (String) -> Unit = {},
    onEnableStudyInvite: () -> Unit = {},
    onCompleteStudySession: (Int) -> Unit = {},
    onShareStudySession: (Int) -> Unit = {},
    onSetDisappearingHours: (Long) -> Unit,
    onToggleBlockUser: (String) -> Unit,
    onReportUser: (targetUid: String, targetName: String, reason: String) -> Unit,
    onForwardMessage: (targetChat: Chat, original: ChatMessage) -> Unit,
    onToggleReaction: (ChatMessage, String) -> Unit,
    onToggleStar: (ChatMessage) -> Unit,
    onEditMessage: (ChatMessage, String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onMarkRead: (List<ChatMessage>, Boolean) -> Unit,
    onUpdateGroup: (String, String) -> Unit,
    onAddGroupMember: (String) -> Unit,
    onToggleStudyLock: () -> Unit = {},
    onLeaveGroup: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalAlapColors.current
    val listState = rememberLazyListState()

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showStickerTray by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showContactInfoDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showQuizDialog by remember { mutableStateOf(false) }
    var showFlashcardDialog by remember { mutableStateOf(false) }
    var showStudyBoard by remember { mutableStateOf(false) }
    var showPomodoroDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var infoMessageDialogTarget by remember { mutableStateOf<ChatMessage?>(null) }

    val isGroup = chat?.chatType == "group"
    val isGroupAdmin = chat != null && currentUserId in chat.adminUids
    val canPostStudyTools = !isGroup || chat?.studyLockToAdmin != true || isGroupAdmin
    val otherUid = chat?.otherParticipantUid(currentUserId)
    val otherUser = otherUid?.let { usersMap[it] }
    val isOtherBlocked = otherUid != null && otherUid in blockedUids
    val hideOtherOnline = otherUser?.hideOnlineStatus == true
    val headerTitle = if (isGroup) {
        chat?.title?.ifBlank { "Group Chat" } ?: "Group Chat"
    } else {
        otherUser?.displayName ?: "Alap Chat"
    }
    val typingOthers = chat?.typingUids?.filter { it != currentUserId } ?: emptyList()
    val timerBadge = when (chat?.disappearingHours) {
        24L -> " • 24h timer"
        168L -> " • 7d timer"
        else -> ""
    }
    val subtitle = when {
        isOtherBlocked -> "Blocked contact"
        typingOthers.isNotEmpty() -> "typing..."
        isGroup -> "${chat?.participantUids?.size ?: 0} members$timerBadge"
        hideOtherOnline -> "Tap here for contact info$timerBadge"
        otherUser?.isOnline == true -> "Online$timerBadge"
        otherUser?.lastSeenAt != null -> "Last seen ${AlapDateFormatter.formatChatTime(otherUser.lastSeenAt)}$timerBadge"
        else -> "Encrypted text chat$timerBadge"
    }

    val rawMessages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val disappearingCutoffMs = remember(chat?.disappearingHours) {
        val hours = chat?.disappearingHours ?: 0L
        if (hours > 0L) System.currentTimeMillis() - (hours * 3600_000L) else 0L
    }
    val nonExpiredMessages = remember(rawMessages, disappearingCutoffMs) {
        if (disappearingCutoffMs <= 0L) rawMessages
        else rawMessages.filter { ui ->
            val createdMs = ui.message.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            createdMs >= disappearingCutoffMs
        }
    }

    LaunchedEffect(nonExpiredMessages.size, hideReadReceipts) {
        if (nonExpiredMessages.isNotEmpty()) {
            listState.animateScrollToItem(nonExpiredMessages.lastIndex)
            onMarkRead(nonExpiredMessages.map { it.message }, hideReadReceipts)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (isGroup) showGroupInfoDialog = true else if (otherUser != null) showContactInfoDialog = true
                            }
                        ) {
                            AlapAvatar(
                                avatarId = if (isGroup) "group_default" else (otherUser?.avatarId ?: "avatar_emerald"),
                                displayName = headerTitle,
                                isGroup = isGroup,
                                isOnline = !isOtherBlocked && otherUser?.isOnline == true && !hideOtherOnline,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = headerTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (!isGroup && otherUser?.isVerified == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge()
                                    }
                                    if (!isGroup && otherUser?.isTeacher == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TeacherBadge()
                                    }
                                    if (!isGroup && otherUser?.isStudent == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StudentBadge()
                                    } else if (isGroup && chat?.isStudyGroup == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StudentBadge(label = "STUDY")
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (typingOthers.isNotEmpty()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        extendedColors.mutedText
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showStudyBoard = true },
                            modifier = Modifier.testTag("topbar_study_board_button")
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "Study Board", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showPomodoroDialog = true },
                            modifier = Modifier.testTag("topbar_pomodoro_button")
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = "Pomodoro Study Timer")
                        }
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search in chat")
                        }
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("chat_overflow_button")
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Chat options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = Color.White
                            ) {
                                if (isGroup) {
                                    DropdownMenuItem(
                                        text = { Text("Group info & members") },
                                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showGroupInfoDialog = true
                                        }
                                    )
                                } else if (otherUser != null) {
                                    DropdownMenuItem(
                                        text = { Text("View contact info") },
                                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showContactInfoDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (canPostStudyTools) "Create MCQ Quiz" else "Create MCQ Quiz (Admin only)") },
                                    leadingIcon = { Icon(Icons.Filled.Quiz, contentDescription = null) },
                                    enabled = canPostStudyTools,
                                    onClick = {
                                        showOverflowMenu = false
                                        showQuizDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (canPostStudyTools) "Create Flashcard" else "Create Flashcard (Admin only)") },
                                    leadingIcon = { Icon(Icons.Filled.Style, contentDescription = null) },
                                    enabled = canPostStudyTools,
                                    onClick = {
                                        showOverflowMenu = false
                                        showFlashcardDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Study Board (Notes & Exams)") },
                                    leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showStudyBoard = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pomodoro Study Timer") },
                                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showPomodoroDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Create Poll") },
                                    leadingIcon = { Icon(Icons.Filled.Poll, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showPollDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Disappearing Messages") },
                                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showTimerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Quick Stickers") },
                                    leadingIcon = { Icon(Icons.Filled.EmojiEmotions, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showStickerTray = !showStickerTray
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export chat (.txt)") },
                                    leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        exportChatAsText(context, headerTitle, nonExpiredMessages.map { it.message })
                                    }
                                )
                                if (!isGroup && otherUid != null) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isOtherBlocked) "Unblock Contact" else "Block Contact",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onToggleBlockUser(otherUid)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Report Contact") },
                                        leadingIcon = { Icon(Icons.Filled.Report, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showReportDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search messages...") },
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
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                OfflineAndMaintenanceStrip(
                    isOnline = isOnline,
                    isReadOnlyMode = appConfig.isReadOnlyMode,
                    maintenanceBanner = appConfig.maintenanceBanner
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .background(extendedColors.chatWallpaper)
        ) {
            if (errorBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) { Text("Dismiss") }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                ChatPatternBackground(
                    dotColor = extendedColors.mutedText.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxSize()
                )

                when (messagesState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(messagesState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is UiState.Success -> {
                        val visible = remember(nonExpiredMessages, searchQuery) {
                            val q = searchQuery.trim().lowercase()
                            if (q.isBlank()) nonExpiredMessages
                            else nonExpiredMessages.filter {
                                it.message.text.lowercase().contains(q) ||
                                    it.message.pollQuestion.orEmpty().lowercase().contains(q) ||
                                    it.message.flashcardFront.orEmpty().lowercase().contains(q) ||
                                    it.message.flashcardBack.orEmpty().lowercase().contains(q) ||
                                    it.message.quizExplanation.orEmpty().lowercase().contains(q) ||
                                    it.message.pollOptions.orEmpty().any { opt -> opt.lowercase().contains(q) }
                            }
                        }

                        if (visible.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No matching messages." else "Messages are lightweight and cached offline.\nSwipe right on any message to reply!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(visible, key = { it.message.id }) { uiMsg ->
                                    val msg = uiMsg.message
                                    if (msg.messageType == "system") {
                                        SystemEventPill(text = msg.text)
                                    } else {
                                        MessageBubble(
                                            uiMessage = uiMsg,
                                            isMine = msg.senderId == currentUserId,
                                            isGroup = isGroup,
                                            currentUserId = currentUserId,
                                            onSwipeToReply = { replyTarget = msg },
                                            onVotePoll = { optionIdx -> onVotePoll(msg, optionIdx) },
                                            onClickOrLongPress = { selectedMessage = msg }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = replyTarget != null) {
                replyTarget?.let { target ->
                    Surface(
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.senderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (target.isDeleted) "Deleted message" else target.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyTarget = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showStickerTray) {
                Surface(
                    color = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AlapConstants.STICKER_OPTIONS.forEach { (id, label) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable {
                                    onSendSticker(id, label, replyTarget)
                                    replyTarget = null
                                    showStickerTray = false
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Composer Bar
            Surface(
                color = extendedColors.chatWallpaper,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                when {
                    isOtherBlocked && otherUid != null -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "You blocked this contact.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onToggleBlockUser(otherUid) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }

                    appConfig.isReadOnlyMode -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sending paused (Read-only mode)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showStickerTray = !showStickerTray },
                                        modifier = Modifier.testTag("toggle_stickers_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEmotions,
                                            contentDescription = "Quick stickers",
                                            tint = extendedColors.mutedText
                                        )
                                    }
                                    TextField(
                                        value = draftText,
                                        onValueChange = {
                                            if (it.length <= appConfig.maxMessageLength.toInt()) {
                                                onDraftChanged(it)
                                            }
                                        },
                                        placeholder = { Text("Message") },
                                        maxLines = 5,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("message_composer_input")
                                    )
                                    IconButton(
                                        onClick = { showPollDialog = true },
                                        modifier = Modifier.testTag("open_poll_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Poll,
                                            contentDescription = "Create poll",
                                            tint = extendedColors.mutedText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = if (draftText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = draftText.isNotBlank()) {
                                        onSendText(replyTarget)
                                        replyTarget = null
                                    }
                                    .testTag("send_message_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send message",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMessage?.let { msg ->
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AlapConstants.QUICK_REACTIONS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onToggleReaction(msg, emoji)
                                    selectedMessage = null
                                }
                                .padding(6.dp)
                        )
                    }
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Reply") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                    modifier = Modifier.clickable {
                        replyTarget = msg
                        selectedMessage = null
                    }
                )
                if (!msg.isDeleted) {
                    ListItem(
                        headlineContent = { Text("Forward") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
                        modifier = Modifier.clickable {
                            forwardingMessage = msg
                            selectedMessage = null
                        }
                    )
                    ListItem(
                        headlineContent = {
                            Text(if (currentUserId in msg.starredByUids) "Unstar message" else "Star message")
                        },
                        leadingContent = {
                            Icon(
                                if (currentUserId in msg.starredByUids) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            onToggleStar(msg)
                            selectedMessage = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Copy text") },
                        leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Alap message", msg.text))
                            selectedMessage = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Message Info") },
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        modifier = Modifier.clickable {
                            infoMessageDialogTarget = msg
                            selectedMessage = null
                        }
                    )
                }
                if (msg.senderId == currentUserId && !msg.isDeleted) {
                    if (msg.messageType == "text") {
                        ListItem(
                            headlineContent = { Text("Edit message") },
                            leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            modifier = Modifier.clickable {
                                editingMessage = msg
                                selectedMessage = null
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Delete for everyone", color = MaterialTheme.colorScheme.error) },
                        leadingContent = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.clickable {
                            onDeleteMessage(msg)
                            selectedMessage = null
                        }
                    )
                }
            }
        }
    }

    editingMessage?.let { target ->
        EditMessageDialog(
            target = target,
            onDismiss = { editingMessage = null },
            onSave = { newText ->
                onEditMessage(target, newText)
                editingMessage = null
            }
        )
    }

    forwardingMessage?.let { targetMsg ->
        ForwardMessageDialog(
            currentChatId = chat?.id,
            currentUserId = currentUserId,
            allChats = allChats,
            usersMap = usersMap,
            onDismiss = { forwardingMessage = null },
            onSelectTarget = { destChat ->
                onForwardMessage(destChat, targetMsg)
                forwardingMessage = null
            }
        )
    }

    if (showPollDialog) {
        CreatePollDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { q, opts ->
                onSendPoll(q, opts)
                showPollDialog = false
            }
        )
    }

    if (showQuizDialog) {
        CreateQuizDialog(
            onDismiss = { showQuizDialog = false },
            onCreateQuiz = { q, opts, correctIdx, explanation ->
                onSendQuiz(q, opts, correctIdx, explanation)
                showQuizDialog = false
            }
        )
    }

    if (showFlashcardDialog) {
        CreateFlashcardDialog(
            onDismiss = { showFlashcardDialog = false },
            onCreateFlashcard = { front, back ->
                onSendFlashcard(front, back)
                showFlashcardDialog = false
            }
        )
    }

    if (showStudyBoard) {
        StudyBoardSheet(
            chatTitle = headerTitle,
            currentUserId = currentUserId,
            isGroupAdmin = isGroupAdmin,
            canAddStudyItems = canPostStudyTools,
            studyItems = studyItems,
            onAddItem = onAddStudyItem,
            onDeleteItem = onDeleteStudyItem,
            onDismiss = { showStudyBoard = false }
        )
    }

    if (showPomodoroDialog) {
        StudyPomodoroDialog(
            totalFocusMinutes = studyFocusMinutesTotal,
            completedSessions = studySessionsCompleted,
            onSessionCompleted = onCompleteStudySession,
            onShareToChat = onShareStudySession,
            onDismiss = { showPomodoroDialog = false }
        )
    }

    if (showTimerDialog && chat != null) {
        DisappearingTimerDialog(
            currentHours = chat.disappearingHours,
            onDismiss = { showTimerDialog = false },
            onSelectHours = onSetDisappearingHours
        )
    }

    if (showReportDialog && otherUser != null) {
        ReportUserDialog(
            targetName = otherUser.displayName,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason ->
                onReportUser(otherUser.userId, otherUser.displayName, reason)
                showReportDialog = false
            }
        )
    }

    if (showGroupInfoDialog && chat != null && isGroup) {
        GroupInfoDialog(
            chat = chat,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            usersMap = usersMap,
            onDismiss = { showGroupInfoDialog = false },
            onUpdateGroup = onUpdateGroup,
            onAddGroupMember = onAddGroupMember,
            onEnableStudyInvite = onEnableStudyInvite,
            onToggleStudyLock = onToggleStudyLock,
            onLeaveGroup = onLeaveGroup
        )
    }

    if (showContactInfoDialog && otherUser != null && !isGroup) {
        ContactInfoDialog(
            otherUser = otherUser,
            isBlocked = isOtherBlocked,
            onToggleBlock = { onToggleBlockUser(otherUser.userId) },
            onReportClick = { showReportDialog = true },
            onDismiss = { showContactInfoDialog = false }
        )
    }

    infoMessageDialogTarget?.let { msg ->
        MessageInfoDialog(
            msg = msg,
            usersMap = usersMap,
            onDismiss = { infoMessageDialogTarget = null }
        )
    }
}

private fun exportChatAsText(context: Context, title: String, messages: List<ChatMessage>) {
    val lines = messages.joinToString("\n") { msg ->
        val time = AlapDateFormatter.formatChatTime(msg.createdAt)
        val body = if (msg.isDeleted) "[deleted]" else msg.text
        "[$time] ${msg.senderName}: $body"
    }.ifBlank { "No messages in $title." }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Alap Chat Export - $title")
        putExtra(Intent.EXTRA_TEXT, lines)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export $title (.txt)"))
}

@Composable
private fun ChatPatternBackground(
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val step = 34.dp.toPx()
        var x = step / 2f
        while (x < size.width) {
            var y = step / 2f
            while (y < size.height) {
                drawCircle(color = dotColor, radius = 1.6.dp.toPx(), center = Offset(x, y))
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun SystemEventPill(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    uiMessage: UiMessage,
    isMine: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    onSwipeToReply: () -> Unit,
    onVotePoll: (Int) -> Unit,
    onClickOrLongPress: () -> Unit
) {
    val msg = uiMessage.message
    val extended = LocalAlapColors.current
    val bubbleColor = if (isMine) extended.outgoingBubble else extended.incomingBubble
    val textColor = if (isMine) extended.onOutgoingBubble else extended.onIncomingBubble
    val isReadByOther = msg.readByUids.any { it != currentUserId }
    val isStarred = currentUserId in msg.starredByUids

    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragOffsetX = (dragOffsetX + delta).coerceIn(0f, 120f)
                },
                onDragStopped = {
                    if (dragOffsetX >= 72f) {
                        onSwipeToReply()
                    }
                    dragOffsetX = 0f
                }
            ),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMine) 14.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 14.dp
            ),
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(
                    onClick = onClickOrLongPress,
                    onLongClick = onClickOrLongPress
                )
                .testTag("message_bubble_${msg.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !isMine) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                if (msg.isForwarded) {
                    Text(
                        text = "Forwarded",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = extended.mutedText
                    )
                }
                if (!msg.replyToPreview.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(
                                text = msg.replyToSenderName ?: "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = msg.replyToPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                when {
                    msg.messageType == "poll" && !msg.isDeleted -> {
                        val options = msg.pollOptions.orEmpty()
                        val votes = msg.pollVotes.orEmpty()
                        val totalVotes = votes.size.coerceAtLeast(1)
                        val myVote = (votes[currentUserId] as? Number)?.toInt()

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = msg.pollQuestion ?: msg.text,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            options.forEachIndexed { idx, optLabel ->
                                val count = votes.values.count { (it as? Number)?.toInt() == idx }
                                val pct = if (votes.isEmpty()) 0f else count.toFloat() / totalVotes.toFloat()
                                val selected = myVote == idx
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.8f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onVotePoll(idx) }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = optLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${votes.size} vote(s) • Tap to vote",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.mutedText
                            )
                        }
                    }

                    msg.messageType == "quiz" && !msg.isDeleted -> {
                        QuizMessageContent(
                            msg = msg,
                            currentUserId = currentUserId,
                            textColor = textColor,
                            onSelectOption = onVotePoll
                        )
                    }

                    msg.messageType == "flashcard" && !msg.isDeleted -> {
                        FlashcardMessageContent(
                            msg = msg,
                            textColor = textColor
                        )
                    }

                    msg.messageType == "sticker" && !msg.isDeleted -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = if (msg.isDeleted) FontStyle.Italic else FontStyle.Normal,
                            color = if (msg.isDeleted) extended.mutedText else textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Starred",
                            tint = extended.mutedText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (msg.isEdited && !msg.isDeleted) {
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.mutedText
                        )
                    }
                    Text(
                        text = AlapDateFormatter.formatMessageTime(msg.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.mutedText
                    )
                    if (isMine) {
                        MessageStatusTick(
                            isPendingSync = uiMessage.isPendingSync,
                            isReadByOther = isReadByOther
                        )
                    }
                }
            }
        }

        if (msg.reactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp)
            ) {
                Text(
                    text = msg.reactions.values.groupingBy { it }.eachCount()
                        .entries.joinToString(" ") { (emoji, count) ->
                            if (count > 1) "$emoji $count" else emoji
                        },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageInfoDialog(
    msg: ChatMessage,
    usersMap: Map<String, UserProfile>,
    onDismiss: () -> Unit
) {
    val seenUids = msg.readByUids
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sender: ${msg.senderName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Sent at: ${AlapDateFormatter.formatChatTime(msg.createdAt)}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Read / Seen By (${seenUids.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (seenUids.isEmpty()) {
                    Text("Delivered (Not yet viewed)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    seenUids.forEach { uid ->
                        val uName = usersMap[uid]?.displayName ?: "User $uid"
                        Text("• $uName", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
