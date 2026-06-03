package com.spiderlauncher.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiderlauncher.android.data.PreferencesRepository
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.LaunchState
import com.spiderlauncher.android.model.Profile
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.repository.LauncherRepository
import com.spiderlauncher.android.runtime.RuntimeInstaller
import com.spiderlauncher.android.runtime.RuntimeManager
import com.spiderlauncher.android.runtime.JvmCommandBuilder
import com.spiderlauncher.android.runtime.MinecraftProcess
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LauncherUiState(
    val isLoadingVersions: Boolean           = true,
    val versions: List<VersionEntry>         = emptyList(),
    val filteredVersions: List<VersionEntry> = emptyList(),
    val selectedVersion: VersionEntry?       = null,
    val showSnapshots: Boolean               = false,
    val downloadedVersions: Set<String>      = emptySet(),

    val downloadState: DownloadState = DownloadState.Idle,
    val launchState: LaunchState     = LaunchState.Idle,

    val errorMessage: String?        = null,
    val consoleLog: List<String>     = emptyList(),

    // In-game overlay panel flags
    val overlayVisible: Boolean      = false,
    val showLogOutput: Boolean       = false,
    val showKeycodes: Boolean        = false,
    val showQuickSettings: Boolean   = false,
    val showCustomControls: Boolean  = false,

    // Home-screen console drawer (slides up from bottom)
    val showConsoleOverlay: Boolean  = false,

    val javaArgs: String             = "",
    val fullscreen: Boolean          = true,
    val autoInstallAssets: Boolean   = true,

    val profile: Profile = Profile(
        id = "default", name = "Default Profile",
        username = "Player", selectedVersion = "", memoryMb = 512
    )
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo  = LauncherRepository(application)
    private val prefs = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore all saved prefs before loading versions
            val saved = prefs.userPreferences.first()
            _uiState.update {
                it.copy(
                    showSnapshots      = saved.showSnapshots,
                    javaArgs           = saved.javaArgs,
                    fullscreen         = saved.fullscreen,
                    autoInstallAssets  = saved.autoInstallAssets,
                    profile = it.profile.copy(
                        username = saved.username,
                        memoryMb = saved.memoryMb
                    )
                )
            }
            refreshDownloaded()
            loadVersionManifest(restoreVersionId = saved.selectedVersionId)
        }

        // Install Java 17 runtime in background
        viewModelScope.launch {
            RuntimeInstaller.installJava17(getApplication()) { log(it) }
        }
    }

    // ── Version loading ──────────────────────────────────────────────────────

    fun loadVersionManifest(restoreVersionId: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVersions = true, errorMessage = null) }
            val result = repo.fetchVersionManifest()
            result.fold(
                onSuccess = { manifest ->
                    _uiState.update { state ->
                        val versions = manifest.versions
                        val filtered = versions.filter { it.isRelease || state.showSnapshots }
                        // Prefer the saved version, fallback to latest release
                        val selected = versions.firstOrNull { it.id == restoreVersionId }
                            ?: versions.firstOrNull { it.id == manifest.latest.release }
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
                    _uiState.update {
                        it.copy(isLoadingVersions = false, errorMessage = error.message)
                    }
                    log("Error loading versions: ${error.message}")
                }
            )
        }
    }

    fun selectVersion(version: VersionEntry) {
        _uiState.update { it.copy(selectedVersion = version) }
        viewModelScope.launch { prefs.saveSelectedVersion(version.id) }
    }

    fun toggleSnapshots(show: Boolean) {
        _uiState.update { state ->
            val filtered = state.versions.filter { it.isRelease || show }
            state.copy(showSnapshots = show, filteredVersions = filtered)
        }
        viewModelScope.launch { prefs.saveShowSnapshots(show) }
    }

    // ── Profile / settings ────────────────────────────────────────────────────

    fun updateUsername(name: String) {
        _uiState.update { it.copy(profile = it.profile.copy(username = name)) }
        viewModelScope.launch { prefs.saveUsername(name) }
    }

    fun updateMemory(mb: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(memoryMb = mb)) }
        viewModelScope.launch { prefs.saveMemoryMb(mb) }
    }

    fun updateJavaArgs(args: String) {
        _uiState.update { it.copy(javaArgs = args) }
        viewModelScope.launch { prefs.saveJavaArgs(args) }
    }

    fun updateFullscreen(value: Boolean) {
        _uiState.update { it.copy(fullscreen = value) }
        viewModelScope.launch { prefs.saveFullscreen(value) }
    }

    fun updateAutoInstallAssets(value: Boolean) {
        _uiState.update { it.copy(autoInstallAssets = value) }
        viewModelScope.launch { prefs.saveAutoInstallAssets(value) }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    fun downloadSelectedVersion() {
        val version = _uiState.value.selectedVersion ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(downloadState = DownloadState.Downloading(0, "Fetching version info…"))
            }
            log("Fetching details for ${version.id}…")

            val detailResult = repo.fetchVersionDetail(version)
            detailResult.fold(
                onSuccess = { detail ->
                    log("Starting download for ${detail.id} (${detail.downloads.client.size / 1024 / 1024} MB)…")
                    repo.saveVersionJson(detail)

                    if (_uiState.value.autoInstallAssets) {
                        log("Downloading libraries…")
                        repo.downloadLibraries(detail) { log(it) }
                        log("Downloading assets…")
                        repo.downloadAssets(detail) { log(it) }
                    }

                    repo.downloadClientJar(detail).collect { state ->
                        _uiState.update { it.copy(downloadState = state) }
                        when (state) {
                            is DownloadState.Downloading -> { /* progress shown in UI */ }
                            is DownloadState.Done  -> {
                                log("✓ Download complete for ${detail.id}")
                                refreshDownloaded()
                            }
                            is DownloadState.Error -> log("✗ ${state.message}")
                            else -> {}
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(downloadState = DownloadState.Error(error.message ?: "Unknown"))
                    }
                    log("✗ Failed to fetch version detail: ${error.message}")
                }
            )
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            if (repo.deleteVersion(versionId)) {
                log("Deleted version $versionId")
                refreshDownloaded()
            }
        }
    }

    // ── Launch ────────────────────────────────────────────────────────────────

    fun launchGame() {
        val version  = _uiState.value.selectedVersion ?: run {
            _uiState.update { it.copy(errorMessage = "No version selected") }; return
        }
        val username = _uiState.value.profile.username.ifBlank { "Player" }

        if (!repo.isVersionDownloaded(version.id)) {
            _uiState.update { it.copy(errorMessage = "Version ${version.id} is not downloaded yet") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(launchState = LaunchState.Launching, showConsoleOverlay = true) }

            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            log("  SpiderLauncher  •  MC ${version.id}")
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            log("User    : $username")
            log("Memory  : ${_uiState.value.profile.memoryMb} MB")

            val runtimes = RuntimeManager.detectInstalledRuntimes(getApplication())
            if (runtimes.isEmpty()) {
                log("✗ No Java runtime found — installing…")
                val installed = RuntimeInstaller.installJava17(getApplication()) { log(it) }
                if (!installed) {
                    _uiState.update { it.copy(launchState = LaunchState.Error("Java 17 install failed")) }
                    return@launch
                }
            }

            val runtime = RuntimeManager.detectInstalledRuntimes(getApplication())
                .firstOrNull() ?: run {
                _uiState.update { it.copy(launchState = LaunchState.Error("No Java runtime available")) }
                log("✗ No runtime available after install")
                return@launch
            }

            log("Runtime : ${runtime.name} (Java ${runtime.version})")

            val detailResult = repo.fetchVersionDetail(version)
            detailResult.onSuccess { detail ->
                repo.extractNatives(detail)
                log("Natives extracted")

                val info = repo.buildLaunchInfo(detail)
                log("MainClass: ${info["mainClass"]}")

                val args    = repo.buildLaunchArguments(detail, username)
                val command = JvmCommandBuilder.build(
                    javaPath   = runtime.javaBinary.absolutePath,
                    args       = args,
                    memoryMb   = _uiState.value.profile.memoryMb,
                    nativesDir = repo.nativesDir.absolutePath,
                    extraArgs  = _uiState.value.javaArgs
                )

                log("Launching JVM…")
                val launched = MinecraftProcess.launch(command, repo.minecraftDir)

                if (launched) {
                    _uiState.update { it.copy(launchState = LaunchState.Running(1)) }
                    log("✓ Minecraft process started")
                    prefs.saveLastPlayed(version.id)

                    // Stream stdout/stderr lines to the console log
                    MinecraftProcess.streamOutput { line -> log(line) }
                } else {
                    _uiState.update { it.copy(launchState = LaunchState.Error("Process failed to start")) }
                    log("✗ Launch failed")
                }
            }
            detailResult.onFailure { err ->
                _uiState.update { it.copy(launchState = LaunchState.Error(err.message ?: "Detail fetch failed")) }
                log("✗ ${err.message}")
            }
        }
    }

    fun stopGame() {
        MinecraftProcess.stop()
        _uiState.update { it.copy(launchState = LaunchState.Exited) }
        log("Game stopped.")
    }

    // ── Console overlay ───────────────────────────────────────────────────────

    fun toggleConsoleOverlay() =
        _uiState.update { it.copy(showConsoleOverlay = !it.showConsoleOverlay) }

    fun hideConsoleOverlay() =
        _uiState.update { it.copy(showConsoleOverlay = false) }

    fun clearLog() =
        _uiState.update { it.copy(consoleLog = emptyList()) }

    // ── In-game overlay panel ─────────────────────────────────────────────────

    fun toggleOverlay() =
        _uiState.update { it.copy(overlayVisible = !it.overlayVisible) }

    fun showLogOutput() =
        _uiState.update { it.copy(showLogOutput = true, showKeycodes = false,
            showQuickSettings = false, showCustomControls = false) }

    fun showKeycodes() =
        _uiState.update { it.copy(showLogOutput = false, showKeycodes = true,
            showQuickSettings = false, showCustomControls = false) }

    fun showQuickSettings() =
        _uiState.update { it.copy(showLogOutput = false, showKeycodes = false,
            showQuickSettings = true, showCustomControls = false) }

    fun showCustomControls() =
        _uiState.update { it.copy(showLogOutput = false, showKeycodes = false,
            showQuickSettings = false, showCustomControls = true) }

    fun forceCloseGame() { stopGame(); log("Force-closed game") }

    fun sendKeycode(code: Int) { log("Keycode sent: $code") }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun refreshDownloaded() {
        _uiState.update { it.copy(downloadedVersions = repo.getDownloadedVersions().toSet()) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearDownloadState() = _uiState.update { it.copy(downloadState = DownloadState.Idle) }

    fun log(message: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _uiState.update { it.copy(consoleLog = it.consoleLog + "[$ts] $message") }
    }
}
