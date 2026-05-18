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

    // Auto-scroll to bottom on new log entries
    LaunchedEffect(state.consoleLog.size) {
        if (state.consoleLog.isNotEmpty()) {
            listState.animateScrollToItem(state.consoleLog.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Console") },
            actions = {
                IconButton(onClick = {
                    val text = state.consoleLog.joinToString("\n")
                    clipboard.setText(AnnotatedString(text))
                }) {
                    Icon(Icons.Filled.ContentCopy, "Copy logs")
                }
                IconButton(onClick = { /* viewModel.clearLog() */ }) {
                    Icon(Icons.Filled.DeleteSweep, "Clear console")
                }
            }
        )

        // Console background panel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Color(0xFF0D1117), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (state.consoleLog.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Terminal, null,
                        tint     = Color(0xFF30A14E),
                        modifier = Modifier.size(48.dp))
                    Text(
                        "Console is empty\nLaunch Minecraft to see output",
                        color     = Color(0xFF8B949E),
                        fontSize  = 14.sp,
                        fontFamily= FontFamily.Monospace,
                        lineHeight= 20.sp
                    )
                }
            } else {
                LazyColumn(
                    state   = listState,
                    modifier= Modifier.fillMaxSize()
                ) {
                    items(state.consoleLog) { line ->
                        val color = when {
                            line.contains("Error", ignoreCase = true) ||
                            line.contains("Failed", ignoreCase = true)  -> Color(0xFFFF6B6B)
                            line.contains("Warning", ignoreCase = true) -> Color(0xFFFFD93D)
                            line.contains("complete", ignoreCase = true)||
                            line.contains("Done", ignoreCase = true)    -> Color(0xFF30A14E)
                            line.contains("Downloading")                -> Color(0xFF58A6FF)
                            else                                        -> Color(0xFFE6EDF3)
                        }
                        Text(
                            text      = line,
                            color     = color,
                            fontSize  = 11.sp,
                            fontFamily= FontFamily.Monospace,
                            lineHeight= 16.sp,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 1.dp)
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Scroll to bottom FAB
                FloatingActionButton(
                    onClick   = { /* handled by LaunchedEffect */ },
                    modifier  = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp),
                    containerColor = Color(0xFF161B22)
                ) {
                    Icon(Icons.Filled.ArrowDownward, "Scroll to bottom",
                        tint = Color(0xFF30A14E), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
