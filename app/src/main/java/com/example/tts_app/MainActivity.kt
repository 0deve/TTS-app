package com.example.tts_app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.AppDatabase
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import com.example.tts_app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#111827")
        window.navigationBarColor = Color.parseColor("#111827")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val bookDao = database.bookDao()

        val repository = TtsRepository(applicationContext, bookDao)

        val audioPlayer = AudioPlayerManager(applicationContext)
        val localTts = LocalTtsManager(applicationContext)

        val factory = MainViewModelFactory(repository, audioPlayer, localTts, bookDao)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = ComposeColor(0xFF111827)) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}