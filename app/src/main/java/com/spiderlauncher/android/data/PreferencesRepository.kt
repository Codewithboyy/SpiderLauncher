package com.spiderlauncher.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "spider_prefs")

data class UserPreferences(
    val username: String          = "Player",
    val memoryMb: Int             = 512,
    val showSnapshots: Boolean    = false,
    val selectedVersionId: String = "",
    val lastPlayedVersion: String = "",
    val javaArgs: String          = "",
    val fullscreen: Boolean       = true,
    val autoInstallAssets: Boolean = true
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val USERNAME             = stringKey("username")
        val MEMORY_MB            = intKey("memory_mb")
        val SHOW_SNAPSHOTS       = booleanKey("show_snapshots")
        val SELECTED_VERSION     = stringKey("selected_version")
        val LAST_PLAYED          = stringKey("last_played_version")
        val JAVA_ARGS            = stringKey("java_args")
        val FULLSCREEN           = booleanKey("fullscreen")
        val AUTO_INSTALL_ASSETS  = booleanKey("auto_install_assets")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserPreferences(
                username            = prefs[Keys.USERNAME]            ?: "Player",
                memoryMb            = prefs[Keys.MEMORY_MB]           ?: 512,
                showSnapshots       = prefs[Keys.SHOW_SNAPSHOTS]      ?: false,
                selectedVersionId   = prefs[Keys.SELECTED_VERSION]    ?: "",
                lastPlayedVersion   = prefs[Keys.LAST_PLAYED]         ?: "",
                javaArgs            = prefs[Keys.JAVA_ARGS]           ?: "",
                fullscreen          = prefs[Keys.FULLSCREEN]          ?: true,
                autoInstallAssets   = prefs[Keys.AUTO_INSTALL_ASSETS] ?: true
            )
        }

    suspend fun saveUsername(value: String) =
        context.dataStore.edit { it[Keys.USERNAME] = value }

    suspend fun saveMemoryMb(value: Int) =
        context.dataStore.edit { it[Keys.MEMORY_MB] = value }

    suspend fun saveShowSnapshots(value: Boolean) =
        context.dataStore.edit { it[Keys.SHOW_SNAPSHOTS] = value }

    suspend fun saveSelectedVersion(versionId: String) =
        context.dataStore.edit { it[Keys.SELECTED_VERSION] = versionId }

    suspend fun saveLastPlayed(versionId: String) =
        context.dataStore.edit { it[Keys.LAST_PLAYED] = versionId }

    suspend fun saveJavaArgs(value: String) =
        context.dataStore.edit { it[Keys.JAVA_ARGS] = value }

    suspend fun saveFullscreen(value: Boolean) =
        context.dataStore.edit { it[Keys.FULLSCREEN] = value }

    suspend fun saveAutoInstallAssets(value: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_INSTALL_ASSETS] = value }
}
