package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.tts_app.data.local.Novel

@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Novel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val isOled by viewModel.isOledMode.collectAsState()

    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val surfaceColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val textColor = Color(0xFFF9FAFB)
    val secondaryColor = Color(0xFF9CA3AF)
    val primaryColor = Color(0xFF3B82F6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Novels", color = secondaryColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = secondaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                isLoading = true
                viewModel.searchNovels(query) { fetched ->
                    results = fetched
                    isLoading = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Search", color = Color.White)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(results) { novel ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                        ) {
                            if (novel.coverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = novel.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = novel.title,
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (novel.status.isNotEmpty() && novel.status != "Unknown") {
                                Text(
                                    text = novel.status,
                                    color = if (novel.status.equals("Ongoing", true)) primaryColor else Color.Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (novel.inLibrary) {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                        disabledContentColor = secondaryColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("In Library", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.addToLibrary(novel)
                                        results = results.map {
                                            if (it.url == novel.url) it.copy(inLibrary = true) else it
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Add to Library", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}