package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.CandidateMove
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalEmerald
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * SeeMate AR Overlay Component (matching the reference HUD in SeeMate demo & Ray-Ban Meta glasses):
 * - Corner tracking brackets
 * - Smooth directional arrows with arrowheads (Primary: Vibrant Emerald, Candidate: Sleek Purple)
 * - Move target pill badge: [Nf6 +0.26]
 * - Ray-Ban Meta / AR HUD telemetry header: "AI 22 moves deep +0.26"
 */
@Composable
fun SeeMateArOverlay(
    bestMove: Move?,
    candidateMoves: List<CandidateMove>,
    formattedEval: String,
    depth: Int,
    playerColor: PieceColor,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {

        // 1. Ray-Ban Meta / SeeMate AR Header Status (Top-Right as in reference image)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glasses indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "👓 Ray-Ban Meta",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TacticalEmerald)
                    )
                }
            }

            // Depth & Eval badge: "AI 22 moves deep +0.26"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg.copy(alpha = 0.85f))
                    .border(1.dp, TacticalEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "AI ${depth.coerceAtLeast(14)} moves deep $formattedEval",
                    color = TacticalEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Top-Left SeeMate Brand Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBg.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberCyan)
                )
                Text(
                    text = "SeeMate",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 2. Real-Time AR Chessboard Canvas (Arrows & Brackets)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardWidth = size.width
            val boardHeight = size.height
            val squareWidth = boardWidth / 8f
            val squareHeight = boardHeight / 8f

            // Helper to get center Offset for any Square based on perspective
            fun getSquareCenter(square: Square): Offset {
                val col = if (playerColor == PieceColor.BLACK) 7 - square.file else square.file
                val row = if (playerColor == PieceColor.BLACK) square.rank else 7 - square.rank
                return Offset(
                    x = col * squareWidth + squareWidth / 2f,
                    y = row * squareHeight + squareHeight / 2f
                )
            }

            // Draw Corner Brackets (SeeMate green corner brackets)
            val bracketColor = TacticalEmerald
            val bracketLen = squareWidth * 0.55f
            val bracketStroke = 4.5f

            // Top-Left Bracket
            drawLine(bracketColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth = bracketStroke)
            drawLine(bracketColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth = bracketStroke)

            // Top-Right Bracket
            drawLine(bracketColor, Offset(boardWidth, 0f), Offset(boardWidth - bracketLen, 0f), strokeWidth = bracketStroke)
            drawLine(bracketColor, Offset(boardWidth, 0f), Offset(boardWidth, bracketLen), strokeWidth = bracketStroke)

            // Bottom-Right Bracket
            drawLine(bracketColor, Offset(boardWidth, boardHeight), Offset(boardWidth - bracketLen, boardHeight), strokeWidth = bracketStroke)
            drawLine(bracketColor, Offset(boardWidth, boardHeight), Offset(boardWidth, boardHeight - bracketLen), strokeWidth = bracketStroke)

            // Bottom-Left Bracket
            drawLine(bracketColor, Offset(0f, boardHeight), Offset(bracketLen, boardHeight), strokeWidth = bracketStroke)
            drawLine(bracketColor, Offset(0f, boardHeight), Offset(0f, boardHeight - bracketLen), strokeWidth = bracketStroke)

            // Draw Candidate Move Arrows (Purple / Cyan)
            val purpleArrowColor = Color(0xFFB388FF).copy(alpha = 0.8f)
            candidateMoves.drop(1).take(2).forEach { cand ->
                val fromCenter = getSquareCenter(cand.move.from)
                val toCenter = getSquareCenter(cand.move.to)
                drawArrow(
                    start = fromCenter,
                    end = toCenter,
                    color = purpleArrowColor,
                    strokeWidth = 5.dp.toPx(),
                    arrowHeadSize = 14.dp.toPx()
                )
            }

            // Draw Primary Best Move Arrow (Vibrant Tactical Emerald with Arrowhead)
            if (bestMove != null) {
                val fromCenter = getSquareCenter(bestMove.from)
                val toCenter = getSquareCenter(bestMove.to)

                // Highlight source square
                drawCircle(
                    color = TacticalEmerald.copy(alpha = 0.35f * pulseAlpha),
                    radius = squareWidth * 0.40f,
                    center = fromCenter
                )

                // Highlight target square box
                val targetCol = if (playerColor == PieceColor.BLACK) 7 - bestMove.to.file else bestMove.to.file
                val targetRow = if (playerColor == PieceColor.BLACK) bestMove.to.rank else 7 - bestMove.to.rank
                drawRect(
                    color = TacticalEmerald.copy(alpha = 0.30f * pulseAlpha),
                    topLeft = Offset(targetCol * squareWidth, targetRow * squareHeight),
                    size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                )

                drawRect(
                    color = TacticalEmerald.copy(alpha = pulseAlpha),
                    topLeft = Offset(targetCol * squareWidth, targetRow * squareHeight),
                    size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw thick directional arrow
                drawArrow(
                    start = fromCenter,
                    end = toCenter,
                    color = TacticalEmerald,
                    strokeWidth = 8.dp.toPx(),
                    arrowHeadSize = 22.dp.toPx()
                )
            }
        }

        // 3. Floating Target Pill Badge directly on the target square: "[Nf6 +0.26]"
        if (bestMove != null) {
            val toSquare = bestMove.to
            val sanMove = bestMove.san

            // Position pill using weight/offsets
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellW = maxWidth / 8f
                val cellH = maxHeight / 8f
                val col = if (playerColor == PieceColor.BLACK) 7 - toSquare.file else toSquare.file
                val row = if (playerColor == PieceColor.BLACK) toSquare.rank else 7 - toSquare.rank

                val pillOffsetX = (col * cellW.value) + (cellW.value * 0.1f)
                val pillOffsetY = ((row - 0.7f) * cellH.value).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .offset(x = pillOffsetX.dp, y = pillOffsetY.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D1E16).copy(alpha = 0.95f))
                        .border(1.5.dp, TacticalEmerald, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sanMove,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalEmerald)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = formattedEval,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a clean vector arrow from start to end with an equilateral triangle arrowhead.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    arrowHeadSize: Float
) {
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble()).toFloat()

    // Shorten end slightly so arrow head sits right on destination
    val adjustedEnd = Offset(
        x = end.x - (arrowHeadSize * 0.35f * cos(angle.toDouble())).toFloat(),
        y = end.y - (arrowHeadSize * 0.35f * sin(angle.toDouble())).toFloat()
    )

    // Arrow Shaft
    drawLine(
        color = color,
        start = start,
        end = adjustedEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Arrowhead Triangle
    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            (end.x - arrowHeadSize * cos((angle - Math.PI / 6.0))).toFloat(),
            (end.y - arrowHeadSize * sin((angle - Math.PI / 6.0))).toFloat()
        )
        lineTo(
            (end.x - arrowHeadSize * cos((angle + Math.PI / 6.0))).toFloat(),
            (end.y - arrowHeadSize * sin((angle + Math.PI / 6.0))).toFloat()
        )
        close()
    }
    drawPath(path = headPath, color = color)
}
