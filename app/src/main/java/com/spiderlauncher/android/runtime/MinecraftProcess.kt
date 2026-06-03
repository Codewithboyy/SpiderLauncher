package com.spiderlauncher.android.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MinecraftProcess {

    private var process: Process? = null

    fun launch(command: List<String>, workingDir: File): Boolean {
        return try {
            process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Stream stdout lines to the provided callback (suspending, runs on IO). */
    suspend fun streamOutput(onLine: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            process?.inputStream?.bufferedReader()?.forEachLine { line ->
                onLine(line)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true
}
