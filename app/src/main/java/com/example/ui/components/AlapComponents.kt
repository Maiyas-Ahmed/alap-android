package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.ui.theme.LocalAlapColors
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AlapAvatar(
    avatarId: String,
    displayName: String,
    isGroup: Boolean = false,
    isOnline: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        if (isGroup) {
            Image(
                painter = painterResource(id = R.drawable.img_avatar_group),
                contentDescription = "$displayName group avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            val bgColor = when (avatarId) {
                "avatar_sage" -> Color(0xFF2E6F5E)
                "avatar_slate" -> Color(0xFF475866)
                "avatar_charcoal" -> Color(0xFF2B3942)
                "avatar_sand" -> Color(0xFF7D6B52)
                "avatar_teal" -> Color(0xFF006D5B)
                else -> MaterialTheme.colorScheme.primary
            }
            val initials = displayName
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "A" }

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        if (isOnline && !isGroup) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified account",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp)
    )
}

@Composable
fun StudentBadge(
    modifier: Modifier = Modifier,
    label: String = "ST"
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.testTag("student_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "Student badge",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TeacherBadge(
    modifier: Modifier = Modifier,
    label: String = "TR"
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFF3CD),
        modifier = modifier.testTag("teacher_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = "Verified Teacher badge",
                tint = Color(0xFF9A5B00),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9A5B00)
            )
        }
    }
}

@Composable
fun MessageStatusTick(
    isPendingSync: Boolean,
    isReadByOther: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = LocalAlapColors.current
    when {
        isPendingSync -> Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Queued offline",
            tint = extended.mutedText,
            modifier = modifier.size(14.dp)
        )
        isReadByOther -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Read",
            tint = extended.readTick,
            modifier = modifier.size(15.dp)
        )
        else -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Sent",
            tint = extended.mutedText,
            modifier = modifier.size(14.dp)
        )
    }
}

@Composable
fun OfflineAndMaintenanceStrip(
    isOnline: Boolean,
    isReadOnlyMode: Boolean,
    maintenanceBanner: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !isOnline || isReadOnlyMode || maintenanceBanner.isNotBlank()) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (!isOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiOff,
                            contentDescription = "Offline mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Offline mode • Cached chats & queued sending active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isReadOnlyMode || maintenanceBanner.isNotBlank()) {
                val text = maintenanceBanner.ifBlank {
                    "Alap is currently in read-only mode for maintenance."
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateBannerCard(
    announcement: Announcement,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onViewDetails() }
            .testTag("update_announcement_banner")
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = "App update",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (announcement.versionTag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = announcement.versionTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.testTag("view_update_details_button")
            ) {
                Text("View")
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_update_banner_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss update banner",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

object AlapDateFormatter {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun formatChatTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return ""
        val cal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }
        return if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            timeFormat.format(date)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
            ) {
                "Yesterday"
            } else {
                dateFormat.format(date)
            }
        }
    }

    fun formatMessageTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return timeFormat.format(date)
    }

    fun formatDayHeader(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Today"
        val cal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }
        if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Today"
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Yesterday"
        }
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    }
}
