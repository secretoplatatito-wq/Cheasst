package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.example.chess.model.PieceColor
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.components.AiAdviceDialog
import com.example.chess.ui.components.CandidateMovesCard
import com.example.chess.ui.components.ChessboardView
import com.example.chess.ui.components.EvaluationBar
import com.example.ui.theme.*

@Composable
fun AnalysisScreen(
    viewModel: CheasstViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val selectedSquare by viewModel.selectedSquare.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var saveNotes by remember { mutableStateOf("") }
    var showAiSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("analysis_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CALCULATION ENGINE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${settings.activeProfile.displayName} Analysis",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Engine status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = TacticalEmerald
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calculating...",
                        color = TacticalEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "${analysisResult?.nodesPerSecond ?: 0} nps",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Evaluation Bar
        EvaluationBar(analysisResult = analysisResult)

        Spacer(modifier = Modifier.height(10.dp))

        // 8x8 Chessboard
        ChessboardView(
            board = board,
            selectedSquare = selectedSquare,
            legalMoves = legalMoves,
            isFlipped = isFlipped,
            lastMove = moveHistory.lastOrNull(),
            onSquareClicked = { viewModel.onSquareClicked(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Turn & Check Alert Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val turnText = if (board.activeColor == PieceColor.WHITE) "White to move" else "Black to move"
            Text(
                text = turnText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (board.isInCheck(board.activeColor)) {
                Text(
                    text = "CHECK!",
                    color = TacticalRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            } else if (analysisResult?.isCheckmate == true) {
                Text(
                    text = "CHECKMATE",
                    color = TacticalRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Board Action Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val playerColor by viewModel.playerColor.collectAsState()

            // Player Color Toggle
            Button(
                onClick = { viewModel.togglePlayerColor() },
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.border(1.dp, CardBorderSlate, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = if (playerColor == PieceColor.BLACK) "BLACK ♚" else "WHITE ♔",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.undoMove() },
                enabled = moveHistory.isNotEmpty(),
                modifier = Modifier.testTag("undo_move_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo Move",
                    tint = if (moveHistory.isNotEmpty()) Color.White else TextMuted
                )
            }
            IconButton(
                onClick = { viewModel.flipBoard() },
                modifier = Modifier.testTag("flip_board_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Flip Board",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { viewModel.resetBoard() },
                modifier = Modifier.testTag("reset_board_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Board",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.testTag("save_position_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save Position",
                    tint = TacticalEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Move History Strip
        if (moveHistory.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSlate)
                    .padding(8.dp)
            ) {
                Text(
                    text = "MOVE HISTORY",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(moveHistory) { index, move ->
                        val moveNum = (index / 2) + 1
                        val prefix = if (index % 2 == 0) "$moveNum. " else ""
                        Text(
                            text = "$prefix${move.san}",
                            color = if (index == moveHistory.lastIndex) TacticalEmerald else TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (index == moveHistory.lastIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Candidate Variations Card
        CandidateMovesCard(
            candidates = analysisResult?.candidateMoves ?: emptyList(),
            onMoveClicked = { viewModel.makeMove(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-Engine, Lc0 & ACAS Analytics Card
        val lc0 = analysisResult?.lc0Policy
        val acas = analysisResult?.acasMetrics

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
                    text = "ENGINE & ACCURACY TELEMETRY",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F2618))
                        .border(1.dp, TacticalEmerald, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = analysisResult?.engineSource ?: "Stockfish + Lc0",
                        color = TacticalEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chesalyser / A.C.A.S Accuracy metrics
            if (acas != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Move Accuracy", color = TextSecondary, fontSize = 10.sp)
                        Text("${"%.1f".format(acas.accuracyScore)}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Centipawn Loss", color = TextSecondary, fontSize = 10.sp)
                        Text("${acas.averageCentipawnLoss} ACPL", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Move Rating", color = TextSecondary, fontSize = 10.sp)
                        Text(acas.moveClassification, color = TacticalEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Lc0 Positional Policy Themes
            if (lc0 != null) {
                Text(
                    text = "Lc0 Positional Themes: ${lc0.positionalThemes.joinToString(", ")}",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Win: ${(lc0.winRate * 100).toInt()}%", color = TacticalEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Draw: ${(lc0.drawRate * 100).toInt()}%", color = TextSecondary, fontSize = 11.sp)
                    Text("Loss: ${(lc0.lossRate * 100).toInt()}%", color = Color(0xFFFF8A80), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Grandmaster Advisor Button
        Button(
            onClick = {
                viewModel.requestAiAdvice()
                showAiSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ask_ai_advisor_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Explain Strategy with AI",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // AI Strategic Advice Sheet
    if (showAiSheet) {
        AiAdviceDialog(
            advice = aiAdvice,
            isLoading = isAiThinking,
            onDismiss = { showAiSheet = false }
        )
    }

    // Save Position Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = ElevatedSlate,
            title = {
                Text("Save Position to Memory", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = { saveTitle = it },
                        label = { Text("Title (e.g. Italian Game Tactic)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalEmerald,
                            unfocusedBorderColor = CardBorderSlate,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_title_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        label = { Text("Personal Notes") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalEmerald,
                            unfocusedBorderColor = CardBorderSlate,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentPosition(saveTitle, saveNotes)
                        saveTitle = ""
                        saveNotes = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald),
                    modifier = Modifier.testTag("confirm_save_button")
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
