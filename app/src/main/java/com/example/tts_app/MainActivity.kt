package com.example.tts_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.AppDatabase
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.ui.*
import com.example.tts_app.player.LocalTtsManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TtsRepository(applicationContext)
        val audioPlayer = AudioPlayerManager(applicationContext)
        val localTts = LocalTtsManager(applicationContext)

        val factory = MainViewModelFactory(repository, audioPlayer, localTts, database.bookDao())

        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            AppNavigation(viewModel)
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {

        // ecranul principal cu lista de carti
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onBookSelected = { bookId ->
                    viewModel.openBook(bookId)
                    navController.navigate("reader")
                }
            )
        }

        composable("reader") {
            ReaderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }
    }
}