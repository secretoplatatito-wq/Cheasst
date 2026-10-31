package com.example.chess.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.components.CandidateMovesCard
import com.example.chess.ui.components.EvaluationBar
import com.example.chess.ui.components.HardwareTelemetryCard
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: CheasstViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToModes: () -> Unit,
    onNavigateToMemory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val hardwareMetrics by viewModel.hardwareMetrics.collectAsState()
    val board by viewModel.board.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("home_screen")
    ) {
        // App Header with Logo & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Cheasst Logo",
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CHEASST",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Mobile AI Chess Assistant",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Connectivity Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, if (settings.isCloudAiEnabled) CyberCyan.copy(alpha = 0.5f) else TacticalEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (settings.isCloudAiEnabled) CyberCyan else TacticalEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (settings.isCloudAiEnabled) "HYBRID AI" else "OFFLINE LOCAL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Active Mode Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .clickable { onNavigateToModes() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE ENGINE PROFILE",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = settings.activeProfile.displayName,
                        color = TacticalEmerald,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings.activeProfile.subtitle,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Change Mode",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Hub Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Vision Camera",
                subtitle = "Scan physical board",
                icon = Icons.Default.CameraAlt,
                iconColor = TacticalEmerald,
                onClick = onNavigateToCamera,
                modifier = Modifier.weight(1f),
                testTag = "home_camera_tile"
            )
            ActionTile(
                title = "Analysis Board",
                subtitle = "Explore calculations",
                icon = Icons.Default.Analytics,
                iconColor = CyberCyan,
                onClick = onNavigateToAnalysis,
                modifier = Modifier.weight(1f),
                testTag = "home_analysis_tile"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "6 Engine Modes",
                subtitle = "Normal to CGM",
                icon = Icons.Default.Speed,
                iconColor = TacticalAmber,
                onClick = onNavigateToModes,
                modifier = Modifier.weight(1f),
                testTag = "home_modes_tile"
            )
            ActionTile(
                title = "Memory Bank",
                subtitle = "Saved games & notes",
                icon = Icons.Default.Bookmark,
                iconColor = Color(0xFFBA68C8),
                onClick = onNavigateToMemory,
                modifier = Modifier.weight(1f),
                testTag = "home_memory_tile"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Position Evaluation Snapshot
        Text(
            text = "LIVE POSITION STATUS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        EvaluationBar(analysisResult = analysisResult)

        Spacer(modifier = Modifier.height(12.dp))

        // Top Candidate Variations
        CandidateMovesCard(
            candidates = analysisResult?.candidateMoves ?: emptyList(),
            onMoveClicked = { viewModel.makeMove(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Telemetry
        HardwareTelemetryCard(
            metrics = hardwareMetrics,
            analysisResult = analysisResult
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSlate)
            .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag(testTag)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
