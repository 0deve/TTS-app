package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tts_app.data.local.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onBookSelected: (Int) -> Unit
) {
    val books by viewModel.books.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Recent") }

    val filteredBooks = remember(books, searchQuery, selectedTab) {
        var result = books

        if (searchQuery.isNotEmpty()) {
            result = result.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedTab == "All") {
            result = result.sortedBy { it.title }
        } else {
            result
        }
        result
    }

    val bgColor = Color(0xFF111827)
    val surfaceColor = Color(0xFF1F2937)
    val primaryColor = Color(0xFF3B82F6)
    val textColor = Color(0xFFF9FAFB)
    val secondaryTextColor = Color(0xFF9CA3AF)

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                contentColor = textColor
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "library") },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = surfaceColor
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = secondaryTextColor,
                        unselectedTextColor = secondaryTextColor,
                        indicatorColor = surfaceColor
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val baseText = "The quick brown fox jumps over the lazy dog. Amidst the whispering woods, the silver stream hummed a quiet lullaby."
                    val demoContent = (1..5).joinToString(separator = "\n\n") { baseText }
                    viewModel.importBookMock("New Book ${books.size + 1}", demoContent)
                },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "My Library",
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "search", tint = secondaryTextColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search title...", color = secondaryTextColor)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton("Recent", selectedTab == "Recent", primaryColor, textColor, surfaceColor) {
                    selectedTab = "Recent"
                }
                TabButton("All", selectedTab == "All", primaryColor, textColor, surfaceColor) {
                    selectedTab = "All"
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredBooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (books.isEmpty()) "No books. Tap + to add." else "No matches found.",
                        color = secondaryTextColor
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBooks) { book ->
                        BookCardItem(
                            book = book,
                            onClick = { onBookSelected(book.id) },
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TabButton(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    textColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) Color(0xFF111827) else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) textColor else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun BookCardItem(
    book: Book,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4B5563), surfaceColor)
                    )
                )
        ) {
            Text(
                text = book.title,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = book.title,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        val progressPercent = if (book.totalChapters > 0) {
            (((book.currentChapterIndex + 1).toFloat() / book.totalChapters) * 100).toInt()
        } else 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${book.currentChapterIndex + 1}/${book.totalChapters} Ch",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$progressPercent%",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}