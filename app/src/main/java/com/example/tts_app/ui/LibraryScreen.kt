package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tts_app.data.local.Novel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onBookSelected: (Int) -> Unit,
    onNavigateToBrowse: () -> Unit
) {
    val novels by viewModel.libraryNovels.collectAsState()
    val downloadedNovels by viewModel.downloadedNovels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Recent") }

    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedNovelForDownload by remember { mutableStateOf<Novel?>(null) }

    val filteredNovels = remember(novels, downloadedNovels, searchQuery, selectedTab) {
        var result = if (selectedTab == "Downloaded") downloadedNovels else novels

        if (searchQuery.isNotEmpty()) {
            result = result.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        if (selectedTab == "All") {
            result = result.sortedBy { it.title }
        }
        result
    }

    val bgColor = Color(0xFF111827)
    val surfaceColor = Color(0xFF1F2937)
    val primaryColor = Color(0xFF3B82F6)
    val textColor = Color(0xFFF9FAFB)
    val secondaryTextColor = Color(0xFF9CA3AF)

    if (showDownloadDialog && selectedNovelForDownload != null) {
        DownloadDialog(
            novel = selectedNovelForDownload!!,
            onDismiss = { showDownloadDialog = false },
            onDownload = { limit, mode ->
                viewModel.downloadNovel(selectedNovelForDownload!!.id, limit, mode)
                showDownloadDialog = false
            }
        )
    }

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            Column {
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = downloadProgress!!,
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryColor,
                        trackColor = surfaceColor
                    )
                }
                NavigationBar(
                    containerColor = surfaceColor,
                    contentColor = textColor
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Home, contentDescription = "library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = primaryColor, selectedTextColor = primaryColor, indicatorColor = surfaceColor)
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToSettings,
                        icon = { Icon(Icons.Default.Settings, contentDescription = "settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = secondaryTextColor, unselectedTextColor = secondaryTextColor, indicatorColor = surfaceColor)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBrowse,
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("My Library", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Surface(modifier = Modifier.fillMaxWidth().height(50.dp), color = surfaceColor, shape = RoundedCornerShape(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "search", tint = secondaryTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true, cursorBrush = SolidColor(primaryColor), modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(8.dp)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TabButton("Recent", selectedTab == "Recent", primaryColor, textColor, surfaceColor) { selectedTab = "Recent" }
                TabButton("All", selectedTab == "All", primaryColor, textColor, surfaceColor) { selectedTab = "All" }
                TabButton("Downloaded", selectedTab == "Downloaded", primaryColor, textColor, surfaceColor) { selectedTab = "Downloaded" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredNovels) { novel ->
                    NovelCardItem(
                        novel = novel,
                        onClick = {
                            viewModel.openNovelDetails(novel.id, filterDownloaded = (selectedTab == "Downloaded"))
                            onBookSelected(novel.id)
                        },
                        onOptionClick = {
                            selectedNovelForDownload = novel
                            showDownloadDialog = true
                        },
                        surfaceColor = surfaceColor, textColor = textColor, primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadDialog(
    novel: Novel,
    onDismiss: () -> Unit,
    onDownload: (Int, String) -> Unit
) {
    var customAmount by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download ${novel.title}") },
        text = {
            Column {
                Text("Select download option:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) customAmount = it },
                    label = { Text("Number of chapters") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDownload(-1, "all") }) { Text("All Chapters") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val amt = customAmount.toIntOrNull() ?: 5
                    onDownload(amt, "unread")
                }) { Text("Next $customAmount Unread") }
            }
        }
    )
}

@Composable
fun NovelCardItem(
    novel: Novel, onClick: () -> Unit, onOptionClick: () -> Unit, surfaceColor: Color, textColor: Color, primaryColor: Color
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).background(surfaceColor)) {
            if (novel.coverUrl.isNotEmpty()) AsyncImage(model = novel.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { onOptionClick() }, modifier = Modifier.padding(4.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            }
            if (novel.coverUrl.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF4B5563), surfaceColor))), contentAlignment = Alignment.Center) {
                    Text(text = novel.title, modifier = Modifier.padding(8.dp), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = novel.title, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "${novel.totalChapters} Ch", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RowScope.TabButton(
    text: String, isSelected: Boolean, activeColor: Color, textColor: Color, bgColor: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF111827) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (isSelected) textColor else Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}