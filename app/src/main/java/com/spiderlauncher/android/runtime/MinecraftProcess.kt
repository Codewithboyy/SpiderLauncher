package com.spiderlauncher.android.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MinecraftProcess {

    private var process: Process? = null

    data class LaunchResult(
        val started: Boolean,
        val error: String? = null,
        val command: List<String> = emptyList(),
        val workingDir: String = ""
    )

    fun launch(command: List<String>, workingDir: File): LaunchResult {
        return try {
            process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            LaunchResult(
                started = true,
                command = command,
                workingDir = workingDir.absolutePath
            )
        } catch (e: Exception) {
            LaunchResult(
                started = false,
                error = buildString {
                    append(e::class.java.simpleName)
                    e.message?.let { message -> append(": ").append(message) }
                    e.cause?.let { cause ->
                        append("; caused by ")
                        append(cause::class.java.simpleName)
                        cause.message?.let { causeMessage -> append(": ").append(causeMessage) }
                    }
                },
                command = command,
                workingDir = workingDir.absolutePath
            )
        }
    }

    /** Stream stdout/stderr lines to the provided callback (suspending, runs on IO). */
    suspend fun streamOutput(onLine: (String) -> Unit): Int? = withContext(Dispatchers.IO) {
        val runningProcess = process ?: return@withContext null

        try {
            runningProcess.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> onLine(line) }
            }
            runningProcess.waitFor()
        } catch (e: Exception) {
            onLine(
                "✗ Console stream error: ${e::class.java.simpleName}" +
                    (e.message?.let { ": $it" } ?: "")
            )
            null
        }
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true
}
