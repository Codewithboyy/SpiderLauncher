package com.spiderlauncher.android.repository

import android.content.Context
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.VersionDetail
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.model.VersionManifest
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
            ApiClient.gson.toJson(detail)
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
