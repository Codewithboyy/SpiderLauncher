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

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home     : Screen("home",     "Home",     Icons.Filled.Home)
    object Versions : Screen("versions", "Versions", Icons.Filled.FormatListBulleted)
    object Console  : Screen("console",  "Console",  Icons.Filled.Terminal)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

private val bottomNavItems = listOf(Screen.Home, Screen.Versions, Screen.Console, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiderNavHost(viewModel: LauncherViewModel) {
    val navController = rememberNavController()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStack   by navController.currentBackStackEntryAsState()
                val currentDest    = navBackStack?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, screen.label) },
                        label    = { Text(screen.label) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = { navigateTo(screen.route) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(padding),
            enterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(180)) },
            exitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(180)) },
            popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(180)) },
            popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(180)) }
        ) {
            composable(Screen.Home.route) {
                // Pass settings navigation callback so the ⚙ icon on HomeScreen works
                HomeScreen(viewModel = viewModel, onOpenSettings = { navigateTo(Screen.Settings.route) })
            }
            composable(Screen.Versions.route) { VersionsScreen(viewModel) }
            composable(Screen.Console.route)  { ConsoleScreen(viewModel)  }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
