package com.spiderlauncher.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spiderlauncher.android.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsState()

    // "Saved" toast — fires whenever any setting changes
    var showSaved by remember { mutableStateOf(false) }
    val settingsKey = "${state.profile.username}|${state.profile.memoryMb}|" +
        "${state.showSnapshots}|${state.javaArgs}|${state.fullscreen}|${state.autoInstallAssets}"
    LaunchedEffect(settingsKey) {
        showSaved = true
        kotlinx.coroutines.delay(2000)
        showSaved = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Settings") },
            actions = {
                AnimatedVisibility(
                    visible = showSaved,
                    enter   = fadeIn() + slideInHorizontally { it },
                    exit    = fadeOut() + slideOutHorizontally { it }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 14.dp)
                    ) {
                        Icon(Icons.Filled.Check, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Text("Saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Player ────────────────────────────────────────────────────────
            SettingsSection("Player", Icons.Filled.Person) {
                OutlinedTextField(
                    value         = state.profile.username,
                    onValueChange = viewModel::updateUsername,
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Filled.AccountCircle, null) },
                    supportingText= { Text("Your offline-mode display name") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
            }

            // ── Performance ───────────────────────────────────────────────────
            SettingsSection("Performance", Icons.Filled.Memory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Memory, null,
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("RAM Allocation", fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("${state.profile.memoryMb} MB",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }

                    Slider(
                        value         = state.profile.memoryMb.toFloat(),
                        onValueChange = { viewModel.updateMemory(it.toInt()) },
                        valueRange    = 256f..4096f,
                        steps         = 14
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("256 MB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Recommended: 1024–2048 MB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4096 MB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedTextField(
                        value         = state.javaArgs,
                        onValueChange = viewModel::updateJavaArgs,
                        label         = { Text("Extra JVM Arguments") },
                        placeholder   = { Text("-XX:+UseG1GC -XX:MaxGCPauseMillis=20") },
                        leadingIcon   = { Icon(Icons.Filled.Code, null) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                }
            }

            // ── Versions ──────────────────────────────────────────────────────
            SettingsSection("Versions", Icons.Filled.List) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show Snapshots", fontWeight = FontWeight.Medium)
                            Text("Include snapshot & beta builds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked         = state.showSnapshots,
                            onCheckedChange = viewModel::toggleSnapshots
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-install Assets & Libraries", fontWeight = FontWeight.Medium)
                            Text("Download sounds, textures & libs on version download",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked         = state.autoInstallAssets,
                            onCheckedChange = viewModel::updateAutoInstallAssets
                        )
                    }

                    state.selectedVersion?.let { v ->
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Selected: ${v.id}", fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (state.downloadedVersions.contains(v.id))
                                        "✓ Downloaded and ready"
                                    else "Not yet downloaded — go to Versions tab",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.downloadedVersions.contains(v.id))
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Game ──────────────────────────────────────────────────────────
            SettingsSection("Game", Icons.Filled.Gamepad) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fullscreen", fontWeight = FontWeight.Medium)
                        Text("Launch Minecraft in fullscreen mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = state.fullscreen,
                        onCheckedChange = viewModel::updateFullscreen
                    )
                }
            }

            // ── Storage ───────────────────────────────────────────────────────
            SettingsSection("Storage", Icons.Filled.Storage) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val count = state.downloadedVersions.size
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Folder, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("$count version${if (count != 1) "s" else ""} downloaded",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "All data stored in app external storage and persists between sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            SettingsSection("About", Icons.Filled.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutRow("App",      "SpiderLauncher")
                    AboutRow("Version",  "1.0.0")
                    AboutRow("Platform", "Android  •  Jetpack Compose")
                    AboutRow("GitHub",   "github.com/SpiderLauncher")
                    AboutRow("License",  "MIT")
                    HorizontalDivider()
                    Text(
                        "Unofficial launcher. Minecraft is a trademark of Mojang Studios. Not affiliated with Mojang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
}
