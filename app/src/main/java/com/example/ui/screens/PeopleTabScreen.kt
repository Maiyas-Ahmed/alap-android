package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AlapAvatar
import com.example.ui.components.StudentBadge
import com.example.ui.components.TeacherBadge
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalAlapColors

private enum class PeopleFilter { ALL, STUDENTS, TEACHERS }

@Composable
fun PeopleTabScreen(
    currentUserId: String,
    usersState: UiState<List<UserProfile>>,
    searchQuery: String,
    allowGroupCreation: Boolean,
    isReadOnlyMode: Boolean,
    onUserClick: (UserProfile) -> Unit,
    onCreateGroup: (title: String, description: String, memberUids: List<String>, isStudyGroup: Boolean, studyLockToAdmin: Boolean) -> Unit,
    onJoinStudyCode: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinStudyCodeDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(PeopleFilter.ALL) }
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var deviceContactPhones by remember { mutableStateOf(setOf<String>()) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactPermission = granted
        if (granted) {
            deviceContactPhones = fetchDeviceContacts(context)
        }
    }

    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission) {
            deviceContactPhones = fetchDeviceContacts(context)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = allowGroupCreation && !isReadOnlyMode) {
                        showCreateGroupDialog = true
                    }
                    .testTag("new_group_button")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GroupAdd,
                        contentDescription = "New group",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Group",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Create conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isReadOnlyMode) { showJoinStudyCodeDialog = true }
                    .testTag("join_class_code_button")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Join Class Code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Class Code",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Join Study Group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!hasContactPermission) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                        .testTag("sync_contacts_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contacts,
                            contentDescription = "Sync contacts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sync Contacts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Phonebook",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeFilter == PeopleFilter.ALL,
                onClick = { activeFilter = PeopleFilter.ALL },
                label = { Text("All People") },
                modifier = Modifier.testTag("people_filter_all")
            )
            FilterChip(
                selected = activeFilter == PeopleFilter.STUDENTS,
                onClick = { activeFilter = PeopleFilter.STUDENTS },
                leadingIcon = {
                    Icon(Icons.Filled.School, contentDescription = null)
                },
                label = { Text("Students (ST)") },
                modifier = Modifier.testTag("people_filter_students")
            )
            FilterChip(
                selected = activeFilter == PeopleFilter.TEACHERS,
                onClick = { activeFilter = PeopleFilter.TEACHERS },
                leadingIcon = {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
                },
                label = { Text("Teachers (TR)") },
                modifier = Modifier.testTag("people_filter_teachers")
            )
        }

        Text(
            text = when {
                activeFilter == PeopleFilter.STUDENTS -> "STUDENTS ON ALAP (ST)"
                activeFilter == PeopleFilter.TEACHERS -> "VERIFIED TEACHERS ON ALAP (TR)"
                hasContactPermission && deviceContactPhones.isNotEmpty() -> "PHONEBOOK & ALAP CONTACTS"
                else -> "PEOPLE ON ALAP"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        when (usersState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(usersState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val otherUsers = remember(usersState.data, currentUserId, searchQuery, deviceContactPhones, activeFilter) {
                    val q = searchQuery.trim().lowercase()
                    usersState.data.filter { user ->
                        val roleMatch = when (activeFilter) {
                            PeopleFilter.ALL -> true
                            PeopleFilter.STUDENTS -> user.isStudent
                            PeopleFilter.TEACHERS -> user.isTeacher
                        }
                        roleMatch && user.userId != currentUserId && !user.isSuspended && (
                            q.isBlank() ||
                                user.displayName.lowercase().contains(q) ||
                                (!user.hideUserId && user.handle.lowercase().contains(q)) ||
                                user.publicPhoneNumber.contains(q) ||
                                (user.isTeacher && user.teacherSubject.lowercase().contains(q)) ||
                                (user.isStudent && (user.studentClass.lowercase().contains(q) || user.studentSubject.lowercase().contains(q)))
                            )
                    }
                }

                if (otherUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No contacts match \"$searchQuery\"."
                            } else {
                                "No other contacts have signed in yet.\nInvite friends to sign in with their Gmail on Alap!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(otherUsers, key = { it.userId }) { user ->
                            val isPhoneMatched = deviceContactPhones.any { p -> user.publicPhoneNumber.contains(p) || (user.publicPhoneNumber.isNotBlank() && p.contains(user.publicPhoneNumber)) }
                            val subtitleParts = buildList {
                                if (isPhoneMatched) add("In phone contacts")
                                if (user.isTeacher && user.teacherSubject.isNotBlank()) add("Teacher • ${user.teacherSubject}")
                                if (user.isStudent && user.studentClass.isNotBlank()) add(user.studentClass)
                                if (user.isStudent && user.studentSubject.isNotBlank()) add(user.studentSubject)
                                if (!user.hideUserId && user.handle.isNotBlank()) {
                                    add("@${user.handle}")
                                }
                                if (!user.hideAbout && user.about.isNotBlank()) {
                                    add(user.about)
                                }
                            }
                            val subtitleText = if (subtitleParts.isEmpty()) {
                                "Alap contact"
                            } else {
                                subtitleParts.joinToString(" • ")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserClick(user) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("contact_row_${user.userId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlapAvatar(
                                    avatarId = user.avatarId,
                                    displayName = user.displayName,
                                    isOnline = user.isOnline && !user.hideOnlineStatus,
                                    size = 48.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (user.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge()
                                        }
                                        if (user.isTeacher) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TeacherBadge()
                                        }
                                        if (user.isStudent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StudentBadge()
                                        }
                                    }
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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

    if (showCreateGroupDialog && usersState is UiState.Success) {
        val candidates = usersState.data.filter { it.userId != currentUserId && !it.isSuspended }
        var groupTitle by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var isStudyGroup by remember { mutableStateOf(false) }
        var studyLockToAdmin by remember { mutableStateOf(false) }
        var selectedUids by remember { mutableStateOf(setOf<String>()) }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { if (it.length <= 60) groupTitle = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { if (it.length <= 160) groupDesc = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Study Group Mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Auto-creates a 6-char Class Code & Study Board",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isStudyGroup,
                            onCheckedChange = {
                                isStudyGroup = it
                                if (!it) studyLockToAdmin = false
                            },
                            modifier = Modifier.testTag("study_group_toggle")
                        )
                    }
                    if (isStudyGroup) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Teacher / Admin Only Study Posts", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Only group admins can add Study Board notes, quizzes & flashcards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = studyLockToAdmin,
                                onCheckedChange = { studyLockToAdmin = it },
                                modifier = Modifier.testTag("study_lock_toggle")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select members (${selectedUids.size} selected)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (candidates.isEmpty()) {
                        Text(
                            text = "No contacts available to add yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(candidates, key = { it.userId }) { user ->
                                val checked = user.userId in selectedUids
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUids = if (checked) {
                                                selectedUids - user.userId
                                            } else {
                                                selectedUids + user.userId
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selectedUids = if (checked) selectedUids - user.userId else selectedUids + user.userId
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
                                    if (user.isTeacher) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TeacherBadge()
                                    }
                                    if (user.isStudent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StudentBadge()
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateGroup(groupTitle, groupDesc, selectedUids.toList(), isStudyGroup, studyLockToAdmin)
                        showCreateGroupDialog = false
                    },
                    enabled = groupTitle.isNotBlank() && selectedUids.isNotEmpty(),
                    modifier = Modifier.testTag("confirm_create_group_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showJoinStudyCodeDialog) {
        JoinStudyCodeDialog(
            onDismiss = { showJoinStudyCodeDialog = false },
            onJoinCode = { code ->
                showJoinStudyCodeDialog = false
                onJoinStudyCode(code)
            }
        )
    }
}

private fun fetchDeviceContacts(context: Context): Set<String> {
    val phones = mutableSetOf<String>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (numIdx >= 0) {
                    val raw = it.getString(numIdx).orEmpty()
                    val cleaned = raw.filter { c -> c.isDigit() || c == '+' }
                    if (cleaned.isNotBlank()) {
                        phones.add(cleaned)
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignored
    }
    return phones
}
