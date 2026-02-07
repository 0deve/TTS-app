package com.example.tts_app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val voicePitch by viewModel.voicePitch.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontColor by viewModel.fontColor.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lineHeight by viewModel.lineHeightMultiplier.collectAsState()
    val textMargin by viewModel.textMargin.collectAsState()
    val fontFamilyName by viewModel.fontFamilyName.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val isOled by viewModel.isOledMode.collectAsState()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.backupLibrary(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreLibrary(it) }
    }

    var testInput by remember { mutableStateOf("Hello, this is a test of the voice system.") }

    val bgColor = if (isOled) Color.Black else Color(0xFF111827)
    val cardColor = if (isOled) Color(0xFF121212) else Color(0xFF1F2937)
    val textColor = Color(0xFFF9FAFB)
    val secondaryColor = Color(0xFF9CA3AF)
    val primaryColor = Color(0xFF3B82F6)
    val errorColor = Color(0xFFEF4444)

    val colorOptions = listOf(
        0xFFF9FAFB,
        0xFF9CA3AF,
        0xFFEF4444,
        0xFFF59E0B,
        0xFF10B981,
        0xFF3B82F6,
        0xFF8B5CF6
    )

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
                .verticalScroll(rememberScrollState())
        ) {

            SectionHeader("APPEARANCE", secondaryColor)
            SettingsGroup(cardColor) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OLED Dark Mode", color = textColor, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isOled,
                            onCheckedChange = { viewModel.setOledMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                        )
                    }

                    Divider(color = bgColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

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
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Font Color", color = textColor, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(color), CircleShape)
                                    .clickable { viewModel.setFontColor(color) }
                                    .then(if (fontColor == color) Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape) else Modifier)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Line Height: ${String.format("%.1f", lineHeight)}", color = textColor)
                    Slider(
                        value = lineHeight,
                        onValueChange = { viewModel.setLineHeight(it) },
                        valueRange = 1.0f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Margin: ${textMargin}dp", color = textColor)
                    Slider(
                        value = textMargin.toFloat(),
                        onValueChange = { viewModel.setTextMargin(it.toInt()) },
                        valueRange = 0f..64f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Font Family", color = textColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Default", "Serif", "SansSerif", "Monospace").forEach { font ->
                            FilterChip(
                                selected = fontFamilyName == font,
                                onClick = { viewModel.setFontFamily(font) },
                                label = { Text(font) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White,
                                    labelColor = secondaryColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.\nThis is a second line to visualize line height and spacing settings.",
                        color = Color(fontColor),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        lineHeight = (fontSize * lineHeight).sp
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
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                    )
                }

                Divider(color = bgColor, thickness = 1.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Voice Selection", color = textColor, fontWeight = FontWeight.SemiBold)
                    if (availableVoices.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedVoice, color = textColor)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(cardColor).heightIn(max = 300.dp)
                            ) {
                                availableVoices.forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice, color = textColor) },
                                        onClick = {
                                            viewModel.setSelectedVoice(voice)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("No voices found or connection failed", color = errorColor, fontSize = 12.sp)
                    }
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
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Voice Pitch", color = textColor, fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.1f", voicePitch)}", color = secondaryColor)
                    }
                    Slider(
                        value = voicePitch,
                        onValueChange = { viewModel.setVoicePitch(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = primaryColor)
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

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("DATA MANAGEMENT", secondaryColor)
            SettingsGroup(cardColor) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { backupLauncher.launch("library_backup.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Backup Library")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Library")
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