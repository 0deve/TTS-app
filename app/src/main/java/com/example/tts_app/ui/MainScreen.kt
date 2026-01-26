package com.example.tts_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var textInput by remember { mutableStateOf("The quick brown fox jumps over the lazy dog. Amidst the whispering woods, the silver stream hummed a quiet lullaby. It was the best of times, it was the worst of times—a paradox of ordinary life, where every shadow held a secret and every light promised a new beginning") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TTS App AllTalk") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.generateAudio(textInput)
                },
                enabled = uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate")
                } else {
                    Text("Generate")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.stopAudio() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("STOP")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is UiState.Success -> {
                    Text(
                        text = "succes",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is UiState.Error -> {
                    Text(
                        text = "${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}