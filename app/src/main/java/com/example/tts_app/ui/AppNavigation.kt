package com.example.tts_app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onBookSelected = { novelId ->
                    navController.navigate("novel_details")
                },
                onNavigateToBrowse = { navController.navigate("browse") },
                onNavigateToStatistics = { navController.navigate("statistics") }
            )
        }
        composable("novel_details") {
            NovelDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlayChapter = { index ->
                    viewModel.playFromIndex(index, autoPlay = false)
                    navController.navigate("reader")
                }
            )
        }
        composable("browse") {
            BrowseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("reader") {
            ReaderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("statistics") {
            StatisticsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}