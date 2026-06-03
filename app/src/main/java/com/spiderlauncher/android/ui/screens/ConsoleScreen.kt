package com.spiderlauncher.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiderlauncher.android.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(viewModel: LauncherViewModel) {
    val state     by viewModel.uiState.collectAsState()
    val listState  = rememberLazyListState()
    val clipboard  = LocalClipboardManager.current
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(state.consoleLog.size) {
        if (autoScroll && state.consoleLog.isNotEmpty()) {
            listState.animateScrollToItem(state.consoleLog.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Console")
                    if (state.consoleLog.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("${state.consoleLog.size}",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            },
            actions = {
                // Auto-scroll lock toggle
                IconButton(onClick = { autoScroll = !autoScroll }) {
                    Icon(
                        if (autoScroll) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "Auto-scroll ${if (autoScroll) "on" else "off"}",
                        tint = if (autoScroll) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Copy all
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(state.consoleLog.joinToString("\n")))
                }) {
                    Icon(Icons.Filled.ContentCopy, "Copy all logs")
                }
                // Clear — now wired to viewModel.clearLog()
                IconButton(onClick = viewModel::clearLog) {
                    Icon(Icons.Filled.DeleteSweep, "Clear console")
                }
            }
        )

        // Terminal pane
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(14.dp))
        ) {
            if (state.consoleLog.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Terminal, null,
                        tint = Color(0xFF30A14E), modifier = Modifier.size(52.dp))
                    Text("Console is empty",
                        color = Color(0xFF8B949E), fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace)
                    Text("Download or launch a version to see output here",
                        color = Color(0xFF484F58), fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    state    = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(state.consoleLog) { line ->
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
                        Text(
                            text       = line,
                            color      = color,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier   = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 0.5.dp)
                        )
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                }

                // Scroll-to-bottom FAB
                SmallFloatingActionButton(
                    onClick        = { autoScroll = true },
                    modifier       = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    containerColor = Color(0xFF161B22),
                    contentColor   = Color(0xFF30A14E)
                ) {
                    Icon(Icons.Filled.ArrowDownward, "Scroll to bottom",
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
