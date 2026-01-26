package com.aihos.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aihos.ui.screens.DashboardScreen
import com.aihos.ui.screens.SettingsScreen
import com.aihos.ui.screens.VisualizationScreen

/**
 * Navigation Routes
 */
object NavRoute {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val VISUALIZATION = "visualization"
}

/**
 * Navigation Graph for SA-AIHOS App
 */
@Composable
fun SAIHOSNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.DASHBOARD
    ) {
        composable(NavRoute.DASHBOARD) {
            DashboardScreen(
                onSettingsClick = {
                    navController.navigate(NavRoute.SETTINGS)
                },
                onVisualizationClick = {
                    navController.navigate(NavRoute.VISUALIZATION)
                }
            )
        }

        composable(NavRoute.SETTINGS) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoute.VISUALIZATION) {
            VisualizationScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
