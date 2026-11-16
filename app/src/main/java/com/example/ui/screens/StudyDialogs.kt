package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AlapConstants
import com.example.data.model.StudyItem
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun CreateQuizDialog(
    onDismiss: () -> Unit,
    onCreateQuiz: (question: String, options: List<String>, correctIdx: Int, explanation: String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var opt4 by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    val rawOptions = listOf(opt1, opt2, opt3, opt4)
    val nonBlankIndexed = rawOptions.mapIndexedNotNull { i, text ->
        val clean = text.trim()
        if (clean.isNotBlank()) i to clean else null
    }
    val mappedCorrectIdx = nonBlankIndexed.indexOfFirst { it.first == correctIdx }.takeIf { it >= 0 } ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Create 1-Tap MCQ Quiz") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { if (it.length <= 200) question = it },
                    label = { Text("Quiz question") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_question_input")
                )
                Text(
                    text = "Tap the circle next to the correct answer:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    0 to Pair(opt1, "Option 1 (required)"),
                    1 to Pair(opt2, "Option 2 (required)"),
                    2 to Pair(opt3, "Option 3 (optional)"),
                    3 to Pair(opt4, "Option 4 (optional)")
                ).forEach { (idx, pair) ->
                    val (valText, labelText) = pair
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = correctIdx == idx,
                            onClick = { correctIdx = idx }
                        )
                        OutlinedTextField(
                            value = valText,
                            onValueChange = { next ->
                                val clipped = next.take(60)
                                when (idx) {
                                    0 -> opt1 = clipped
                                    1 -> opt2 = clipped
                                    2 -> opt3 = clipped
                                    else -> opt4 = clipped
                                }
                            },
                            label = { Text(labelText) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { if (it.length <= 300) explanation = it },
                    label = { Text("Explanation (shown after answer)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateQuiz(
                        question,
                        nonBlankIndexed.map { it.second },
                        mappedCorrectIdx,
                        explanation
                    )
                },
                enabled = question.isNotBlank() && nonBlankIndexed.size >= 2,
                modifier = Modifier.testTag("create_quiz_submit")
            ) { Text("Send Quiz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateFlashcardDialog(
    onDismiss: () -> Unit,
    onCreateFlashcard: (front: String, back: String) -> Unit
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Create Study Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Classmates can tap the card in chat to flip between question & answer offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = front,
                    onValueChange = { if (it.length <= 200) front = it },
                    label = { Text("Front (Question / Formula / Term)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_front_input")
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { if (it.length <= 500) back = it },
                    label = { Text("Back (Answer / Definition)") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_back_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateFlashcard(front, back) },
                enabled = front.isNotBlank() && back.isNotBlank(),
                modifier = Modifier.testTag("create_flashcard_submit")
            ) { Text("Send Flashcard") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinStudyCodeDialog(
    onDismiss: () -> Unit,
    onJoinCode: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Join Class Code")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your teacher or study group's 6-character Class Code to join without sharing phone numbers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { raw ->
                        code = raw.uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(16)
                    },
                    label = { Text("Class / Batch Code") },
                    placeholder = { Text("e.g. HSC26A") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_study_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoinCode(code) },
                enabled = code.trim().length >= 6,
                modifier = Modifier.testTag("join_study_code_submit")
            ) { Text("Join Group") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBoardSheet(
    chatTitle: String,
    currentUserId: String,
    isGroupAdmin: Boolean,
    canAddStudyItems: Boolean = true,
    studyItems: List<StudyItem>,
    onAddItem: (itemType: String, title: String, details: String, scheduleText: String, examDateEpochMs: Long) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeType by remember { mutableStateOf("note") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(studyItems, activeType) {
        val list = studyItems.filter { it.itemType == activeType }
        if (activeType == "exam") list.sortedBy { it.examDateEpochMs } else list
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Study Board",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Study Board",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$chatTitle • Cached offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (canAddStudyItems) {
                    Button(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("study_board_add_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Admin only",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "note" to "Notes",
                    "routine" to "Class Routine",
                    "exam" to "Exam Countdown"
                ).forEach { (typeKey, label) ->
                    val count = studyItems.count { it.itemType == typeKey }
                    FilterChip(
                        selected = activeType == typeKey,
                        onClick = { activeType = typeKey },
                        label = { Text(if (count > 0) "$label ($count)" else label) }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (!canAddStudyItems) {
                            "No items here yet.\nOnly Teachers or Group Admins can add Study Board items in this group."
                        } else {
                            when (activeType) {
                                "routine" -> "No class routine entries yet.\nTap Add to pin weekly class timings."
                                "exam" -> "No exam countdowns yet.\nTap Add to track upcoming test dates offline."
                                else -> "No pinned study notes yet.\nTap Add to save formulas, syllabus topics, or reminders."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val canDelete = item.createdBy == currentUserId || isGroupAdmin
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (item.itemType == "exam" && item.examDateEpochMs > 0L) {
                                            val diffDays = ceil((item.examDateEpochMs - System.currentTimeMillis()) / 86_400_000.0).toInt()
                                            val countdownLabel = when {
                                                diffDays > 1 -> "$diffDays days left"
                                                diffDays == 1 -> "Tomorrow"
                                                diffDays == 0 -> "Today!"
                                                else -> "Completed"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (diffDays in 0..3) {
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                }
                                            ) {
                                                Text(
                                                    text = countdownLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (diffDays in 0..3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (item.scheduleText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.scheduleText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (item.details.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Added by ${item.createdByName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                if (canDelete) {
                                    IconButton(onClick = { onDeleteItem(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete study item",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var scheduleText by remember { mutableStateOf("") }
        var daysFromToday by remember { mutableStateOf("7") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    when (activeType) {
                        "routine" -> "Add Class Routine"
                        "exam" -> "Add Exam Countdown"
                        else -> "Pin Study Note"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 100) title = it },
                        label = {
                            Text(
                                when (activeType) {
                                    "routine" -> "Subject / Class Name"
                                    "exam" -> "Exam Title (e.g. Physics Final)"
                                    else -> "Note Title"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activeType == "routine") {
                        OutlinedTextField(
                            value = scheduleText,
                            onValueChange = { if (it.length <= 80) scheduleText = it },
                            label = { Text("Day & Time (e.g. Sun & Tue 10:00 AM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (activeType == "exam") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("3", "7", "14", "30").forEach { d ->
                                FilterChip(
                                    selected = daysFromToday == d,
                                    onClick = { daysFromToday = d },
                                    label = { Text("+$d days") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = daysFromToday,
                            onValueChange = { raw -> daysFromToday = raw.filter { it.isDigit() }.take(3) },
                            label = { Text("Days from today") },
                            leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { if (it.length <= 400) details = it },
                        label = { Text("Details / Room / Syllabus (optional)") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysFromToday.toLongOrNull()?.coerceIn(0L, 365L) ?: 0L
                        val examEpoch = if (activeType == "exam") {
                            System.currentTimeMillis() + days * 86_400_000L
                        } else 0L
                        onAddItem(activeType, title, details, scheduleText, examEpoch)
                        showAddDialog = false
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("study_board_submit_item")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StudyPomodoroDialog(
    totalFocusMinutes: Int,
    completedSessions: Int,
    onSessionCompleted: (minutes: Int) -> Unit,
    onShareToChat: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var sessionFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, selectedMinutes) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds <= 0) {
                isRunning = false
                sessionFinished = true
                onSessionCompleted(selectedMinutes)
            }
        }
    }

    val totalSeconds = (selectedMinutes * 60).coerceAtLeast(1)
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val minsPart = remainingSeconds / 60
    val secsPart = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minsPart, secsPart)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pomodoro Study Focus")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlapConstants.STUDY_POMODORO_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = selectedMinutes == preset,
                            onClick = {
                                isRunning = false
                                sessionFinished = false
                                selectedMinutes = preset
                                remainingSeconds = preset * 60
                            },
                            label = { Text("${preset}m") }
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(156.dp)
                ) {
                    Canvas(modifier = Modifier.size(144.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sessionFinished) "Session Complete!" else if (isRunning) "Focusing..." else "Offline Timer",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sessionFinished) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (remainingSeconds <= 0) {
                                remainingSeconds = selectedMinutes * 60
                                sessionFinished = false
                            }
                            isRunning = !isRunning
                        },
                        modifier = Modifier.testTag("pomodoro_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause timer" else "Start timer",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "Pause" else "Start")
                    }
                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            sessionFinished = false
                            remainingSeconds = selectedMinutes * 60
                        },
                        modifier = Modifier.testTag("pomodoro_reset_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Offline Study Stats: $totalFocusMinutes mins • $completedSessions session(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        onShareToChat(selectedMinutes)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pomodoro_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Focus Milestone to Chat")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
