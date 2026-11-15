package com.example

import android.app.Application
import com.example.service.AlapBackgroundSyncWorker
import com.example.util.NotificationHelper

class AlapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        AlapBackgroundSyncWorker.schedule(this)
    }
}
