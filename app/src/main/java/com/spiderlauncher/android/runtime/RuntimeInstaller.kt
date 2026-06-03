package com.spiderlauncher.android.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object RuntimeInstaller {

    private val client = OkHttpClient()

    private const val JAVA17_URL =
        "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre17-pojav.zip"
    private const val JAVA21_URL = 
        "https://github.com/MojoLauncher/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"

    suspend fun installJava17(
        context: Context,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {

        try {

            val runtimesDir =
                RuntimeManager.getRuntimeDir(context)

            val java17Dir =
                File(runtimesDir, "java17")

            if (
                File(java17Dir, "bin/java").exists()
            ) {
                onLog("Java 17 already installed")
                return@withContext true
            }

            java17Dir.mkdirs()

            val zipFile =
                File(runtimesDir, "jre17-pojav.zip")

            onLog("Downloading Java 17...")

            val request =
                Request.Builder()
                    .url(JAVA17_URL)
                    .build()

            val response =
                client.newCall(request)
                    .execute()

            if (!response.isSuccessful) {
                onLog("Download failed")
                return@withContext false
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
            }

            onLog("Extracting Java 17...")

            ZipInputStream(zipFile.inputStream()).use { zip ->

                var entry = zip.nextEntry

                while (entry != null) {

                    val outFile =
                        File(
                            java17Dir,
                            entry.name
                        )

                    if (entry.isDirectory) {

                        outFile.mkdirs()

                    } else {

                        outFile.parentFile?.mkdirs()

                        FileOutputStream(outFile).use {
                            zip.copyTo(it)
                        }

                        outFile.setExecutable(true)
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            zipFile.delete()

            onLog("Java 17 installed")

            true

        } catch (e: Exception) {

            onLog(
                e.message ?: "Unknown error"
            )

            false
        }
    }
}
