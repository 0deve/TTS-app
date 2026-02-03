package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val novel by viewModel.activeNovel.collectAsState()
    val chapters by viewModel.activeChapters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lines by viewModel.chapterLines.collectAsState()
    val currentIndex by viewModel.currentPlaybackIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()

    val currentChapterTitle = remember(novel, chapters) {
        chapters.find { it.index == (novel?.currentChapterIndex ?: 0) }?.title ?: "Unknown Chapter"
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex != -1) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val bgColor = Color(0xFF111827)
    val cardColor = Color(0xFF1F2937)
    val primaryColor = Color(0xFF3B82F6)
    val textColor = Color(0xFFF9FAFB)
    val secondaryColor = Color(0xFF9CA3AF)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                ),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = novel?.title ?: "Unknown",
                            fontSize = 12.sp,
                            color = secondaryColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentChapterTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.stopAudio(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (lines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No content loaded.", color = secondaryColor)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 160.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                    itemsIndexed(lines) { index, line ->
                        val isActive = (index == currentIndex)
                        Text(
                            text = line,
                            color = if (isActive) primaryColor else textColor,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 10).sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .background(if (isActive) primaryColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { viewModel.onSegmentClick(index) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${lines.size} segments",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFF374151),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                val newSpeed = when (ttsSpeed) { 0.5f -> 1.0f; 1.0f -> 1.5f; 1.5f -> 2.0f; 2.0f -> 3.0f; else -> 0.5f }; viewModel.setTtsSpeed(newSpeed)
                            }
                        ) {
                            Text(
                                text = "${String.format("%.1f", ttsSpeed)}x",
                                color = textColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val progress = if (lines.isNotEmpty()) (currentIndex + 1).toFloat() / lines.size else 0f
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = primaryColor, trackColor = Color(0xFF374151))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { novel?.let { if (it.currentChapterIndex > 0) viewModel.playFromIndex(it.currentChapterIndex - 1, autoPlay = true) } }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "prev", tint = secondaryColor)
                        }
                        Box(modifier = Modifier.size(56.dp).background(primaryColor, CircleShape).clickable { viewModel.togglePlayPause() }, contentAlignment = Alignment.Center) {
                            if (uiState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else if (isPlaying) Icon(painter = painterResource(android.R.drawable.ic_media_pause), contentDescription = "pause", tint = Color.White)
                            else Icon(Icons.Default.PlayArrow, contentDescription = "play", tint = Color.White)
                        }
                        IconButton(onClick = { novel?.let { if (it.currentChapterIndex < (chapters.size - 1)) viewModel.playFromIndex(it.currentChapterIndex + 1, autoPlay = true) } }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "next", tint = secondaryColor)
                        }
                    }
                }
            }
        }
    }
}