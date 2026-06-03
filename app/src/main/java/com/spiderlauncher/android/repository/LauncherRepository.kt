package com.spiderlauncher.android.repository

import android.content.Context
import com.google.gson.Gson
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.VersionDetail
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.model.VersionManifest
import com.spiderlauncher.android.model.AssetManifest
import com.spiderlauncher.android.runtime.LaunchArguments
import com.spiderlauncher.android.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class LauncherRepository(private val context: Context) {

    private val api = ApiClient.mojangApi
    private val httpClient = ApiClient.buildDownloadClient()

    // ── Minecraft Root ──────────────────────────────────────────────────────
    val minecraftDir: File
        get() = File(context.getExternalFilesDir(null), "minecraft").also { it.mkdirs() }

    val versionsDir: File
        get() = File(minecraftDir, "versions").also { it.mkdirs() }

    val librariesDir: File
        get() = File(minecraftDir, "libraries").also { it.mkdirs() }

    val assetsDir: File
        get() = File(minecraftDir, "assets").also { it.mkdirs() }
        
    val assetsIndexesDir: File
        get() = File(assetsDir, "indexes").also { it.mkdirs() }

    val assetsObjectsDir: File
        get() = File(assetsDir, "objects").also { it.mkdirs() }
    
    val nativesDir: File
        get() = File(
            minecraftDir,
            "natives"
        ).also { it.mkdirs() }

    // ── Version Manifest ────────────────────────────────────────────────────
    suspend fun fetchVersionManifest(): Result<VersionManifest> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVersionManifest()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch manifest: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Version Detail ──────────────────────────────────────────────────────
    suspend fun fetchVersionDetail(version: VersionEntry): Result<VersionDetail> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getVersionDetail(version.url)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch version detail"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
    suspend fun saveVersionJson(detail:         VersionDetail
        ) {

        val versionDir =
            File(versionsDir, detail.id)
                .also { it.mkdirs() }

        val jsonFile =
            File(versionDir, "${detail.id}.json")

        jsonFile.writeText(
            Gson().toJson(detail)
        )
    }

    // ── Download Client JAR ─────────────────────────────────────────────────
    fun downloadClientJar(detail: VersionDetail): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0, "Preparing download…"))

        val versionDir = File(versionsDir, detail.id).also { it.mkdirs() }
        val jarFile    = File(versionDir, "${detail.id}.jar")

        if (jarFile.exists() && verifySha1(jarFile, detail.downloads.client.sha1)) {
            emit(DownloadState.Done)
            return@flow
        }

        try {
            val request  = Request.Builder().url(detail.downloads.client.url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("HTTP ${response.code}"))
                return@flow
            }

            val body        = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }
            val totalBytes  = detail.downloads.client.size
            var downloaded  = 0L
            val buffer      = ByteArray(8192)

            FileOutputStream(jarFile).use { out ->
                body.byteStream().use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else 0
                        emit(DownloadState.Downloading(progress, "Downloading client… $progress%"))
                    }
                }
            }

            if (verifySha1(jarFile, detail.downloads.client.sha1)) {
                emit(DownloadState.Done)
            } else {
                jarFile.delete()
                emit(DownloadState.Error("SHA1 verification failed"))
            }

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun downloadLibraries(detail: VersionDetail,onProgress: (String) -> Unit) {
        val osName = "linux"

            detail.libraries.forEach { library ->

                if (!library.isCompatible(osName))
                    return@forEach

                val artifact =
                    library.downloads?.artifact
                        ?: return@forEach

                val outputFile =
                    File(librariesDir, artifact.path)

                if (
                    outputFile.exists() &&
                    verifySha1(outputFile, artifact.sha1)
                ) {
                    return@forEach
                }

                outputFile.parentFile?.mkdirs()

                onProgress("Library: ${library.name}")

                val request =
                    Request.Builder()
                        .url(artifact.url)
                        .build()

                val response =
                    httpClient.newCall(request)
                        .execute()

                if (!response.isSuccessful)
                    return@forEach

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (
                    !verifySha1(outputFile, artifact.sha1)
                ) {
                    outputFile.delete()
                }
            }
        }
        
    suspend fun downloadAssets(
    detail: VersionDetail,
    onProgress: (String) -> Unit
    ) {

        val indexRequest =
            Request.Builder()
                .url(detail.assetIndex.url)
                .build()

        val indexResponse =
            httpClient.newCall(indexRequest)
                .execute()

        if (!indexResponse.isSuccessful)
            return

        val json =
            indexResponse.body?.string()
                ?: return

        File(
            assetsIndexesDir,
            "${detail.assets}.json"
        ).writeText(json)

        val manifest =
            Gson().fromJson(
                json,
                AssetManifest::class.java
            )

        manifest.objects.forEach { (name, asset) ->

            val hash = asset.hash

            val folder = hash.substring(0, 2)

            val objectFile =
                File(
                    assetsObjectsDir,
                    "$folder/$hash"
                )

            if (objectFile.exists())
                return@forEach

            objectFile.parentFile?.mkdirs()

            val assetUrl =
                "https://resources.download.minecraft.net/$folder/$hash"

            onProgress("Asset: $name")

            val request =
                Request.Builder()
                    .url(assetUrl)
                    .build()

            val response =
                httpClient.newCall(request)
                    .execute()

            if (!response.isSuccessful)
                return@forEach

            response.body?.byteStream()?.use { input ->
                FileOutputStream(objectFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
    
    fun buildClasspath(detail: VersionDetail): String {

        val entries = mutableListOf<String>()

        val clientJar =
            File(
                versionsDir,
                "${detail.id}/${detail.id}.jar"
            )

        entries += clientJar.absolutePath

        detail.libraries.forEach { library ->

            val artifact =
                library.downloads?.artifact
                    ?: return@forEach

            val libFile =
                File(
                    librariesDir,
                    artifact.path
                )

            if (libFile.exists()) {
                entries += libFile.absolutePath
            }
        }

        return entries.joinToString(":")
    }
    
    fun extractNatives(
    detail: VersionDetail
    ) {

        detail.libraries.forEach { library ->

            val classifiers =
                library.downloads?.classifiers
                    ?: return@forEach

            val native =
                classifiers["natives-linux"]
                    ?: return@forEach

            val nativeFile =
                File(
                    librariesDir,
                    native.path
                )

            if (!nativeFile.exists())
                return@forEach

            java.util.zip.ZipFile(nativeFile)
                .use { zip ->

                    zip.entries().asSequence()
                        .forEach { entry ->

                            if (entry.isDirectory)
                                return@forEach

                            val output =
                                File(
                                    nativesDir,
                                    entry.name
                                )

                            output.parentFile?.mkdirs()

                            zip.getInputStream(entry)
                                .use { input ->

                                    output.outputStream()
                                        .use { out ->
                                            input.copyTo(out)
                                        }
                                }
                        }
                }
        }
    }
    
    fun buildLaunchInfo(
        detail: VersionDetail
    ): Map<String, String> {

        return mapOf(
            "mainClass" to detail.mainClass,
            "classpath" to buildClasspath(detail),
            "assetsDir" to assetsDir.absolutePath,
            "version" to detail.id
        )
    }
    
    fun buildLaunchArguments(
        detail: VersionDetail,
        username: String
    ): LaunchArguments {

        return LaunchArguments(
            mainClass = detail.mainClass,
            classpath = buildClasspath(detail),
            username = username,
            version = detail.id,
            assetsDir = assetsDir.absolutePath,
            gameDir = minecraftDir.absolutePath
        )
    }

    // ── Local Helpers ───────────────────────────────────────────────────────
    fun isVersionDownloaded(versionId: String): Boolean {
        val jarFile = File(versionsDir, "$versionId/$versionId.jar")
        return jarFile.exists() && jarFile.length() > 0
    }

    fun getDownloadedVersions(): List<String> {
        return versionsDir.listFiles()
            ?.filter { it.isDirectory && File(it, "${it.name}.jar").exists() }
            ?.map { it.name }
            ?: emptyList()
    }

    fun deleteVersion(versionId: String): Boolean {
        val versionDir = File(versionsDir, versionId)
        return versionDir.deleteRecursively()
    }

    private fun verifySha1(file: File, expectedSha1: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expectedSha1, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
