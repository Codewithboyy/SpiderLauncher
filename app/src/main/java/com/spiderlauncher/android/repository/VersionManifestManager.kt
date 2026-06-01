package com.spiderlauncher.android.repository

import org.json.JSONObject
import java.net.URL

data class MinecraftVersion(
    val id: String,
    val type: String,
    val url: String
)

object VersionManifestManager {

    private const val MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    fun fetchVersions(): List<MinecraftVersion> {

        val json =
            URL(MANIFEST_URL)
                .readText()

        val root =
            JSONObject(json)

        val versions =
            root.getJSONArray("versions")

        val result =
            mutableListOf<MinecraftVersion>()

        for (i in 0 until versions.length()) {

            val obj =
                versions.getJSONObject(i)

            result.add(
                MinecraftVersion(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    url = obj.getString("url")
                )
            )
        }

        return result
    }
}
