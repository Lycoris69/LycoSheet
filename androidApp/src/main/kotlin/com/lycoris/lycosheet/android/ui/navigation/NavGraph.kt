package com.lycoris.lycosheet.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lycoris.lycosheet.android.ui.home.HomeScreen
import com.lycoris.lycosheet.android.ui.library.DeckDetailScreen
import com.lycoris.lycosheet.android.ui.library.LibraryScreen
import com.lycoris.lycosheet.android.ui.settings.SettingsScreen
import com.lycoris.lycosheet.android.ui.study.StudyScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun LycoSheetNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, "Create", Icons.Default.Home),
        BottomNavItem(Screen.Library, "Library", Icons.Default.LibraryBooks),
        BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
    )

    // Only show bottom bar on top-level screens
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onDeckClick = { deckId ->
                        navController.navigate(Screen.DeckDetail.createRoute(deckId))
                    },
                    onStartStudy = { deckId ->
                        navController.navigate(Screen.Study.createRoute(deckId))
                    }
                )
            }
            composable(
                route = Screen.DeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                DeckDetailScreen(
                    deckId = deckId,
                    onBack = { navController.popBackStack() },
                    onStartStudy = { navController.navigate(Screen.Study.createRoute(deckId)) }
                )
            }
            composable(
                route = Screen.Study.route,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                StudyScreen(deckId = deckId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
