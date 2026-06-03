package com.spiderlauncher.android.model

import com.google.gson.annotations.SerializedName

// ── Version Manifest ────────────────────────────────────────────────────────

data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionEntry>
)

data class LatestVersions(
    val release: String,
    val snapshot: String
)

data class VersionEntry(
    val id: String,
    val type: String,           // "release" | "snapshot" | "old_beta" | "old_alpha"
    val url: String,
    val time: String,
    val releaseTime: String
) {
    val isRelease   get() = type == "release"
    val isSnapshot  get() = type == "snapshot"
    val displayName get() = if (isRelease) "Release $id" else id
}

// ── Version Detail ──────────────────────────────────────────────────────────

data class VersionDetail(
    val id: String,
    val type: String,
    val mainClass: String,
    val minecraftArguments: String?,
    val arguments: Arguments?,
    val downloads: VersionDownloads,
    val assetIndex: AssetIndex,
    val assets: String,
    val libraries: List<Library>
)

data class Arguments(
    val game: List<Any>,
    val jvm: List<Any>
)

data class VersionDownloads(
    val client: Download,
    val server: Download?
)

data class Download(
    val sha1: String,
    val size: Long,
    val url: String
)

data class AssetIndex(
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long,
    val url: String
)

data class AssetManifest(
    val objects: Map<String, AssetObject>
)

data class AssetObject(
    val hash: String,
    val size: Long
)

data class Library(
    val name: String,
    val downloads: LibraryDownloads?,
    val rules: List<Rule>?,
    val natives: Map<String, String>?
) {
    fun isCompatible(os: String): Boolean {
        if (rules.isNullOrEmpty()) return true
        var allowed = false
        for (rule in rules) {
            val osMatch = rule.os == null || rule.os.name == os
            if (osMatch) allowed = rule.action == "allow"
        }
        return allowed
    }
}

data class LibraryDownloads(
    val artifact: LibraryArtifact?,
    val classifiers: Map<String, LibraryArtifact>?
)

data class LibraryArtifact(
    val path: String,
    val sha1: String,
    val size: Long,
    val url: String
)

data class Rule(
    val action: String,
    val os: OsRule?
)

data class OsRule(
    val name: String
)

// ── Local Profile ───────────────────────────────────────────────────────────

data class Profile(
    val id: String,
    val name: String,
    val username: String,
    val selectedVersion: String,
    val memoryMb: Int = 512,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Download State ──────────────────────────────────────────────────────────

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val label: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
    object Done : DownloadState()
}

// ── Launch State ────────────────────────────────────────────────────────────

sealed class LaunchState {
    object Idle : LaunchState()
    object Launching : LaunchState()
    data class Running(val pid: Int) : LaunchState()
    data class Error(val message: String) : LaunchState()
    object Exited : LaunchState()
}
