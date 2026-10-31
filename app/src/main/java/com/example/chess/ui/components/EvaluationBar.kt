package com.example.chess.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.AnalysisResult
import com.example.ui.theme.CardBorderSlate
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.TacticalEmerald

@Composable
fun EvaluationBar(
    analysisResult: AnalysisResult?,
    modifier: Modifier = Modifier
) {
    val evalCp = analysisResult?.evaluationCp ?: 0
    val winProbability = analysisResult?.winProbabilityPercent ?: 50

    // Fraction of bar for white (0.0 to 1.0)
    val targetFraction = (winProbability / 100f).coerceIn(0.05f, 0.95f)
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "eval_anim")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CardBorderSlate, RoundedCornerShape(8.dp))
            .background(ElevatedSlate)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Win probability for White
        Text(
            text = "W: ${winProbability}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Animated Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF263238))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(Color.White)
            )
        }

        // Formatted score (e.g. +1.40 or -M2)
        Text(
            text = analysisResult?.formattedEval ?: "+0.00",
            color = if (evalCp >= 0) TacticalEmerald else Color(0xFFFF8A80),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun EvaluationBar(
    centipawns: Int,
    formattedScore: String,
    isWhiteTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val winProbability = (1.0 / (1.0 + Math.exp(-centipawns / 400.0)) * 100).toInt().coerceIn(1, 99)
    val targetFraction = (winProbability / 100f).coerceIn(0.05f, 0.95f)
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "eval_anim_direct")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CardBorderSlate, RoundedCornerShape(8.dp))
            .background(ElevatedSlate)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "W: ${winProbability}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF263238))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(Color.White)
            )
        }

        Text(
            text = formattedScore,
            color = if (centipawns >= 0) TacticalEmerald else Color(0xFFFF8A80),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
