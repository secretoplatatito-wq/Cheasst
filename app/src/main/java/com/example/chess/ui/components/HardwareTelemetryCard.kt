package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.chess.engine.AnalysisResult
import com.example.chess.ui.HardwareMetrics
import com.example.ui.theme.*

@Composable
fun HardwareTelemetryCard(
    metrics: HardwareMetrics,
    analysisResult: AnalysisResult?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
            .background(ElevatedSlate)
            .padding(12.dp)
            .testTag("hardware_telemetry_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HARDWARE TELEMETRY & ENGINE STATS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "RAM Target: 3-4 GB",
                color = TacticalEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // RAM Bar
        val ramUsageFraction = (metrics.usedRamMb.toFloat() / metrics.maxRamMb.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "App RAM: ${metrics.usedRamMb} MB / ${metrics.maxRamMb} MB",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${(ramUsageFraction * 100).toInt()}% heap",
                    color = if (ramUsageFraction > 0.8f) TacticalAmber else TacticalEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { ramUsageFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = TacticalEmerald,
                trackColor = ObsidianBg
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill(
                label = "ENGINE SPEED",
                value = "${analysisResult?.nodesPerSecond ?: 0} nps"
            )
            MetricPill(
                label = "SEARCH DEPTH",
                value = "d${analysisResult?.depthReached ?: 0}"
            )
            MetricPill(
                label = "CPU THREADS",
                value = "${metrics.availableProcessors} cores (${metrics.cpuUtilizationPercent}%)"
            )
            MetricPill(
                label = "BATTERY",
                value = "${metrics.batteryPercent}%"
            )
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
