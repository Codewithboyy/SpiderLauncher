package com.spiderlauncher.android.installer

import java.io.File
import java.net.URL

object AssetInstaller {

    fun installAsset(
        assetUrl: String,
        output: File
    ) {

        output.parentFile?.mkdirs()

        if (output.exists()) {
            return
        }

        URL(assetUrl)
            .openStream()
            .use { input ->

                output.outputStream()
                    .use { outputStream ->

                        input.copyTo(
                            outputStream
                        )
                    }
            }
    }
}
