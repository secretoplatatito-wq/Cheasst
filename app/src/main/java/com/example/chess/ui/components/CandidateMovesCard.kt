package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.CandidateMove
import com.example.chess.model.Move
import com.example.ui.theme.*

@Composable
fun CandidateMovesCard(
    candidates: List<CandidateMove>,
    onMoveClicked: (Move) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
            .background(ElevatedSlate)
            .padding(12.dp)
            .testTag("candidate_moves_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CANDIDATE VARIATIONS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${candidates.size} lines calculated",
                color = TacticalEmerald,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (candidates.isEmpty()) {
            Text(
                text = "Calculating optimal variations...",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            candidates.forEachIndexed { index, candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index == 0) ObsidianBg else Color.Transparent)
                        .clickable { onMoveClicked(candidate.move) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("candidate_move_$index"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge
                    val badgeColor = when (index) {
                        0 -> TacticalEmerald
                        1 -> CyberCyan
                        else -> TacticalAmber
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = candidate.badge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Move SAN
                    Text(
                        text = candidate.move.san,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Explanation
                    Text(
                        text = candidate.explanation,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Score
                    Text(
                        text = candidate.formattedScore,
                        color = if (candidate.scoreCp >= 0) TacticalEmerald else Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
