package com.spiderlauncher.android.runtime

import android.content.Context
import java.io.File

object RuntimeManager {

    fun getRuntimeDir(context: Context): File {
        return File(
            context.getExternalFilesDir(null),
            "runtimes"
        ).also { it.mkdirs() }
    }

    fun detectInstalledRuntimes(
        context: Context
    ): List<JavaRuntime> {

        val result = mutableListOf<JavaRuntime>()

        getRuntimeDir(context)
            .listFiles()
            ?.forEach { runtime ->

                val javaFile =
                    File(runtime, "bin/java")

                if (javaFile.exists()) {

                    val version =
                        runtime.name
                            .filter { it.isDigit() }
                            .toIntOrNull()
                            ?: 17

                    result += JavaRuntime(
                        runtime.name,
                        version,
                        javaFile
                    )
                }
            }

        return result
    }
}
