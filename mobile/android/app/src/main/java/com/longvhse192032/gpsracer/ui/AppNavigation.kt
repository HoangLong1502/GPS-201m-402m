package com.longvhse192032.gpsracer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.ui.screens.HomeScreen
import com.longvhse192032.gpsracer.ui.screens.LeaderboardScreen
import com.longvhse192032.gpsracer.ui.screens.LoginScreen
import com.longvhse192032.gpsracer.ui.screens.ProfileScreen
import com.longvhse192032.gpsracer.ui.screens.ResultScreen
import com.longvhse192032.gpsracer.ui.screens.RouteMapScreen
import com.longvhse192032.gpsracer.ui.screens.TrackingScreen
import com.longvhse192032.gpsracer.ui.theme.GpsRacerTheme

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val TRACKING = "tracking/{mode}"
    const val RESULT = "result"
    const val ROUTE_MAP = "route_map"
    const val LEADERBOARD = "leaderboard"

    fun tracking(mode: RaceMode) = "tracking/${mode.name}"
}

@Composable
fun AppNavigation(appViewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val appState by appViewModel.appState.collectAsState()

    GpsRacerTheme {
        if (appState.isLoadingSession) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFF4D4F),
            )
            return@GpsRacerTheme
        }

        val start = if (appState.user != null) Routes.HOME else Routes.LOGIN

        LaunchedEffect(appState.user) {
            if (appState.user != null) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        NavHost(navController = navController, startDestination = start) {
            composable(Routes.LOGIN) {
                LoginScreen(appViewModel)
            }
            composable(Routes.HOME) {
                HomeScreen(
                    user = appState.user,
                    appViewModel = appViewModel,
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    onMode = { mode -> navController.navigate(Routes.tracking(mode)) },
                    onLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    user = appState.user,
                    appViewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.TRACKING,
                arguments = listOf(navArgument("mode") { type = NavType.StringType }),
            ) { entry ->
                val mode = RaceMode.valueOf(entry.arguments?.getString("mode") ?: RaceMode.GPS.name)
                TrackingScreen(
                    mode = mode,
                    user = appState.user,
                    appViewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                    onResult = {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onRouteMap = {
                        navController.navigate(Routes.ROUTE_MAP) {
                            popUpTo(Routes.HOME)
                        }
                    },
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    result = appState.latestResult,
                    onHome = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ROUTE_MAP) {
                RouteMapScreen(
                    result = appState.latestResult,
                    onHome = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(user = appState.user, onBack = { navController.popBackStack() })
            }
        }
    }
}
