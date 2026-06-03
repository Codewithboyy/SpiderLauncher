package com.spiderlauncher.android.game

data class GameConfig(
    val version: String,
    val username: String,
    val memoryMb: Int,
    val width: Int,
    val height: Int,
    val fullscreen: Boolean
)
