package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayChapter: (Int) -> Unit
) {
    val novel by viewModel.activeNovel.collectAsState()
    val chapters by viewModel.activeChapters.collectAsState()
    val isAscending by viewModel.isChapterSortAscending.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val displayChapters = remember(chapters, isAscending) { if (isAscending) chapters else chapters.sortedByDescending { it.index } }
    val bgColor = Color(0xFF111827); val surfaceColor = Color(0xFF1F2937); val textColor = Color(0xFFF9FAFB); val primaryColor = Color(0xFF3B82F6); val secondaryColor = Color(0xFF9CA3AF); val readColor = Color(0xFF6B7280)

    Scaffold(containerColor = bgColor, topBar = { TopAppBar(title = {}, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { padding ->
        if (novel == null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryColor) } } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.width(100.dp).aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).background(surfaceColor)) { if (novel!!.coverUrl.isNotEmpty()) AsyncImage(model = novel!!.coverUrl, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) { Text(text = novel!!.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 3, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.height(8.dp)); Text(text = novel!!.author.ifEmpty { "Unknown Author" }, fontSize = 14.sp, color = secondaryColor); Spacer(modifier = Modifier.height(4.dp)); Text(text = "${novel!!.totalChapters} Chapters", fontSize = 14.sp, color = secondaryColor) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { viewModel.playFromIndex(novel!!.currentChapterIndex, autoPlay = false); onPlayChapter(novel!!.currentChapterIndex) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Continue Reading") }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(surfaceColor).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Chapters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chapters.size < novel!!.totalChapters) {
                            TextButton(onClick = { viewModel.loadAllChapters() }, enabled = !isLoadingMore) {
                                Text(if (isLoadingMore) "Loading..." else "Try to load all", fontSize = 12.sp, color = primaryColor)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleChapterSort() }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = textColor)
                        }
                    }
                }

                LazyColumn {
                    if (!isAscending && chapters.size < novel!!.totalChapters) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(24.dp))
                                } else {
                                    Button(
                                        onClick = { viewModel.loadMoreChapters() },
                                        colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
                                    ) {
                                        Text("Load More Chapters", color = textColor)
                                    }
                                }
                            }
                        }
                    }

                    items(displayChapters) { chapter ->
                        val isRead = chapter.index < novel!!.currentChapterIndex; val isCurrent = chapter.index == novel!!.currentChapterIndex; val itemColor = when { isCurrent -> primaryColor; isRead -> readColor; else -> textColor }
                        Column(modifier = Modifier.fillMaxWidth().clickable { viewModel.playFromIndex(chapter.index, autoPlay = false); onPlayChapter(chapter.index) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(text = chapter.title, color = itemColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                if (chapter.isDownloaded) Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = primaryColor, modifier = Modifier.size(20.dp)) else IconButton(onClick = { viewModel.downloadSingleChapter(chapter) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Download, contentDescription = "Download", tint = secondaryColor) }
                            }
                        }
                        Divider(color = surfaceColor, thickness = 1.dp)
                    }

                    if (isAscending && chapters.size < novel!!.totalChapters) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(24.dp))
                                } else {
                                    Button(
                                        onClick = { viewModel.loadMoreChapters() },
                                        colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
                                    ) {
                                        Text("Load More Chapters", color = textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}