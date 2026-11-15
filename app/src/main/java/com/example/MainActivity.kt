package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.navigation.AlapRoot
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private var launchChatId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchChatId = intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
        // Non-immersive clean light system bars (3-button navigation bar stays visible)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE)
        )
        setContent {
            AlapRoot(
                initialOpenChatId = launchChatId,
                onConsumeInitialOpenChatId = { launchChatId = null },
                onScreenSecurityChanged = { secured ->
                    if (secured) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            launchChatId = chatId
        }
    }
}
