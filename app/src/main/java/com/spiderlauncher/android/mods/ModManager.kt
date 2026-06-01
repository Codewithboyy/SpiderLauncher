package com.spiderlauncher.android.mods

import java.io.File

object ModManager {

    fun getMods(
        modsDir: File
    ): List<File> {

        if (!modsDir.exists()) {
            modsDir.mkdirs()
        }

        return modsDir.listFiles()
            ?.filter {

                it.extension == "jar"

            } ?: emptyList()
    }

    fun removeMod(
        file: File
    ) {

        if (file.exists()) {
            file.delete()
        }
    }
}
