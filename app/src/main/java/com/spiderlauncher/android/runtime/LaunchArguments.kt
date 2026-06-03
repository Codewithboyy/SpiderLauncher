package com.spiderlauncher.android.runtime

data class LaunchArguments(
    val mainClass: String,
    val classpath: String,
    val username: String,
    val version: String,
    val assetsDir: String,
    val gameDir: String = ""
)
