package com.example.tts_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isServerEnabled by viewModel.isServerTtsEnabled.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var testInput by remember { mutableStateOf("Hello, this is a test of the voice system.") }

    val bgColor = Color(0xFF111827)
    val cardColor = Color(0xFF1F2937)
    val textColor = Color(0xFFF9FAFB)
    val secondaryColor = Color(0xFF9CA3AF)
    val primaryColor = Color(0xFF3B82F6)
    val successColor = Color(0xFF10B981)
    val errorColor = Color(0xFFEF4444)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = textColor, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor),
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
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
        ) {

            SectionHeader("APPEARANCE", secondaryColor)
            SettingsGroup(cardColor) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", color = textColor, fontWeight = FontWeight.SemiBold)
                        Text("${fontSize.toInt()} sp", color = secondaryColor)
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 12f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = primaryColor
                        )
                    )
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.",
                        color = textColor,
                        fontSize = fontSize.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("READING PREFERENCES", secondaryColor)
            SettingsGroup(cardColor) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TTS Source", color = textColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isServerEnabled) "Server (AllTalk)" else "Local",
                            color = secondaryColor,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isServerEnabled,
                        onCheckedChange = { viewModel.setTtsMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor
                        )
                    )
                }

                if (isServerEnabled) {
                    Divider(color = bgColor, thickness = 1.dp)

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Server URL", color = textColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = serverIp,
                                onValueChange = { viewModel.updateServerIp(it) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = secondaryColor,
                                    cursorColor = primaryColor
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.testServerConnection() },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                modifier = Modifier.height(56.dp)
                            ) {
                                when (connectionState) {
                                    ConnectionState.Testing -> {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    }
                                    ConnectionState.Success -> {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                                    }
                                    ConnectionState.Failed -> {
                                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.White)
                                    }
                                    else -> {
                                        Text("Test")
                                    }
                                }
                            }
                        }

                        if (connectionState == ConnectionState.Success) {
                            Text("Connected successfully", color = successColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        } else if (connectionState == ConnectionState.Failed) {
                            Text("Connection failed", color = errorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Divider(color = bgColor, thickness = 1.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speaking Rate", color = textColor, fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.1f", ttsSpeed)}x", color = secondaryColor)
                    }

                    Slider(
                        value = ttsSpeed,
                        onValueChange = { viewModel.setTtsSpeed(it) },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = primaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("TEST AUDIO", secondaryColor)
            SettingsGroup(cardColor) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        label = { Text("Enter text to test", color = secondaryColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = secondaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.generateAudio(testInput) },
                        enabled = uiState !is UiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Play Test Audio")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsGroup(bgColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            content()
        }
    }
}