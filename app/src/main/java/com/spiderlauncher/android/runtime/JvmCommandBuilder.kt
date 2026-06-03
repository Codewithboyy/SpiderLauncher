package com.spiderlauncher.android.runtime

object JvmCommandBuilder {

    fun build(
        javaPath: String,
        args: LaunchArguments,
        memoryMb: Int,
        nativesDir: String
    ): List<String> {

        return listOf(
            javaPath,

            "-Xms512M",
            "-Xmx${memoryMb}M",

            "-Djava.library.path=$nativesDir",

            "-cp",
            args.classpath,

            args.mainClass,

            "--username",
            args.username,

            "--version",
            args.version,

            "--assetsDir",
            args.assetsDir
        )
    }
}
