package com.example.tts_app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val fontColorVal by viewModel.fontColor.collectAsState()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsState()
    val textMargin by viewModel.textMargin.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val isOled by viewModel.isOledMode.collectAsState()

    val viewingIndex by viewModel.viewingChapterIndex.collectAsState()

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

    LaunchedEffect(lines) {
        if (lines.isNotEmpty()) {
            val targetIndex = if (currentIndex != -1) currentIndex else 0
            listState.scrollToItem(targetIndex)
        }
    }

    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val progressPercent by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visibleInfo = layoutInfo.visibleItemsInfo

            if (total == 0 || visibleInfo.isEmpty()) {
                0
            } else {
                val lastVisibleIndex = visibleInfo.last().index
                if (lastVisibleIndex == total - 1) {
                    100
                } else {
                    ((listState.firstVisibleItemIndex.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 99)
                }
            }
        }
    }

    LaunchedEffect(firstVisibleItemIndex) {
        viewModel.updateProgressIfThresholdMet(firstVisibleItemIndex, lines.size)
    }

    LaunchedEffect(currentIndex, isDragging) {
        if (!isDragging && currentIndex != -1) {
            sliderPosition = currentIndex.toFloat()
        }
    }

    val currentChapterTitle = remember(novel, chapters, viewingIndex) {
        chapters.find { it.index == viewingIndex }?.title ?: "Unknown Chapter"
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex != -1 && !isDragging) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val cardColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val primaryColor = Color(0xFF3B82F6)
    val textColor = Color(fontColorVal)
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
        },
        bottomBar = {
            if (!isImmersiveMode && lines.isNotEmpty() && uiState !is UiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .navigationBarsPadding()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.reloadCurrentChapter() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload chapter", tint = textColor)
                                }
                                IconButton(onClick = { viewModel.setTtsSpeed((ttsSpeed - 0.1f).coerceAtLeast(0.5f)) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease speed", tint = textColor)
                                }
                                Text(
                                    text = "${String.format("%.1f", ttsSpeed)}x",
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(onClick = { viewModel.setTtsSpeed((ttsSpeed + 0.1f).coerceAtMost(3.0f)) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase speed", tint = textColor)
                                }
                            }
                        }

                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                viewModel.onSegmentClick(sliderPosition.toInt())
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
                            IconButton(onClick = { novel?.let { if (viewingIndex > 0) viewModel.playFromIndex(viewingIndex - 1, autoPlay = isPlaying) } }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "prev", tint = secondaryColor)
                            }
                            Box(modifier = Modifier.size(56.dp).background(primaryColor, CircleShape).clickable { viewModel.togglePlayPause() }, contentAlignment = Alignment.Center) {
                                if (uiState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else if (isPlaying) Icon(painter = painterResource(android.R.drawable.ic_media_pause), contentDescription = "pause", tint = Color.White)
                                else Icon(Icons.Default.PlayArrow, contentDescription = "play", tint = Color.White)
                            }
                            IconButton(onClick = { novel?.let { if (viewingIndex < (chapters.size - 1)) viewModel.playFromIndex(viewingIndex + 1, autoPlay = isPlaying) } }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "next", tint = secondaryColor)
                            }
                        }
                    }
                }
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
                    contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
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
                                    .combinedClickable(
                                        onClick = { isImmersiveMode = !isImmersiveMode },
                                        onLongClick = {
                                            selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                        }
                                    )
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

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentIdx = viewingIndex
                            val maxIdx = (chapters.size - 1).coerceAtLeast(0)

                            if (currentIdx > 0) {
                                Button(
                                    onClick = { viewModel.playFromIndex(currentIdx - 1, autoPlay = isPlaying) },
                                    colors = ButtonDefaults.buttonColors(containerColor = cardColor)
                                ) {
                                    Text("Previous Chapter", color = textColor)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            if (currentIdx < maxIdx) {
                                Button(
                                    onClick = { viewModel.playFromIndex(currentIdx + 1, autoPlay = isPlaying) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("Next Chapter", color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (lines.isNotEmpty()) {
                    Text(
                        text = "$progressPercent%",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    )
                }

                if (!isImmersiveMode) {
                    val layoutInfo = listState.layoutInfo
                    val totalItems = layoutInfo.totalItemsCount
                    val visibleItems = layoutInfo.visibleItemsInfo.size

                    if (totalItems > visibleItems) {
                        val density = LocalDensity.current
                        var trackHeightPx by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .offset(y = (-80).dp)
                                .fillMaxHeight(0.5f)
                                .width(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .onGloballyPositioned { coordinates ->
                                    trackHeightPx = coordinates.size.height.toFloat()
                                }
                                .pointerInput(totalItems, trackHeightPx) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (trackHeightPx > 0) {
                                            val ratio = dragAmount / trackHeightPx
                                            val currentIdx = listState.firstVisibleItemIndex
                                            val changeIdx = ratio * totalItems
                                            val newIdx = (currentIdx + changeIdx).toInt().coerceIn(0, totalItems - 1)

                                            coroutineScope.launch {
                                                listState.scrollToItem(newIdx)
                                            }
                                        }
                                    }
                                }
                        ) {
                            val minThumbHeightPx = with(density) { 32.dp.toPx() }

                            val ratioVisible = visibleItems.toFloat() / totalItems.toFloat()
                            val thumbHeightPx = (trackHeightPx * ratioVisible).coerceAtLeast(minThumbHeightPx)

                            val scrollRange = totalItems - visibleItems
                            val scrollProgress = if (scrollRange > 0) listState.firstVisibleItemIndex.toFloat() / scrollRange.toFloat() else 0f
                            val trackScrollableArea = trackHeightPx - thumbHeightPx
                            val thumbOffsetPx = trackScrollableArea * scrollProgress

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .fillMaxWidth()
                                    .height(with(density) { thumbHeightPx.toDp() })
                                    .background(primaryColor, CircleShape)
                            )
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
        }
    }
}