package com.spiderlauncher.android.runtime

import java.io.File

object MinecraftProcess {

    private var process: Process? = null

    fun launch(
        command: List<String>,
        workingDir: File
    ): Boolean {

        return try {

            process =
                ProcessBuilder(command)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start()

            true

        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isRunning(): Boolean {
        return process?.isAlive == true
    }
}
