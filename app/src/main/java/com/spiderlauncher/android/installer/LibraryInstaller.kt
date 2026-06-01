package com.spiderlauncher.android.installer

import java.io.File
import java.net.URL

object LibraryInstaller {

    fun installLibrary(
        libraryUrl: String,
        output: File,
        onProgress: (String) -> Unit
    ) {

        output.parentFile?.mkdirs()

        if (output.exists()) {

            onProgress(
                "Library already exists: ${output.name}"
            )

            return
        }

        onProgress(
            "Downloading ${output.name}"
        )

        URL(libraryUrl)
            .openStream()
            .use { input ->

                output.outputStream()
                    .use { outputStream ->

                        input.copyTo(
                            outputStream
                        )
                    }
            }

        onProgress(
            "Installed ${output.name}"
        )
    }
}
