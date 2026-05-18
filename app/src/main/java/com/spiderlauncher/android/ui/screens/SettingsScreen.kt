package com.spiderlauncher.android.ui.screens

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(title = { Text("Settings") })

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Player Section ───────────────────────────────────────────────
            SettingsSection(title = "Player", icon = Icons.Filled.Person) {
                OutlinedTextField(
                    value         = state.profile.username,
                    onValueChange = viewModel::updateUsername,
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Filled.AccountCircle, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
            }

            // ── Java / Memory Section ────────────────────────────────────────
            SettingsSection(title = "Performance", icon = Icons.Filled.Memory) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Memory, null,
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("RAM Allocation", fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f))
                        Text("${state.profile.memoryMb} MB",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value        = state.profile.memoryMb.toFloat(),
                        onValueChange= { viewModel.updateMemory(it.toInt()) },
                        valueRange   = 256f..4096f,
                        steps        = 14  // 256, 512, 768 … 4096
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("256 MB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4096 MB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Version Preferences ─────────────────────────────────────────
            SettingsSection(title = "Versions", icon = Icons.Filled.List) {
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
            }

            // ── About ────────────────────────────────────────────────────────
            SettingsSection(title = "About", icon = Icons.Filled.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutRow(label = "App",     value = "SpiderLauncher")
                    AboutRow(label = "Version", value = "1.0.0")
                    AboutRow(label = "Platform","Android")
                    AboutRow(label = "GitHub",  value = "github.com/SpiderLauncher")
                    AboutRow(label = "License", value = "MIT")

                    Divider()

                    Text(
                        "SpiderLauncher is an unofficial launcher. Minecraft is a trademark of Mojang Studios.",
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
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp),
            style    = MaterialTheme.typography.bodyMedium)
        Text(value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
}
