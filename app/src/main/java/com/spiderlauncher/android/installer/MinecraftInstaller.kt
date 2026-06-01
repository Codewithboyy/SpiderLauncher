package com.spiderlauncher.android.installer

import android.content.Context
import java.io.File
import java.net.URL

object MinecraftInstaller {

    suspend fun installVersion(
        context: Context,
        versionId: String,
        clientUrl: String,
        onProgress: (String) -> Unit
    ) {

        val minecraftDir =
            File(
                context.filesDir,
                "minecraft"
            )

        val versionsDir =
            File(
                minecraftDir,
                "versions"
            )

        val versionDir =
            File(
                versionsDir,
                versionId
            )

        versionDir.mkdirs()

        val clientJar =
            File(
                versionDir,
                "$versionId.jar"
            )

        onProgress(
            "Downloading client..."
        )

        URL(clientUrl)
            .openStream()
            .use { input ->

                clientJar.outputStream()
                    .use { output ->

                        input.copyTo(output)
                    }
            }

        onProgress(
            "Installation complete"
        )
    }
}