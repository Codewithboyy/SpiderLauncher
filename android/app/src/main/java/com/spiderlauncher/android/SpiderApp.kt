package com.spiderlauncher.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager

class SpiderApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Minecraft version download progress"
            }

            val launchChannel = NotificationChannel(
                CHANNEL_LAUNCH,
                "Game Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Minecraft launch status"
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(listOf(downloadChannel, launchChannel))
        }
    }

    companion object {
        const val CHANNEL_DOWNLOAD = "spider_download"
        const val CHANNEL_LAUNCH   = "spider_launch"
    }
}
