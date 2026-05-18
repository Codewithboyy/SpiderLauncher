package com.spiderlauncher.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spiderlauncher.android.model.VersionEntry
import com.spiderlauncher.android.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val displayedVersions = remember(searchQuery, state.filteredVersions) {
        if (searchQuery.isBlank()) state.filteredVersions
        else state.filteredVersions.filter { it.id.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ─────────────────────────────────────────────────────────
        TopAppBar(
            title = { Text("Minecraft Versions") },
            actions = {
                IconButton(onClick = viewModel::loadVersionManifest) {
                    Icon(Icons.Filled.Refresh, "Refresh versions")
                }
            }
        )

        // ── Search ───────────────────────────────────────────────────────────
        OutlinedTextField(
            value         = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder   = { Text("Search versions…") },
            leadingIcon   = { Icon(Icons.Filled.Search, null) },
            trailingIcon  = if (searchQuery.isNotBlank()) {{
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Filled.Clear, "Clear search")
                }
            }} else null,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true
        )

        // ── Snapshot Toggle ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Snapshots", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Switch(
                checked         = state.showSnapshots,
                onCheckedChange = viewModel::toggleSnapshots
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Version Count ────────────────────────────────────────────────────
        Text(
            "${displayedVersions.size} versions found",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(4.dp))

        // ── Loading State ────────────────────────────────────────────────────
        if (state.isLoadingVersions) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // ── Version List ─────────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedVersions, key = { it.id }) { version ->
                    VersionCard(
                        version      = version,
                        isSelected   = state.selectedVersion?.id == version.id,
                        isDownloaded = state.downloadedVersions.contains(version.id),
                        onSelect     = { viewModel.selectVersion(version) },
                        onDelete     = { showDeleteDialog = version.id }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    showDeleteDialog?.let { versionId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title   = { Text("Delete Version") },
            text    = { Text("Are you sure you want to delete $versionId? You'll need to re-download it to play.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVersion(versionId)
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VersionCard(
    version: VersionEntry,
    isSelected: Boolean,
    isDownloaded: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = when {
        isSelected   -> MaterialTheme.colorScheme.primaryContainer
        isDownloaded -> MaterialTheme.colorScheme.secondaryContainer
        else         -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier  = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = when {
                    isSelected   -> Icons.Filled.CheckCircle
                    isDownloaded -> Icons.Filled.DownloadDone
                    else         -> Icons.Filled.FiberManualRecord
                },
                contentDescription = null,
                tint = when {
                    isSelected   -> MaterialTheme.colorScheme.primary
                    isDownloaded -> MaterialTheme.colorScheme.secondary
                    else         -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Version info
            Column(Modifier.weight(1f)) {
                Text(
                    version.id,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyLarge
                )
                Text(
                    buildString {
                        append(version.type.replaceFirstChar { it.uppercase() })
                        if (isDownloaded) append(" • Downloaded")
                        if (isSelected)   append(" • Selected")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button (only for downloaded)
            if (isDownloaded) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
