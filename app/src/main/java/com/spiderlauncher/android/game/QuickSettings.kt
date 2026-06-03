package com.spiderlauncher.android.game

data class QuickSettings(
    val resolutionScale: Int = 100,
    val mouseSpeed: Int = 100,
    val mouseTriggerMs: Int = 50,
    val buttonScale: Int = 100,
    val buttonOpacity: Int = 100,
    val showFps: Boolean = true,
    val showRam: Boolean = true
)
