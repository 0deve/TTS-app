package com.example.tts_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val book by viewModel.activeBook.collectAsState()
    val uiState by viewModel.uiState.collectAsState()


    val chapters = remember(book) {
        book?.content?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val currentText = if (chapters.isNotEmpty() && book != null) {
        chapters.getOrElse(book!!.currentChapterIndex) { "End" }
    } else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Reader") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("<") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Chapter ${(book?.currentChapterIndex ?: 0) + 1}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = currentText, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        book?.let {
                            if (it.currentChapterIndex > 0) {
                                viewModel.updateProgress(it, it.currentChapterIndex - 1)
                                viewModel.stopAudio()
                            }
                        }
                    }
                ) { Text("Prev") }

                Button(
                    onClick = { viewModel.generateAudio(currentText) },
                    enabled = uiState !is UiState.Loading
                ) {
                    Text(if (uiState is UiState.Loading) "..." else "Play Cap")
                }

                Button(
                    onClick = {
                        book?.let {
                            if (it.currentChapterIndex < chapters.lastIndex) {
                                viewModel.updateProgress(it, it.currentChapterIndex + 1)
                                viewModel.stopAudio()
                            }
                        }
                    }
                ) { Text("Next") }
            }
        }
    }
}