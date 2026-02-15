package com.example.tts_app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.example.tts_app.data.PreferencesManager
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.AppDatabase
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import com.example.tts_app.service.TtsService
import com.example.tts_app.ui.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_PLAY" -> viewModel.togglePlayPause()
                "ACTION_PAUSE" -> viewModel.togglePlayPause()
                "ACTION_NEXT" -> viewModel.playNext()
                "ACTION_PREVIOUS" -> viewModel.playPrevious()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val bookDao = database.bookDao()

        val repository = TtsRepository(applicationContext, bookDao)
        val preferencesManager = PreferencesManager(applicationContext)

        val audioPlayer = AudioPlayerManager(applicationContext)
        val localTts = LocalTtsManager(applicationContext)
        val workManager = WorkManager.getInstance(applicationContext)

        val factory = MainViewModelFactory(applicationContext, repository, audioPlayer, localTts, bookDao, workManager, preferencesManager)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        val filter = IntentFilter().apply {
            addAction("ACTION_PLAY")
            addAction("ACTION_PAUSE")
            addAction("ACTION_NEXT")
            addAction("ACTION_PREVIOUS")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mediaReceiver, filter)
        }

        setContent {
            val isOled by viewModel.isOledMode.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val context = LocalContext.current

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(permission)
                    }
                }
            }

            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(isOled) {
                window.statusBarColor = if (isOled) Color.BLACK else Color.parseColor("#111827")
                window.navigationBarColor = if (isOled) Color.BLACK else Color.parseColor("#111827")
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isOled) ComposeColor.Black else ComposeColor(0xFF111827)
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mediaReceiver)
        val intent = Intent(applicationContext, TtsService::class.java)
        intent.action = "STOP_SERVICE"
        startService(intent)
    }
}