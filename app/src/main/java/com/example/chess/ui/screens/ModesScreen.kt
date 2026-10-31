package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.chess.model.Complexity
import com.example.chess.model.EngineProfile
import com.example.chess.ui.CheasstViewModel
import com.example.ui.theme.*

@Composable
fun ModesScreen(
    viewModel: CheasstViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("modes_screen")
    ) {
        // Header
        Text(
            text = "ENGINE PROFILES & MODES",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Select Calculation Strength",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Each profile reconfigures search depth, pruning heuristics, candidate lines, and tactical evaluation.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calculation Toggles Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "CALCULATION SPEED & COMPLEXITY TOGGLES",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fast Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Fast Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Reduces calculation time for quicker responses", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isFast,
                    onCheckedChange = { viewModel.toggleFast(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TacticalEmerald),
                    modifier = Modifier.testTag("fast_mode_toggle")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Super Fast Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Super Fast Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Aggressively prunes depth for instant moves", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isSuperFast,
                    onCheckedChange = { viewModel.toggleSuperFast(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyberCyan),
                    modifier = Modifier.testTag("super_fast_toggle")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Complexity Selector (Pro vs Complex)
            Text("Complexity Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = settings.complexity == Complexity.PRO,
                    onClick = { viewModel.setComplexity(Complexity.PRO) },
                    label = { Text("Pro (Practical Lines)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TacticalEmerald,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f).testTag("complexity_pro_chip")
                )
                FilterChip(
                    selected = settings.complexity == Complexity.COMPLEX,
                    onClick = { viewModel.setComplexity(Complexity.COMPLEX) },
                    label = { Text("Complex (Deep Lines)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f).testTag("complexity_complex_chip")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Effective Specs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Effective Depth: ${settings.effectiveDepth} plies",
                    color = TacticalEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Time Limit: ${settings.effectiveTimeLimitMs} ms",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6 Engine Profiles
        EngineProfile.ALL_PROFILES.forEach { profile ->
            val isSelected = settings.activeProfile.id == profile.id
            val accentColor = when (profile.id) {
                com.example.chess.model.ModeId.NORMAL -> Color(0xFF81C784)
                com.example.chess.model.ModeId.PRO -> TacticalEmerald
                com.example.chess.model.ModeId.PROFAST -> CyberCyan
                com.example.chess.model.ModeId.MASTER -> TacticalAmber
                com.example.chess.model.ModeId.GRANDMASTER -> Color(0xFFFF8A65)
                com.example.chess.model.ModeId.COMPLEX_GRANDMASTER -> Color(0xFFBA68C8)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSlate)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else CardBorderSlate,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setEngineProfile(profile) }
                    .padding(14.dp)
                    .testTag("mode_card_${profile.id.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName,
                                color = if (isSelected) accentColor else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Depth ${profile.baseDepth}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = profile.subtitle,
                            color = accentColor.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = profile.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
