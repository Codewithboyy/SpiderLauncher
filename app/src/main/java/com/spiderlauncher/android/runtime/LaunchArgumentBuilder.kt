package com.spiderlauncher.android.runtime

import com.spiderlauncher.android.profile.LaunchProfile

object LaunchArgumentBuilder {

    fun build(
        profile: LaunchProfile
    ): List<String> {

        return listOf(

            "-Xms512M",

            "-Xmx${profile.ramMB}M",

            profile.javaArgs
        )
    }
}
