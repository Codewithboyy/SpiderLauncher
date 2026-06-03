package com.spiderlauncher.android.runtime

object JvmCommandBuilder {

    fun build(
        javaPath: String,
        args: LaunchArguments,
        memoryMb: Int,
        nativesDir: String,
        extraArgs: String = ""
    ): List<String> {
        val cmd = mutableListOf(
            javaPath,
            "-Xms${minOf(256, memoryMb)}M",
            "-Xmx${memoryMb}M",
            "-Djava.library.path=$nativesDir",
            "-Dfml.ignoreInvalidMinecraftCertificates=true",
            "-Dfml.ignorePatchDiscrepancies=true"
        )

        // Extra JVM args from settings (e.g. -XX:+UseG1GC)
        if (extraArgs.isNotBlank()) {
            extraArgs.trim().split("\\s+".toRegex()).forEach { cmd += it }
        }

        cmd += listOf(
            "-cp", args.classpath,
            args.mainClass,
            "--username", args.username,
            "--version", args.version,
            "--assetsDir", args.assetsDir,
            "--accessToken", "0",
            "--userType", "offline",
            "--gameDir", args.gameDir
        )

        return cmd
    }
}
