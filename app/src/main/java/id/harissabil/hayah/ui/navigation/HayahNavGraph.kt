package id.harissabil.hayah.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.harissabil.hayah.ui.navigation.components.HayahBottomBar
import id.harissabil.hayah.ui.screens.home.HomeScreen
import id.harissabil.hayah.ui.screens.journal.JournalScreen
import id.harissabil.hayah.ui.screens.onboarding.OnboardingScreen
import id.harissabil.hayah.ui.screens.settings.SettingsScreen

@Composable
fun HayahNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarScreens = listOf(Screen.Home, Screen.Journal, Screen.Settings)
    val showBottomBar = bottomBarScreens.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HayahBottomBar(
                    screens = bottomBarScreens,
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Journal.route) {
                JournalScreen(
                    onNavigateToQuranReader = {
                        navController.navigate(Screen.QuranReading.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.QuranReading.route) {
                // Empty for now, will implement Quran reading screen later
            }
        }
    }
}
