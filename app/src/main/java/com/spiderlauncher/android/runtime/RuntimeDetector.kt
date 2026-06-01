package com.spiderlauncher.android.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class DeviceInfo(
    val androidVersion: String,
    val cpuArch: String,
    val totalRamMB: Long,
    val cpuCores: Int
)

object RuntimeDetector {

    fun detect(context: Context): DeviceInfo {

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager

        val memoryInfo =
            ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(memoryInfo)

        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE,
            cpuArch = Build.SUPPORTED_ABIS.firstOrNull()
                ?: "unknown",
            totalRamMB = memoryInfo.totalMem / 1024 / 1024,
            cpuCores = Runtime.getRuntime()
                .availableProcessors()
        )
    }

    fun recommendedRam(
        context: Context
    ): Int {

        val ram =
            detect(context).totalRamMB

        return when {

            ram >= 12000 -> 6144

            ram >= 8000 -> 4096

            ram >= 6000 -> 3072

            ram >= 4000 -> 2048

            else -> 1024
        }
    }
}
