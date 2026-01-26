package com.example.tts_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isServerEnabled by viewModel.isServerTtsEnabled.collectAsState()

    var textInput by remember { mutableStateOf("Test for the Text To Speech Voice") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Audio Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Voice Type", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isServerEnabled) "Server" else "Local",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = isServerEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.setTtsMode(isChecked)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text("Test TTS", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Test for the Text To Speech Voice") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.generateAudio(textInput) },
                enabled = uiState !is UiState.Loading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Test")
                }
            }

            Button(
                onClick = { viewModel.stopAudio() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(0.5f)
            ) {
                Text("Stop")
            }
        }

        if (uiState is UiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}