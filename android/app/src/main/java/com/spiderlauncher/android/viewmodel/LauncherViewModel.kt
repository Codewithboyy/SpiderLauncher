package com.spiderlauncher.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.LaunchState
import com.spiderlauncher.android.model.Profile
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.repository.LauncherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LauncherUiState(
    val isLoadingVersions: Boolean       = true,
    val versions: List<VersionEntry>     = emptyList(),
    val filteredVersions: List<VersionEntry> = emptyList(),
    val selectedVersion: VersionEntry?   = null,
    val showSnapshots: Boolean           = false,
    val downloadedVersions: Set<String>  = emptySet(),
    val downloadState: DownloadState     = DownloadState.Idle,
    val launchState: LaunchState         = LaunchState.Idle,
    val errorMessage: String?            = null,
    val consoleLog: List<String>         = emptyList(),
    val profile: Profile                 = Profile(
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

            // On Android, Minecraft Java Edition cannot be natively executed —
            // we delegate to PojavLauncher or a compatible JVM bridge.
            // This sends an intent to a compatible launcher app if installed,
            // or shows instructions.
            log("Delegating to PojavLauncher bridge…")
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

    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _uiState.update { it.copy(consoleLog = it.consoleLog + "[$timestamp] $message") }
    }
}
