package com.spiderlauncher.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.spiderlauncher.android.model.DownloadState
import com.spiderlauncher.android.model.LaunchState
import com.spiderlauncher.android.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing)),
        label = "rotate"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrollable body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Top icon row: [console toggle]  [settings]  [placeholder] ────
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                // ⚙ Settings — centred at the top
                IconButton(
                    onClick  = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Icon(Icons.Filled.Settings, "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // ▶ Console toggle — top right
                IconButton(
                    onClick  = viewModel::toggleConsoleOverlay,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Filled.Terminal, "Toggle console",
                        tint = if (state.showConsoleOverlay)
                                   MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Logo ────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        ))
                    )
            ) {
                Icon(Icons.Filled.Gamepad, null,
                    tint     = Color.White,
                    modifier = Modifier.size(60.dp).rotate(rotation * 0.08f))
            }

            Text("SpiderLauncher",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Minecraft Java Edition • Android",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()

            // ── Profile card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Person, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Player Profile", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value         = state.profile.username,
                        onValueChange = viewModel::updateUsername,
                        label         = { Text("Username") },
                        leadingIcon   = { Icon(Icons.Filled.AccountCircle, null) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )

                    val verId        = state.selectedVersion?.id ?: "None selected"
                    val isDownloaded = state.downloadedVersions.contains(state.selectedVersion?.id)
                    AssistChip(
                        onClick = {},
                        label   = { Text("Version: $verId") },
                        leadingIcon = {
                            Icon(
                                if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.CloudDownload,
                                null,
                                tint = if (isDownloaded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // ── Download progress ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.downloadState is DownloadState.Downloading ||
                          state.downloadState is DownloadState.Error
            ) {
                when (val dl = state.downloadState) {
                    is DownloadState.Downloading -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(dl.label, style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = dl.progress / 100f,
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    is DownloadState.Error -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(dl.message,
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                style    = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearDownloadState,
                                modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    else -> {}
                }
            }

            // ── Error message ─────────────────────────────────────────────────
            state.errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Text(msg, modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Running banner ────────────────────────────────────────────────
            AnimatedVisibility(visible = state.launchState is LaunchState.Running) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayCircle, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Minecraft is running", fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = viewModel::stopGame) { Text("Stop") }
                    }
                }
            }

            // ── Buttons ───────────────────────────────────────────────────────
            val selectedVersion = state.selectedVersion
            val isDownloaded    = selectedVersion != null &&
                                  state.downloadedVersions.contains(selectedVersion.id)
            val isDownloading   = state.downloadState is DownloadState.Downloading
            val isLaunching     = state.launchState is LaunchState.Launching ||
                                  state.launchState is LaunchState.Running

            AnimatedVisibility(visible = selectedVersion != null && !isDownloaded && !isDownloading) {
                Button(
                    onClick  = viewModel::downloadSelectedVersion,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download ${selectedVersion?.id ?: ""}", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick  = viewModel::launchGame,
                enabled  = isDownloaded && !isLaunching,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isLaunching  -> MaterialTheme.colorScheme.surfaceVariant
                        isDownloaded -> MaterialTheme.colorScheme.primary
                        else         -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isDownloaded && !isLaunching) 4.dp else 0.dp)
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Launching…", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                } else {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play Minecraft", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }

            if (!isDownloaded && selectedVersion == null) {
                Text("Go to Versions tab to select and download a version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }

        // ── In-game console overlay (slides up from bottom) ───────────────────
        AnimatedVisibility(
            visible  = state.showConsoleOverlay,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            InGameConsole(
                logs    = state.consoleLog,
                onClose = viewModel::hideConsoleOverlay,
                onClear = viewModel::clearLog
            )
        }
    }
}

// ── Bottom-sheet style in-game console ───────────────────────────────────────

@Composable
private fun InGameConsole(
    logs: List<String>,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Surface(
        modifier        = Modifier.fillMaxWidth().fillMaxHeight(0.46f),
        shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color           = Color(0xFF0D1117),
        shadowElevation = 20.dp,
        tonalElevation  = 8.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            // Handle bar + toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(38.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF30A14E))
                )
                Spacer(Modifier.width(12.dp))
                Text("Console", color = Color(0xFF30A14E), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f))
                Text("${logs.size} lines", color = Color(0xFF484F58),
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.DeleteSweep, null,
                        tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, null,
                        tint = Color(0xFF8B949E), modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)

            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No output yet…",
                        color = Color(0xFF8B949E), fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    state   = listState,
                    modifier= Modifier.fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.contains("✗") || line.contains("error",  true) ||
                            line.contains("failed", true)              -> Color(0xFFFF6B6B)
                            line.contains("warning", true)             -> Color(0xFFFFD93D)
                            line.contains("✓") || line.contains("complete", true) ||
                            line.contains("started", true) ||
                            line.contains("installed", true)           -> Color(0xFF30A14E)
                            line.contains("Downloading") || line.contains(" MB") -> Color(0xFF58A6FF)
                            line.contains("━")                         -> Color(0xFF484F58)
                            else                                       -> Color(0xFFE6EDF3)
                        }
                        Text(line, color = color, fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace, lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 0.5.dp))
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
