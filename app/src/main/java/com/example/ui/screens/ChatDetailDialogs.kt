package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.Chat
import com.example.data.model.ChatMessage
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.AlapDateFormatter
import com.example.ui.components.VerifiedBadge

@Composable
fun EditMessageDialog(
    target: ChatMessage,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editedText by remember(target.id) { mutableStateOf(target.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Edit Message") },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedText) },
                enabled = editedText.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ForwardMessageDialog(
    currentChatId: String?,
    currentUserId: String,
    allChats: List<Chat>,
    usersMap: Map<String, UserProfile>,
    onDismiss: () -> Unit,
    onSelectTarget: (Chat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Forward to...") },
        text = {
            LazyColumn {
                items(allChats.filter { it.id != currentChatId }, key = { it.id }) { destChat ->
                    val title = if (destChat.chatType == "group") {
                        destChat.title
                    } else {
                        destChat.otherParticipantUid(currentUserId)?.let { usersMap[it]?.displayName } ?: "Alap Contact"
                    }
                    ListItem(
                        headlineContent = { Text(title) },
                        modifier = Modifier.clickable { onSelectTarget(destChat) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var opt4 by remember { mutableStateOf("") }

    val options = listOf(opt1, opt2, opt3, opt4).map { it.trim() }.filter { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Create Poll") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { if (it.length <= 160) question = it },
                    label = { Text("Ask a question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opt1,
                    onValueChange = { if (it.length <= 60) opt1 = it },
                    label = { Text("Option 1 (required)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opt2,
                    onValueChange = { if (it.length <= 60) opt2 = it },
                    label = { Text("Option 2 (required)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opt3,
                    onValueChange = { if (it.length <= 60) opt3 = it },
                    label = { Text("Option 3 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = opt4,
                    onValueChange = { if (it.length <= 60) opt4 = it },
                    label = { Text("Option 4 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreatePoll(question, options) },
                enabled = question.isNotBlank() && options.size >= 2
            ) { Text("Send Poll") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DisappearingTimerDialog(
    currentHours: Long,
    onDismiss: () -> Unit,
    onSelectHours: (Long) -> Unit
) {
    val choices = listOf(
        0L to "Off (Keep messages)",
        24L to "24 Hours",
        168L to "7 Days"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Disappearing Messages") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Hide older messages automatically after the selected timer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                choices.forEach { (hours, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectHours(hours)
                                onDismiss()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentHours == hours,
                            onClick = {
                                onSelectHours(hours)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ReportUserDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (String) -> Unit
) {
    val presets = listOf("Spam or scam", "Harassment", "Impersonation", "Other")
    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var extraDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Report $targetName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reports are reviewed privately by Alap Admins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { reason ->
                        FilterChip(
                            selected = selectedPreset == reason,
                            onClick = { selectedPreset = reason },
                            label = { Text(reason) }
                        )
                    }
                }
                OutlinedTextField(
                    value = extraDetails,
                    onValueChange = { if (it.length <= 200) extraDetails = it },
                    label = { Text("Additional details (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    val finalReason = if (extraDetails.isBlank()) selectedPreset else "$selectedPreset: ${extraDetails.trim()}"
                    onSubmitReport(finalReason)
                }
            ) { Text("Submit Report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GroupInfoDialog(
    chat: Chat,
    currentUserId: String,
    currentUserName: String,
    usersMap: Map<String, UserProfile>,
    onDismiss: () -> Unit,
    onUpdateGroup: (String, String) -> Unit,
    onAddGroupMember: (String) -> Unit,
    onLeaveGroup: () -> Unit
) {
    val isGroupAdmin = currentUserId in chat.adminUids
    var newTitle by remember(chat.id) { mutableStateOf(chat.title) }
    var newDesc by remember(chat.id) { mutableStateOf(chat.description) }
    val nonMembers = usersMap.values.filter { it.userId !in chat.participantUids && !it.isSuspended }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Group Info") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isGroupAdmin) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateGroup(newTitle, newDesc) },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Update Info") }
                } else {
                    Text(chat.title, style = MaterialTheme.typography.titleMedium)
                    if (chat.description.isNotBlank()) {
                        Text(chat.description, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text(
                    text = "Members (${chat.participantUids.size})",
                    style = MaterialTheme.typography.labelMedium
                )
                chat.participantUids.forEach { uid ->
                    val memberName = if (uid == currentUserId) currentUserName else (usersMap[uid]?.displayName ?: uid.take(8))
                    val roleLabel = if (uid in chat.adminUids) " • Admin" else ""
                    Text("$memberName$roleLabel", style = MaterialTheme.typography.bodySmall)
                }

                if (isGroupAdmin && nonMembers.isNotEmpty()) {
                    Text("Add Member", style = MaterialTheme.typography.labelMedium)
                    nonMembers.take(5).forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddGroupMember(candidate.userId) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(candidate.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onLeaveGroup()
            }) {
                Text("Leave Group", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun ContactInfoDialog(
    otherUser: UserProfile,
    isBlocked: Boolean,
    onToggleBlock: () -> Unit,
    onReportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlapAvatar(
                    avatarId = otherUser.avatarId,
                    displayName = otherUser.displayName,
                    isOnline = otherUser.isOnline && !otherUser.hideOnlineStatus,
                    size = 46.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(otherUser.displayName, style = MaterialTheme.typography.titleMedium)
                        if (otherUser.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge()
                        }
                    }
                    Text(
                        text = when {
                            otherUser.hideOnlineStatus -> "Online status hidden"
                            otherUser.isOnline -> "Online now"
                            otherUser.lastSeenAt != null -> "Last seen ${AlapDateFormatter.formatChatTime(otherUser.lastSeenAt)}"
                            else -> "Offline"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("Mobile Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (otherUser.hidePhoneNumber || otherUser.publicPhoneNumber.isBlank()) {
                            "Hidden by privacy settings"
                        } else {
                            otherUser.publicPhoneNumber
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (otherUser.hidePhoneNumber || otherUser.publicPhoneNumber.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                HorizontalDivider()
                Column {
                    Text("Alap User ID & Handle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (otherUser.hideUserId || otherUser.publicUserId.isBlank()) {
                            "Hidden by privacy settings"
                        } else {
                            "${otherUser.publicUserId} (@${otherUser.handle})"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (otherUser.hideUserId || otherUser.publicUserId.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                HorizontalDivider()
                Column {
                    Text("About", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (otherUser.hideAbout) "Hidden by privacy settings" else otherUser.about,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (otherUser.hideAbout) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleBlock,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBlocked) "Unblock" else "Block",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onReportClick()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
