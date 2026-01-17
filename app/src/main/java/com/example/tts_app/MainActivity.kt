package com.example.tts_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.ui.MainScreen
import com.example.tts_app.ui.MainViewModel
import com.example.tts_app.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = TtsRepository(applicationContext)
        val audioPlayer = AudioPlayerManager(applicationContext)

        val factory = MainViewModelFactory(repository, audioPlayer)

        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }
}