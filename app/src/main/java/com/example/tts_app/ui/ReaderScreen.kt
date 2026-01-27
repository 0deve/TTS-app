package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val book by viewModel.activeBook.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val lines by viewModel.chapterLines.collectAsState()
    val currentIndex by viewModel.currentPlaybackIndex.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex != -1) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val chapters = remember(book) {
        book?.content?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Reader") },
                navigationIcon = {
                    Button(onClick = {
                        viewModel.stopAudio()
                        onBack()
                    }) { Text("<") }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Chapter ${(book?.currentChapterIndex ?: 0) + 1}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lines) { index, line ->
                            LineItem(
                                text = line,
                                isHighlighted = (index == currentIndex),
                                onPlayFromHere = {
                                    viewModel.playFromIndex(index)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        book?.let {
                            if (it.currentChapterIndex > 0) {
                                viewModel.updateProgress(it, it.currentChapterIndex - 1)
                            }
                        }
                    }
                ) { Text("Prev") }

                Button(
                    onClick = {
                        if (uiState is UiState.Loading || currentIndex != -1) {
                            viewModel.stopAudio()
                        } else {
                            viewModel.playFromIndex(0)
                        }
                    }
                ) {
                    if (uiState is UiState.Loading) {
                        Text("...")
                    } else if (currentIndex != -1) {
                        Text("Stop")
                    } else {
                        Text("Play All")
                    }
                }

                Button(
                    onClick = {
                        book?.let {
                            if (it.currentChapterIndex < chapters.lastIndex) {
                                viewModel.updateProgress(it, it.currentChapterIndex + 1)
                            }
                        }
                    }
                ) { Text("Next") }
            }
        }
    }
}

@Composable
fun LineItem(
    text: String,
    isHighlighted: Boolean,
    onPlayFromHere: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { showMenu = true }
            .padding(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("TTS from here") },
                onClick = {
                    showMenu = false
                    onPlayFromHere()
                }
            )
        }
    }
}