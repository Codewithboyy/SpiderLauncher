package com.spiderlauncher.android.model

data class OverlayState(
    val visible: Boolean = false,
    val showLogs: Boolean = false,
    val showControls: Boolean = false,
    val showKeycodes: Boolean = false,
    val showQuickSettings: Boolean = false
)
