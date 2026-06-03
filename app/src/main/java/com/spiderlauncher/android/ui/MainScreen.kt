package com.spiderlauncher.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spiderlauncher.android.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LauncherViewModel
) {

    val state by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {

                Text(
                    "Spider Overlay",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                NavigationDrawerItem(
                    label = { Text("Force Close") },
                    selected = false,
                    icon = {
                        Icon(Icons.Default.Close, null)
                    },
                    onClick = {
                        viewModel.forceCloseGame()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Log Output") },
                    selected = false,
                    icon = {
                        Icon(Icons.Default.List, null)
                    },
                    onClick = {
                        viewModel.showLogOutput()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Send Keycode") },
                    selected = false,
                    icon = {
                        Icon(Icons.Default.Keyboard, null)
                    },
                    onClick = {
                        viewModel.showKeycodes()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Quick Settings") },
                    selected = false,
                    icon = {
                        Icon(Icons.Default.Settings, null)
                    },
                    onClick = {
                        viewModel.showQuickSettings()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Custom Controls") },
                    selected = false,
                    icon = {
                        Icon(Icons.Default.SportsEsports, null)
                    },
                    onClick = {
                        viewModel.showCustomControls()
                    }
                )
            }
        }
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            SpiderNavHost(
                viewModel = viewModel
            )
            
            if (state.showLogOutput) {
                LogViewer(
                    logs = state.consoleLog
                )
            }
            
            AnimatedVisibility(
                visible = state.launchState.toString().contains("Running"),
                modifier = Modifier.align(
                    Alignment.CenterEnd
                )
            ) {

                FloatingActionButton(
                    modifier = Modifier.padding(
                        end = 16.dp
                    ),
                    onClick = {
                        scope.launch {

                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }

                        }
                    }
                ) {

                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null
                    )

                }

            }

        }

    }

}
