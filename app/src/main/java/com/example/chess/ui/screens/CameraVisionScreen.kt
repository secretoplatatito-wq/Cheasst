package com.example.chess.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.chess.model.Board
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.components.AiAdviceDialog
import com.example.chess.ui.components.ManualBoardCorrectionSheet
import com.example.chess.ui.components.SeeMateArOverlay
import com.example.ui.theme.*

@Composable
fun CameraVisionScreen(
    viewModel: CheasstViewModel,
    onNavigateToAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val board by viewModel.board.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val visionResult by viewModel.visionResult.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var showCorrectionSheet by remember { mutableStateOf(false) }
    var showAiAdviceDialog by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val bestMove = analysisResult?.bestMove
    val candidateMoves = analysisResult?.candidateMoves ?: emptyList()
    val formattedEval = analysisResult?.formattedEval ?: "+0.00"
    val isUserTurn = board.activeColor == playerColor
    val logListState = rememberLazyListState()

    // Auto-scroll logs to bottom
    LaunchedEffect(consoleLogs.size) {
        if (consoleLogs.isNotEmpty()) {
            logListState.animateScrollToItem(consoleLogs.size - 1)
        }
    }

    // Auto-confirm detected opponent move when emitted by vision tracker
    LaunchedEffect(visionResult?.detectedMoveEvent) {
        val detected = visionResult?.detectedMoveEvent
        if (detected != null && board.activeColor != playerColor) {
            val legal = board.generateLegalMoves().find { it.from == detected.from && it.to == detected.to }
            if (legal != null) {
                viewModel.addLog("automated opponent move recognized: ${legal.san}")
                viewModel.makeMove(legal)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("camera_vision_screen")
    ) {
        // 1. Live CameraX Feed
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    previewViewRef = previewView
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            // Convert imageProxy bitmap if supported or process
                            val bitmap = previewView.bitmap
                            if (bitmap != null) {
                                viewModel.processCameraFrame(bitmap)
                            }
                            imageProxy.close()
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            // Handled gracefully in emulator
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Prompt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Access Required",
                        tint = TacticalEmerald,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cheasst uses the camera to auto-track physical chessboards, identify opponent moves in real-time, and project SeeMate AR arrows.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Enable Camera Access", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Real-Time SeeMate AR Spatial Chessboard Overlay (Canvas with arrows, brackets, [Nf6 +0.26] pill)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .padding(bottom = 50.dp)
        ) {
            SeeMateArOverlay(
                bestMove = bestMove,
                candidateMoves = candidateMoves,
                formattedEval = formattedEval,
                depth = analysisResult?.depth ?: 14,
                playerColor = playerColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Top HUD: Perspective, Turn status, Audio toggle, Gemini bridge
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Perspective Switcher
            Button(
                onClick = { viewModel.togglePlayerColor() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (playerColor == PieceColor.BLACK) ElevatedSlate else Color(0xFF2C3240)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.border(1.dp, CardBorderSlate, RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = if (playerColor == PieceColor.BLACK) "Play: BLACK ♚" else "Play: WHITE ♔",
                    color = if (playerColor == PieceColor.BLACK) CyberCyan else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Turn Indicator Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isUserTurn) TacticalEmerald.copy(alpha = 0.2f) else TacticalAmber.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (isUserTurn) TacticalEmerald else TacticalAmber,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isUserTurn) "● YOUR TURN" else "● OPPONENT'S TURN",
                    color = if (isUserTurn) TacticalEmerald else TacticalAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Icons: Sound toggle & Gemini AI Bridge
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sound Narration Toggle
                IconButton(
                    onClick = { viewModel.toggleSound() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle TTS Audio",
                        tint = if (isSoundEnabled) TacticalEmerald else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Direct Gemini Strategic Advisor Bridge
                IconButton(
                    onClick = {
                        viewModel.requestAiAdvice()
                        showAiAdviceDialog = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini Strategic Advisor",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Bottom Section: Monospace Terminal Console & Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hacker Terminal Console Box (matches the reverse.dev slide & video)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090D12).copy(alpha = 0.92f))
                    .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(consoleLogs) { log ->
                        Text(
                            text = log,
                            color = when {
                                log.contains("CHECKMATE") -> TacticalRed
                                log.contains("new move sent") -> TacticalEmerald
                                log.contains("query stockfish") -> CyberCyan
                                log.contains("automated opponent move") -> TacticalEmerald
                                log.contains("player move confirmed") -> Color(0xFF64B5F6)
                                else -> Color(0xFF90A4AE)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        viewModel.setPlayerColor(PieceColor.BLACK)
                        viewModel.resetBoard("r1bqkbnr/pppp1ppp/8/4N3/2Bn4/8/PPPP1PPP/RNBQK2R b KQkq - 0 4")
                        viewModel.addLog("loaded Blackburne Shilling Gambit position (PJ Chess demo)")
                    },
                    label = { Text("Blackburne Demo", fontSize = 10.sp, color = CyberCyan) },
                    modifier = Modifier.weight(1f)
                )

                SuggestionChip(
                    onClick = {
                        viewModel.setPlayerColor(PieceColor.BLACK)
                        viewModel.resetBoard(Board.STARTING_FEN)
                    },
                    label = { Text("Standard Black", fontSize = 10.sp, color = Color.White) },
                    modifier = Modifier.weight(1f)
                )

                SuggestionChip(
                    onClick = {
                        viewModel.setPlayerColor(PieceColor.WHITE)
                        viewModel.resetBoard(Board.STARTING_FEN)
                    },
                    label = { Text("Standard White", fontSize = 10.sp, color = Color.White) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Action HUD Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Board Edit / Correction
                IconButton(
                    onClick = { showCorrectionSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                        .testTag("manual_edit_board_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manual Board Correction",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Simulate / Step Opponent's Move
                Button(
                    onClick = { viewModel.stepOpponentMove() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, CardBorderSlate, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Opponent Moved",
                        tint = TacticalAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Opponent Move", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Confirm Player's Best Move (advances board)
                Button(
                    onClick = {
                        if (bestMove != null) {
                            viewModel.confirmPlayerMove(bestMove)
                        }
                    },
                    enabled = bestMove != null,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("confirm_player_move_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm Move",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Confirm Move",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Full Analysis Screen
                IconButton(
                    onClick = onNavigateToAnalysis,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                        .testTag("jump_to_analysis_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Deep Analysis",
                        tint = TacticalEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Manual Board Correction Bottom Sheet
        if (showCorrectionSheet) {
            val currentFen = board.toFen()
            ManualBoardCorrectionSheet(
                initialFen = currentFen,
                onDismiss = { showCorrectionSheet = false },
                onConfirmPosition = { correctedFen ->
                    viewModel.applyDetectedFen(correctedFen)
                    showCorrectionSheet = false
                }
            )
        }

        // Gemini Strategic Advisor Dialog
        if (showAiAdviceDialog) {
            AiAdviceDialog(
                advice = aiAdvice,
                isLoading = isAiThinking,
                onDismiss = { showAiAdviceDialog = false }
            )
        }
    }
}
