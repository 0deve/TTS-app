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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.coroutines.launch

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
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsState()
    val textMargin by viewModel.textMargin.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val isOled by viewModel.isOledMode.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    var isImmersiveMode by remember { mutableStateOf(false) }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    var selectedSegmentIndex by remember { mutableStateOf(-1) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentIndex, isDragging) {
        if (!isDragging && currentIndex != -1) {
            sliderPosition = currentIndex.toFloat()
        }
    }

    val currentChapterTitle = remember(novel, chapters) {
        chapters.find { it.index == (novel?.currentChapterIndex ?: 0) }?.title ?: "Unknown Chapter"
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex != -1 && !isDragging) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val cardColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val primaryColor = Color(0xFF3B82F6)
    val textColor = Color(0xFFF9FAFB)
    val secondaryColor = Color(0xFF9CA3AF)
    val errorColor = Color(0xFFEF4444)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            if (!isImmersiveMode) {
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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState is UiState.Loading && lines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (lines.isEmpty() && uiState !is UiState.Error) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No content loaded.", color = secondaryColor)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = textMargin.dp),
                    contentPadding = PaddingValues(
                        top = 20.dp,
                        bottom = if (isImmersiveMode) 80.dp else 240.dp
                    )
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = (index == currentIndex)
                        val isSelected = (index == selectedSegmentIndex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = line,
                                color = if (isActive) primaryColor else textColor,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineHeightMultiplier).sp,
                                fontFamily = fontFamily,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isActive) primaryColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                    }
                                    .padding(4.dp)
                            )

                            if (isSelected) {
                                Surface(
                                    color = cardColor,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor),
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .align(Alignment.End)
                                        .clickable {
                                            viewModel.onSegmentClick(index)
                                            selectedSegmentIndex = -1
                                        }
                                ) {
                                    Text(
                                        text = "TTS from here",
                                        color = primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState is UiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = errorColor),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text((uiState as UiState.Error).message, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.retryAudio() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = errorColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry", color = errorColor)
                        }
                    }
                }
            }

            if (!isImmersiveMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).padding(bottom = 16.dp),
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

                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(sliderPosition.toInt())
                                }
                                isDragging = false
                            },
                            valueRange = 0f..lines.lastIndex.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0xFF374151)
                            ),
                            modifier = Modifier.fillMaxWidth().height(20.dp)
                        )

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

            SmallFloatingActionButton(
                onClick = { isImmersiveMode = !isImmersiveMode },
                containerColor = primaryColor,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isImmersiveMode) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Immersive",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}