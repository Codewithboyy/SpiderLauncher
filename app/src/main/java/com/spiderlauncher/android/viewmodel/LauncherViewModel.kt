package com.spiderlauncher.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.LaunchState
import com.spiderlauncher.android.model.Profile
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.repository.LauncherRepository
import com.spiderlauncher.android.runtime.RuntimeInstaller
import com.spiderlauncher.android.runtime.RuntimeManager
import com.spiderlauncher.android.runtime.JvmCommandBuilder
import com.spiderlauncher.android.runtime.MinecraftProcess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

    
data class QuickSettings(
    val resolutionScale: Int = 100,
    val mouseSpeed: Int = 100,
    val mouseTriggerMs: Int = 50,
    val controlOpacity: Int = 100,
    val buttonScale: Int = 100,
    val showFps: Boolean = true,
    val showRam: Boolean = true,
    val showRenderer: Boolean = false
)

data class LauncherUiState(
    val isLoadingVersions: Boolean = true,
    val versions: List<VersionEntry> = emptyList(),
    val filteredVersions: List<VersionEntry> = emptyList(),
    val selectedVersion: VersionEntry? = null,
    val showSnapshots: Boolean = false,
    val downloadedVersions: Set<String> = emptySet(),

    val downloadState: DownloadState = DownloadState.Idle,
    val launchState: LaunchState = LaunchState.Idle,

    val errorMessage: String? = null,
    val consoleLog: List<String> = emptyList(),

    val overlayVisible: Boolean = false,
    val showLogOutput: Boolean = false,
    val showKeycodes: Boolean = false,
    val showQuickSettings: Boolean = false,
    val showCustomControls: Boolean = false,

    val fpsCounterEnabled: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    val performanceModeEnabled: Boolean = true,

    val profile: Profile = Profile(
        id = "default",
        name = "Default Profile",
        username = "Player",
        selectedVersion = "",
        memoryMb = 512
    )
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LauncherRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        loadVersionManifest()
        refreshDownloaded()
        viewModelScope.launch {

            RuntimeInstaller.installJava17(
                getApplication()
            ) {
                log(it)
            }
        }
    }

    // ── Version Loading ─────────────────────────────────────────────────────

    fun loadVersionManifest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVersions = true, errorMessage = null) }

            val result = repo.fetchVersionManifest()
            result.fold(
                onSuccess = { manifest ->
                    _uiState.update { state ->
                        val versions  = manifest.versions
                        val filtered  = versions.filter { it.isRelease || state.showSnapshots }
                        val latestId  = manifest.latest.release
                        val selected  = versions.firstOrNull { it.id == latestId }
                        state.copy(
                            isLoadingVersions = false,
                            versions          = versions,
                            filteredVersions  = filtered,
                            selectedVersion   = selected
                        )
                    }
                    log("Loaded ${manifest.versions.size} versions")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingVersions = false, errorMessage = error.message) }
                    log("Error: ${error.message}")
                }
            )
        }
    }

    fun selectVersion(version: VersionEntry) {
        _uiState.update { it.copy(selectedVersion = version) }
    }

    fun toggleSnapshots(show: Boolean) {
        _uiState.update { state ->
            val filtered = state.versions.filter { it.isRelease || show }
            state.copy(showSnapshots = show, filteredVersions = filtered)
        }
    }

    // ── Profile ─────────────────────────────────────────────────────────────

    fun updateUsername(name: String) {
        _uiState.update { it.copy(profile = it.profile.copy(username = name)) }
    }

    fun updateMemory(mb: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(memoryMb = mb)) }
    }

    // ── Download ─────────────────────────────────────────────────────────────

    fun downloadSelectedVersion() {
        val version = _uiState.value.selectedVersion ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadState = DownloadState.Downloading(0, "Fetching version info…")) }
            log("Fetching details for ${version.id}…")

            val detailResult = repo.fetchVersionDetail(version)
            detailResult.fold(
                onSuccess = { detail ->
                    log("Starting download for ${detail.id}…")
                    repo.saveVersionJson(detail)
                    log("Downloading libraries...")
                    repo.downloadLibraries(detail) {
                        log(it)
                    }
                    log("Downloading assets...")
                    repo.downloadAssets(detail) {
                        log(it)
                    }
                    repo.downloadClientJar(detail).collect { state ->
                        _uiState.update { it.copy(downloadState = state) }
                        when (state) {
                            is DownloadState.Downloading -> log(state.label)
                            is DownloadState.Done        -> {
                                log("Download complete!")
                                refreshDownloaded()
                            }
                            is DownloadState.Error       -> log("Error: ${state.message}")
                            else -> {}
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(downloadState = DownloadState.Error(error.message ?: "Unknown")) }
                    log("Failed to fetch version detail: ${error.message}")
                }
            )
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            val deleted = repo.deleteVersion(versionId)
            if (deleted) {
                log("Deleted version $versionId")
                refreshDownloaded()
            }
        }
    }

    // ── Launch ──────────────────────────────────────────────────────────────

    fun launchGame() {
        val version  = _uiState.value.selectedVersion ?: return
        val username = _uiState.value.profile.username.ifBlank { "Player" }

        if (!repo.isVersionDownloaded(version.id)) {
            _uiState.update { it.copy(errorMessage = "Version not downloaded. Please download it first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(launchState = LaunchState.Launching) }
            log("Launching Minecraft ${version.id} as $username…")
            
            val runtimes =
                RuntimeManager.detectInstalledRuntimes(
                    getApplication()
                )

            if (runtimes.isEmpty()) {

                log("No Java runtime installed")

                _uiState.update {
                    it.copy(
                        launchState =
                            LaunchState.Error(
                                "No Java Runtime"
                            )
                    )
                }

                return@launch
            }
            
            val detailResult =
                repo.fetchVersionDetail(version)

            detailResult.onSuccess { detail ->

                repo.extractNatives(detail)

                log("Natives extracted")

                val info =
                    repo.buildLaunchInfo(detail)

                log("MainClass = ${info["mainClass"]}")
                log("Classpath entries built")
                log("Assets = ${info["assetsDir"]}")
            }
            
            val runtime = runtimes.first()

                detailResult.onSuccess { detail ->

                    repo.extractNatives(detail)

                    log("Natives extracted")

                    val info =
                        repo.buildLaunchInfo(detail)

                    log("MainClass = ${info["mainClass"]}")
                    log("Classpath entries built")
                    log("Assets = ${info["assetsDir"]}")

                    val args =
                        repo.buildLaunchArguments(
                            detail,
                            username
                        )

                    val command =
                        JvmCommandBuilder.build(
                            javaPath = runtime.javaBinary.absolutePath,
                            args = args,
                            memoryMb = _uiState.value.profile.memoryMb,
                            nativesDir = repo.nativesDir.absolutePath
                        )

                        log("Launching JVM...")

                    val launched =
                        MinecraftProcess.launch(
                            command,
                            repo.minecraftDir
                        )

                    if (launched) {

                        _uiState.update {
                            it.copy(
                                launchState =
                                    LaunchState.Running(1)
                            )
                        }

                        log("Minecraft process started")

                    } else {

                        _uiState.update {
                            it.copy(
                                launchState =
                                    LaunchState.Error(
                                        "Launch failed"
                                    )
                            )
                        }
                        
                        log("Launch failed")
                    }
            
            _uiState.update { it.copy(launchState = LaunchState.Running(0)) }
            log("Game launched!")
        }
    }
        

    fun stopGame() {
        _uiState.update { it.copy(launchState = LaunchState.Exited) }
        log("Game stopped.")
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun refreshDownloaded() {
        val downloaded = repo.getDownloadedVersions().toSet()
        _uiState.update { it.copy(downloadedVersions = downloaded) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearDownloadState() {
        _uiState.update { it.copy(downloadState = DownloadState.Idle) }
    }
    
    fun toggleOverlay() {
        _uiState.update {
            it.copy(
                overlayVisible = !it.overlayVisible
            )
        }
    }

    fun showLogOutput() {
        _uiState.update {
            it.copy(
                showLogOutput = true,
                showKeycodes = false,
                showQuickSettings = false,
                showCustomControls = false
            )
        }
    }

    fun showKeycodes() {
        _uiState.update {
            it.copy(
                showLogOutput = false,
                showKeycodes = true,
                showQuickSettings = false,
                showCustomControls = false
            )
        }
    }

    fun showQuickSettings() {
        _uiState.update {
            it.copy(
                showLogOutput = false,
                showKeycodes = false,
                showQuickSettings = true,
                showCustomControls = false
            )
        }
    }

    fun showCustomControls() {
        _uiState.update {
            it.copy(
                showLogOutput = false,
                showKeycodes = false,
                showQuickSettings = false,
                showCustomControls = true
            )
        }
    }

    fun forceCloseGame() {
        stopGame()
        log("Force close requested")
    }

    fun sendKeycode(code: Int) {
        log("Sent keycode: $code")
    }

    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _uiState.update { it.copy(consoleLog = it.consoleLog + "[$timestamp] $message") }
    }
}
