package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.ApkUpdateManager
import com.example.util.NotificationHelper

@Composable
fun PermissionsOnboardingScreen(
    onContinue: (notificationsGranted: Boolean, overlayGranted: Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotification by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var hasContacts by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasInstallPackages by remember {
        mutableStateOf(ApkUpdateManager.canRequestPackageInstalls(context))
    }
    var hasOverlay by remember {
        mutableStateOf(NotificationHelper.hasOverlayPermission(context))
    }
    var hasUnrestrictedBattery by remember {
        mutableStateOf(NotificationHelper.isIgnoringBatteryOptimizations(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotification = NotificationHelper.hasNotificationPermission(context)
                hasContacts = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                hasInstallPackages = ApkUpdateManager.canRequestPackageInstalls(context)
                hasOverlay = NotificationHelper.hasOverlayPermission(context)
                hasUnrestrictedBattery = NotificationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS] ?: NotificationHelper.hasNotificationPermission(context)
        } else {
            true
        }
        hasContacts = results[Manifest.permission.READ_CONTACTS] ?: (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
            )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "App Permissions Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "সহজে মেসেজ, কন্টাক্ট ও অটো-আপডেট পেতে নিচের পারমিশনগুলো চালু করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            PermissionSetupCard(
                icon = Icons.Filled.Notifications,
                title = "Push Notifications",
                subtitle = "অ্যাপ বন্ধ থাকলেও নতুন মেসেজ ও অ্যাডমিন আপডেটের নোটিফিকেশন পান।",
                isGranted = hasNotification,
                buttonLabel = "Allow Notifications",
                onAction = {
                    val toRequest = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (!hasContacts) {
                            add(Manifest.permission.READ_CONTACTS)
                        }
                    }
                    if (toRequest.isNotEmpty()) {
                        multiPermissionLauncher.launch(toRequest.toTypedArray())
                    }
                }
            )

            PermissionSetupCard(
                icon = Icons.Filled.Contacts,
                title = "Phonebook Contacts",
                subtitle = "আপনার ফোনের সেভ করা পরিচিত মানুষদের সাথে সরাসরি চ্যাট শুরু করুন।",
                isGranted = hasContacts,
                buttonLabel = "Allow Contacts",
                onAction = {
                    multiPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                }
            )

            PermissionSetupCard(
                icon = Icons.Filled.SystemUpdate,
                title = "Direct App Updates",
                subtitle = "নতুন ভার্সন এলে ব্রাউজার ছাড়াই সরাসরি ১-ক্লিকে অ্যাপ আপডেট ইনস্টল করুন।",
                isGranted = hasInstallPackages,
                buttonLabel = "Allow App Install",
                onAction = { ApkUpdateManager.openInstallPermissionSettings(context) }
            )

            PermissionSetupCard(
                icon = Icons.Filled.NotificationsActive,
                title = "Lock Screen & Heads-Up Banners",
                subtitle = "হোয়াটসঅ্যাপের মতো লক স্ক্রিনে ও স্ক্রিনের উপরে ব্যানার পেতে 'Pop on screen' ও 'Lock screen' চালু রাখুন।",
                isGranted = false,
                buttonLabel = "Check Lock Screen & Banner Settings",
                onAction = { NotificationHelper.openMessageChannelSettings(context) }
            )

            PermissionSetupCard(
                icon = Icons.Filled.BatteryChargingFull,
                title = "Instant Background Alerts (Battery)",
                subtitle = "অ্যাপ পুরোপুরি বন্ধ থাকলেও সাথে সাথে মেসেজ রিং ও ব্যানার পেতে ব্যাটারি সেভার 'Unrestricted' করুন।",
                isGranted = hasUnrestrictedBattery,
                buttonLabel = "Allow Unrestricted Battery",
                onAction = { NotificationHelper.openBatteryOptimizationSettings(context) }
            )

            PermissionSetupCard(
                icon = Icons.Filled.Layers,
                title = "Floating Glass Popups (Optional)",
                subtitle = "গেম বা অন্য অ্যাপের উপর আইফোন-স্টাইল গ্লাস পপআপ দেখতে চাইলে অন করুন।",
                isGranted = hasOverlay,
                buttonLabel = "Enable Glass Overlay",
                onAction = { NotificationHelper.openOverlayPermissionSettings(context) }
            )

            if (!hasNotification || !hasContacts) {
                OutlinedButton(
                    onClick = {
                        val perms = buildList {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (!hasContacts) add(Manifest.permission.READ_CONTACTS)
                        }
                        if (perms.isNotEmpty()) {
                            multiPermissionLauncher.launch(perms.toTypedArray())
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_runtime_permissions_button")
                ) {
                    Text("একসাথে Notifications ও Contacts অনুমতি দিন", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onContinue(hasNotification, hasOverlay) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("permissions_continue_button")
            ) {
                Text("চালিয়ে যান (Continue to Alap)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isGranted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (!isGranted) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
