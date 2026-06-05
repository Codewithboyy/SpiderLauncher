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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

class LauncherRepository(private val context: Context) {

    private companion object {
        const val ASSET_PARALLEL_DOWNLOADS = 8
        const val LIBRARY_PARALLEL_DOWNLOADS = 4
        const val DOWNLOAD_PROGRESS_LOG_INTERVAL = 25
        const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    }

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
        
    suspend fun saveVersionJson(detail: VersionDetail) = withContext(Dispatchers.IO) {
        val versionDir = File(versionsDir, detail.id).also { it.mkdirs() }
        val jsonFile = File(versionDir, "${detail.id}.json")
        jsonFile.writeText(Gson().toJson(detail))
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

            response.use { res ->
                if (!res.isSuccessful) {
                    emit(DownloadState.Error("HTTP ${res.code}"))
                    return@flow
                }

                val body = res.body ?: run {
                    emit(DownloadState.Error("Empty response body"))
                    return@flow
                }
                val totalBytes = detail.downloads.client.size
                var downloaded = 0L
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)

                FileOutputStream(jarFile).use { out ->
                    body.byteStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            downloaded += read
                            val progress = if (totalBytes > 0) {
                                (downloaded * 100 / totalBytes).toInt()
                            } else {
                                0
                            }
                            emit(DownloadState.Downloading(progress, "Downloading client… $progress%"))
                        }
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
    
    suspend fun downloadLibraries(detail: VersionDetail, onProgress: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            val osName = "linux"
            val pendingLibraries = detail.libraries.mapNotNull { library ->
                if (!library.isCompatible(osName)) {
                    return@mapNotNull null
                }

                val artifact = library.downloads?.artifact ?: return@mapNotNull null
                val outputFile = File(librariesDir, artifact.path)

                if (outputFile.exists() && verifySha1(outputFile, artifact.sha1)) {
                    return@mapNotNull null
                }

                LibraryDownload(library.name, artifact.url, artifact.sha1, outputFile)
            }

            if (pendingLibraries.isEmpty()) {
                onProgress("Libraries already installed")
                return@withContext
            }

            onProgress("Downloading ${pendingLibraries.size} libraries…")
            downloadInBatches(
                items = pendingLibraries,
                batchSize = LIBRARY_PARALLEL_DOWNLOADS,
                onProgress = onProgress,
                progressLabel = "libraries"
            ) { item ->
                downloadFile(
                    url = item.url,
                    outputFile = item.outputFile,
                    expectedSha1 = item.sha1,
                    label = "library ${item.name}"
                )
            }
        }

    suspend fun downloadAssets(
        detail: VersionDetail,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {

        val indexRequest =
            Request.Builder()
                .url(detail.assetIndex.url)
                .build()

        val json = httpClient.newCall(indexRequest)
            .execute()
            .use { indexResponse ->
                if (!indexResponse.isSuccessful) {
                    throw IOException("Asset index download failed: HTTP ${indexResponse.code}")
                }

                indexResponse.body?.string()
                    ?: throw IOException("Asset index download failed: empty response body")
            }

        File(
            assetsIndexesDir,
            "${detail.assets}.json"
        ).writeText(json)

        val manifest =
            Gson().fromJson(
                json,
                AssetManifest::class.java
            )

        val pendingAssets = manifest.objects.mapNotNull { (name, asset) ->
            val hash = asset.hash
            val folder = hash.substring(0, 2)
            val objectFile = File(assetsObjectsDir, "$folder/$hash")

            if (objectFile.exists() && objectFile.length() == asset.size) {
                return@mapNotNull null
            }

            AssetDownload(
                name = name,
                url = "https://resources.download.minecraft.net/$folder/$hash",
                sha1 = hash,
                outputFile = objectFile
            )
        }

        if (pendingAssets.isEmpty()) {
            onProgress("Assets already installed")
            return@withContext
        }

        onProgress("Downloading ${pendingAssets.size} assets…")
        downloadInBatches(
            items = pendingAssets,
            batchSize = ASSET_PARALLEL_DOWNLOADS,
            onProgress = onProgress,
            progressLabel = "assets"
        ) { item ->
            downloadFile(
                url = item.url,
                outputFile = item.outputFile,
                expectedSha1 = item.sha1,
                label = "asset ${item.name}"
            )
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

    private suspend fun <T> downloadInBatches(
        items: List<T>,
        batchSize: Int,
        onProgress: (String) -> Unit,
        progressLabel: String,
        downloader: suspend (T) -> Unit
    ) = coroutineScope {
        val completed = AtomicInteger(0)
        val failures = mutableListOf<String>()

        items.chunked(batchSize).forEach { batch ->
            batch.map { item ->
                async(Dispatchers.IO) {
                    runCatching { downloader(item) }
                        .onFailure { error ->
                            synchronized(failures) {
                                failures += error.message ?: error::class.java.simpleName
                            }
                        }

                    val done = completed.incrementAndGet()
                    if (done == items.size || done % DOWNLOAD_PROGRESS_LOG_INTERVAL == 0) {
                        onProgress("Downloaded $done/${items.size} $progressLabel")
                    }
                }
            }.awaitAll()
        }

        if (failures.isNotEmpty()) {
            val preview = failures.take(3).joinToString("; ")
            throw IOException(
                "Failed to download ${failures.size}/${items.size} $progressLabel. $preview"
            )
        }
    }

    private fun downloadFile(
        url: String,
        outputFile: File,
        expectedSha1: String,
        label: String
    ) {
        outputFile.parentFile?.mkdirs()

        val tempFile = File(outputFile.parentFile, "${outputFile.name}.part")
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        response.use { res ->
            if (!res.isSuccessful) {
                throw IOException("Failed to download $label: HTTP ${res.code}")
            }

            val body = res.body ?: throw IOException("Failed to download $label: empty response body")
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, DOWNLOAD_BUFFER_SIZE)
                }
            }
        }

        if (!verifySha1(tempFile, expectedSha1)) {
            tempFile.delete()
            throw IOException("Failed to verify $label: SHA1 mismatch")
        }

        if (outputFile.exists() && !outputFile.delete()) {
            tempFile.delete()
            throw IOException("Failed to replace existing $label at ${outputFile.absolutePath}")
        }

        if (!tempFile.renameTo(outputFile)) {
            tempFile.delete()
            throw IOException("Failed to save $label to ${outputFile.absolutePath}")
        }
    }

    private data class LibraryDownload(
        val name: String,
        val url: String,
        val sha1: String,
        val outputFile: File
    )

    private data class AssetDownload(
        val name: String,
        val url: String,
        val sha1: String,
        val outputFile: File
    )

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
