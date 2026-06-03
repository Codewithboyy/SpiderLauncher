package com.spiderlauncher.android.game

object GameSession {

    var currentConfig: GameConfig? = null

    fun start(config: GameConfig) {
        currentConfig = config
    }

    fun stop() {
        currentConfig = null
    }

    fun isRunning(): Boolean {
        return currentConfig != null
    }
}
