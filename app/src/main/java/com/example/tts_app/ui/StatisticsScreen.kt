package com.example.tts_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.statistics.collectAsState()
    val isOled by viewModel.isOledMode.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val cardColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val textColor = Color(0xFFF9FAFB)
    val primaryColor = Color(0xFF3B82F6)
    val secondaryColor = Color(0xFF9CA3AF)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics", color = textColor, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard("Total Novels", stats.totalNovels.toString(), cardColor, textColor, primaryColor)
            StatCard("Read Chapters", stats.totalChaptersRead.toString(), cardColor, textColor, Color(0xFF10B981))
            StatCard("Downloaded Chapters", stats.totalDownloadedChapters.toString(), cardColor, textColor, Color(0xFFF59E0B))
            StatCard("Unread Chapters", stats.totalUnreadChapters.toString(), cardColor, textColor, Color(0xFFEF4444))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, bgColor: Color, textColor: Color, accentColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 16.sp, color = textColor)
        }
    }
}