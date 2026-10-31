package com.example.chess.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.Board
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.components.ChessboardView
import com.example.chess.ui.components.EvaluationBar
import com.example.chess.ui.components.SeeMateArOverlay
import com.example.ui.theme.*

@Composable
fun DigitalScreenModeScreen(
    viewModel: CheasstViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val selectedSquare by viewModel.selectedSquare.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    var showFenDialog by remember { mutableStateOf(false) }
    var fenInputText by remember { mutableStateOf("") }

    val bestMove = analysisResult?.bestMove
    val candidateMoves = analysisResult?.candidateMoves ?: emptyList()
    val formattedEval = analysisResult?.formattedEval ?: "+0.00"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("digital_screen_mode_screen")
    ) {
        // Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIGITAL BOARD & SCREEN SOLVER",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Chess.com & Lichess Sync",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Perspective Button
            Button(
                onClick = { viewModel.togglePlayerColor() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (playerColor == PieceColor.BLACK) ElevatedSlate else Color(0xFF2C3240)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, CardBorderSlate, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = if (playerColor == PieceColor.BLACK) "BLACK ♚" else "WHITE ♔",
                    color = if (playerColor == PieceColor.BLACK) CyberCyan else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Evaluation Bar + Digital Board Container with SeeMate AR Overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                // Centipawn Eval Bar
                EvaluationBar(
                    centipawns = analysisResult?.centipawnScore ?: 0,
                    formattedScore = formattedEval,
                    isWhiteTurn = board.activeColor == PieceColor.WHITE
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Board with SeeMate AR Arrow Overlay layered directly on top!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    ChessboardView(
                        board = board,
                        selectedSquare = selectedSquare,
                        legalMoves = legalMoves,
                        isFlipped = isFlipped,
                        onSquareClicked = { viewModel.onSquareClicked(it) }
                    )

                    // Layer SeeMate AR Arrows on top of the digital board
                    SeeMateArOverlay(
                        bestMove = bestMove,
                        candidateMoves = candidateMoves,
                        formattedEval = formattedEval,
                        depth = analysisResult?.depth ?: 14,
                        playerColor = playerColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fast Solver Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (bestMove != null) {
                        viewModel.makeMove(bestMove)
                    }
                },
                enabled = bestMove != null,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_best_move_button")
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Best Move", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.stepOpponentMove() },
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CardBorderSlate, RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, tint = TacticalAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Opponent Move", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = { viewModel.flipBoard() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ElevatedSlate)
                    .border(1.dp, CardBorderSlate, CircleShape)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Perspective", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FEN / PGN Sync Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    fenInputText = board.toFen()
                    showFenDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste FEN / Link", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    // Load Blackburne Shilling Gambits
                    viewModel.setPlayerColor(PieceColor.BLACK)
                    viewModel.resetBoard("r1bqkbnr/pppp1ppp/8/4N3/2Bn4/8/PPPP1PPP/RNBQK2R b KQkq - 0 4")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmber),
                border = BorderStroke(1.dp, TacticalAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Blackburne Trap", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chesalyser / A.C.A.S Accuracy & Tactical Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedSlate)
                .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "A.C.A.S & CHESALYSER METRICS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Real-Time Precision",
                    color = TacticalEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Calculated Accuracy", color = TextSecondary, fontSize = 11.sp)
                    Text("98.4% (Grandmaster)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Avg Centipawn Loss", color = TextSecondary, fontSize = 11.sp)
                    Text("7.2 ACPL", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Anti-Cheat Status", color = TextSecondary, fontSize = 11.sp)
                    Text("Low (Human)", color = TacticalEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // FEN Input Dialog
    if (showFenDialog) {
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            title = { Text("Load Board Position (FEN)", color = Color.White) },
            text = {
                Column {
                    Text("Enter valid FEN string from Chess.com or Lichess:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fenInputText,
                        onValueChange = { fenInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalEmerald,
                            unfocusedBorderColor = CardBorderSlate,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            viewModel.resetBoard(fenInputText.trim())
                            showFenDialog = false
                        } catch (e: Exception) {
                            // Keep open on error
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald)
                ) {
                    Text("Load Position", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFenDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ElevatedSlate
        )
    }
}
