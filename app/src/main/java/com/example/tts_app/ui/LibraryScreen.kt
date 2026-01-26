package com.example.tts_app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onBookSelected: (Int) -> Unit
) {
    val books by viewModel.books.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.importBookMock(
                    "Demo ${books.size + 1}",
                    "The quick brown fox jumps over the lazy dog. Amidst the whispering woods, the silver stream hummed a quiet lullaby. It was the best of times, it was the worst of times—a paradox of ordinary life, where every shadow held a secret and every light promised a new beginning"
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Import")
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("+ for demo")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(books) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onBookSelected(book.id) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Progress: Chapter ${book.currentChapterIndex + 1} / ${book.totalChapters}")
                            LinearProgressIndicator(
                                progress = if (book.totalChapters > 0) (book.currentChapterIndex + 1) / book.totalChapters.toFloat() else 0f,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}