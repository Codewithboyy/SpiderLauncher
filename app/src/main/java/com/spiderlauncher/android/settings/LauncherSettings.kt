package com.spiderlauncher.android.settings

data class LauncherSettings(

    val ramMB: Int = 2048,

    val javaArgs: String = "",

    val fullscreen: Boolean = true,

    val autoInstallAssets: Boolean = true
)
