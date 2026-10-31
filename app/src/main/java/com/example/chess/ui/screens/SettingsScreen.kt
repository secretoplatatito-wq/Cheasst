package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ui.CheasstViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: CheasstViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val hardwareMetrics by viewModel.hardwareMetrics.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Text(
            text = "CONFIGURATION & HARDWARE",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Cheasst Settings",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Processing Configuration Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "AI PROCESSING MODES",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Local AI switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Local On-Device Engine", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Operates completely offline without external APIs", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isLocalAiEnabled,
                    onCheckedChange = { /* Always active as core */ },
                    enabled = false,
                    colors = SwitchDefaults.colors(
                        disabledCheckedThumbColor = TacticalEmerald,
                        disabledCheckedTrackColor = TacticalEmerald.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gemini Cloud AI switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Optional Gemini Cloud AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Enables Search Grounding & high-thinking strategic summaries", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isCloudAiEnabled,
                    onCheckedChange = { viewModel.toggleCloudAi(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyberCyan),
                    modifier = Modifier.testTag("cloud_ai_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Resource Allocation Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "HARDWARE RESOURCES & THREADING",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // CPU Threads
            Text(
                text = "CPU Thread Allocation: ${settings.cpuThreads} Threads",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.cpuThreads.toFloat(),
                onValueChange = { viewModel.setCpuThreads(it.toInt()) },
                valueRange = 1f..8f,
                steps = 6,
                colors = SliderDefaults.colors(thumbColor = TacticalEmerald, activeTrackColor = TacticalEmerald),
                modifier = Modifier.testTag("cpu_threads_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hash Memory
            Text(
                text = "Transposition Hash Memory: ${settings.hashMemoryMb} MB",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.hashMemoryMb.toFloat(),
                onValueChange = { viewModel.setHashMemory(it.toInt()) },
                valueRange = 16f..256f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                modifier = Modifier.testTag("hash_memory_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cheasst enforces a 3-4 GB RAM ceiling to guarantee fluid responsiveness across low and high-end devices.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Storage & Memory Cleanup Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "MEMORY CLEANUP & CACHE",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Temporary Calculation Cache: ${hardwareMetrics.tempCacheKb} KB",
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Persistent Stored Positions: ${hardwareMetrics.persistentCount} items",
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.clearTemporaryCache() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_temp_cache_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CardBorderSlate)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = TacticalAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Temporary Cache & Trim Memory", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy & Transparency Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Privacy",
                    tint = TacticalEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRIVACY & SECURITY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Camera video frames are analyzed locally on your device's CPU/GPU and are never uploaded or stored remotely.\n• All chess calculation trees and games remain strictly on your local database.\n• Force-stopping the application instantly clears all temporary RAM caches while preserving your saved games.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
