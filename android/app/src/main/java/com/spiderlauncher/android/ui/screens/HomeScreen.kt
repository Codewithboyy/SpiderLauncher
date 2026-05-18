package com.spiderlauncher.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.LaunchState
import com.spiderlauncher.android.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsState()

    // Spider web rotation animation for logo
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Spacer(Modifier.height(12.dp))

        // ── Logo ────────────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        ) {
            Icon(
                Icons.Filled.Gamepad,
                contentDescription = "SpiderLauncher",
                tint   = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation * 0.1f) // subtle sway
            )
        }

        Text(
            "SpiderLauncher",
            style     = MaterialTheme.typography.headlineMedium,
            fontWeight= FontWeight.Bold,
            color     = MaterialTheme.colorScheme.primary
        )
        Text(
            "Minecraft Java Edition • Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // ── Profile Card ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Player Profile", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value        = state.profile.username,
                    onValueChange= viewModel::updateUsername,
                    label        = { Text("Username") },
                    leadingIcon  = { Icon(Icons.Filled.Person, null) },
                    modifier     = Modifier.fillMaxWidth(),
                    singleLine   = true
                )

                // Version chip
                val verId = state.selectedVersion?.id ?: "None selected"
                val isDownloaded = state.downloadedVersions.contains(state.selectedVersion?.id)
                AssistChip(
                    onClick = {},
                    label   = { Text("Version: $verId") },
                    leadingIcon = {
                        Icon(
                            if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                            null,
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // ── Download Progress ─────────────────────────────────────────────────
        when (val dl = state.downloadState) {
            is DownloadState.Downloading -> {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(dl.label, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = dl.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${dl.progress}%",
                            style  = MaterialTheme.typography.bodySmall,
                            color  = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is DownloadState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                        Text(dl.message, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            else -> {}
        }

        // ── Action Buttons ────────────────────────────────────────────────────
        val selectedVersion = state.selectedVersion
        val isDownloaded    = selectedVersion != null &&
                              state.downloadedVersions.contains(selectedVersion.id)
        val isDownloading   = state.downloadState is DownloadState.Downloading
        val isLaunching     = state.launchState is LaunchState.Launching ||
                              state.launchState is LaunchState.Running

        if (selectedVersion != null && !isDownloaded) {
            Button(
                onClick  = viewModel::downloadSelectedVersion,
                enabled  = !isDownloading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Downloading…")
                } else {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download ${selectedVersion.id}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick  = viewModel::launchGame,
            enabled  = isDownloaded && !isLaunching,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isDownloaded) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (isLaunching) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Launching…", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Play Minecraft", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (!isDownloaded && selectedVersion == null) {
            Text(
                "← Go to Versions tab to select and download a version",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
