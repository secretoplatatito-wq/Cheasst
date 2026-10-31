package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.screens.*
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.CheasstTheme
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextMuted

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Camera : Screen("camera", "Vision", Icons.Default.CameraAlt)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Analytics)
    object Modes : Screen("modes", "Modes", Icons.Default.Speed)
    object Memory : Screen("memory", "Memory", Icons.Default.Bookmark)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheasstTheme {
                CheasstApp()
            }
        }
    }
}

@Composable
fun CheasstApp() {
    val navController = rememberNavController()
    val viewModel: CheasstViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Screen.Home,
        Screen.Camera,
        Screen.Analysis,
        Screen.Modes,
        Screen.Memory,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = ElevatedSlate,
                tonalElevation = 4.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = TacticalEmerald,
                            indicatorColor = TacticalEmerald,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                    onNavigateToModes = { navController.navigate(Screen.Modes.route) },
                    onNavigateToMemory = { navController.navigate(Screen.Memory.route) }
                )
            }
            composable(Screen.Camera.route) {
                CameraVisionScreen(
                    viewModel = viewModel,
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) }
                )
            }
            composable(Screen.Analysis.route) {
                AnalysisScreen(
                    viewModel = viewModel
                )
            }
            composable(Screen.Modes.route) {
                ModesScreen(
                    viewModel = viewModel
                )
            }
            composable(Screen.Memory.route) {
                MemoryScreen(
                    viewModel = viewModel,
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
