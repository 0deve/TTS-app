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
import androidx.compose.material.icons.filled.MoreVert
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
    val isOled by viewModel.isOledMode.collectAsState()

    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    val displayChapters = remember(chapters, isAscending) { if (isAscending) chapters else chapters.sortedByDescending { it.index } }
    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val surfaceColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val textColor = Color(0xFFF9FAFB)
    val primaryColor = Color(0xFF3B82F6)
    val secondaryColor = Color(0xFF9CA3AF)
    val readColor = Color(0xFF6B7280)

    if (showOptionsDialog && novel != null) {
        val downloadedNovels by viewModel.downloadedNovels.collectAsState()
        val isInstalled = downloadedNovels.any { it.id == novel!!.id }

        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = novel!!.title,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.removeFromLibrary(novel!!.id)
                            showOptionsDialog = false
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = "Remove from library",
                            color = Color(0xFFEF4444),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            showDownloadDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = "Download chapters",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.uninstallChapters(novel!!.id)
                            showOptionsDialog = false
                        },
                        enabled = isInstalled,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = "Uninstall chapters",
                            color = if (isInstalled) textColor else Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Close", color = secondaryColor)
                }
            }
        )
    }

    if (showDownloadDialog && novel != null) {
        DownloadDialog(
            novel = novel!!,
            onDismiss = { showDownloadDialog = false },
            onDownload = { limit, mode ->
                viewModel.downloadNovel(novel!!.id, limit, mode)
                showDownloadDialog = false
            }
        )
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showOptionsDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (novel == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.width(100.dp).aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).background(surfaceColor)) {
                        if (novel!!.coverUrl.isNotEmpty()) AsyncImage(model = novel!!.coverUrl, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(text = novel!!.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = novel!!.author.ifEmpty { "Unknown Author" }, fontSize = 14.sp, color = secondaryColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${novel!!.totalChapters} Chapters", fontSize = 14.sp, color = secondaryColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (novel!!.status.isNotEmpty() && novel!!.status != "Unknown") {
                            Text(text = "Status: ${novel!!.status}", fontSize = 14.sp, color = if (novel!!.status.equals("Ongoing", true)) primaryColor else Color.Green)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { viewModel.playFromIndex(novel!!.currentChapterIndex, autoPlay = false); onPlayChapter(novel!!.currentChapterIndex) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Continue Reading") }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(surfaceColor).padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Chapters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.loadAllChapters() }, enabled = !isLoadingMore, contentPadding = PaddingValues(0.dp)) {
                            Text(if (isLoadingMore) "Loading..." else "Try to load all", fontSize = 12.sp, color = primaryColor)
                        }
                        IconButton(onClick = { viewModel.toggleChapterSort() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = textColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Button(
                    onClick = { viewModel.reloadAllNovelChaptersContent(novel!!.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reload All Chapters", color = textColor)
                }

                LazyColumn {
                    if (!isAscending) {
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
                        val isRead = chapter.index < novel!!.currentChapterIndex
                        val isCurrent = chapter.index == novel!!.currentChapterIndex
                        val itemColor = when { isCurrent -> primaryColor; isRead -> readColor; else -> textColor }

                        Column(modifier = Modifier.fillMaxWidth().clickable { viewModel.playFromIndex(chapter.index, autoPlay = false); onPlayChapter(chapter.index) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(text = chapter.title, color = itemColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (chapter.releaseDate.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = chapter.releaseDate, color = secondaryColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (chapter.isDownloaded) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = primaryColor, modifier = Modifier.size(20.dp))
                                } else {
                                    IconButton(onClick = { viewModel.downloadSingleChapter(chapter) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = secondaryColor)
                                    }
                                }
                            }
                        }
                        Divider(color = surfaceColor, thickness = 1.dp)
                    }

                    if (isAscending) {
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