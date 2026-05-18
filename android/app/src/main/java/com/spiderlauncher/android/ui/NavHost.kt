package com.spiderlauncher.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.spiderlauncher.android.ui.screens.*
import com.spiderlauncher.android.viewmodel.LauncherViewModel

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Filled.Home)
    object Versions : Screen("versions", "Versions", Icons.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Console  : Screen("console",  "Console",  Icons.Filled.Terminal)
}

private val bottomNavItems = listOf(Screen.Home, Screen.Versions, Screen.Console, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiderNavHost(viewModel: LauncherViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon  = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start,  tween(200)) },
            exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
            popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) },
            popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End,tween(200)) }
        ) {
            composable(Screen.Home.route)     { HomeScreen(viewModel) }
            composable(Screen.Versions.route) { VersionsScreen(viewModel) }
            composable(Screen.Console.route)  { ConsoleScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
