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

    // Pulsing animation for AR highlight
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val bestMove = analysisResult?.bestMove
    val isUserTurn = board.activeColor == playerColor
    val logListState = rememberLazyListState()

    // Auto-scroll logs to bottom
    LaunchedEffect(consoleLogs.size) {
        if (consoleLogs.isNotEmpty()) {
            logListState.animateScrollToItem(consoleLogs.size - 1)
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
                            // Handled gracefully in container/emulator
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
                        text = "Cheasst uses the camera to recognize physical chessboards and calculate moves in real-time.",
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

        // 2. Real-Time AR Spatial Chessboard Overlay (PJ Chess style)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .padding(bottom = 60.dp)
        ) {
            // Glowing Cyan Outer Reticle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, CyberCyan.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            )

            // Dynamic Coordinate Labels along the perimeter based on player perspective
            val filesList = if (playerColor == PieceColor.BLACK) {
                listOf("h", "g", "f", "e", "d", "c", "b", "a")
            } else {
                listOf("a", "b", "c", "d", "e", "f", "g", "h")
            }

            val ranksList = if (playerColor == PieceColor.BLACK) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8")
            } else {
                listOf("8", "7", "6", "5", "4", "3", "2", "1")
            }

            // Top File Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                filesList.forEach { f ->
                    Text(
                        text = f,
                        color = CyberCyan.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Left Rank Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                ranksList.forEach { r ->
                    Text(
                        text = r,
                        color = CyberCyan.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 8x8 Grid overlay
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                repeat(8) { displayRow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        repeat(8) { displayCol ->
                            // Map display grid cell to chess square
                            val actualFile = if (playerColor == PieceColor.BLACK) 7 - displayCol else displayCol
                            val actualRank = if (playerColor == PieceColor.BLACK) displayRow else 7 - displayRow
                            val currentSquare = Square.at(actualFile, actualRank)

                            val isSource = bestMove != null && bestMove.from == currentSquare
                            val isTarget = bestMove != null && bestMove.to == currentSquare

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.White.copy(alpha = 0.07f))
                                    .then(
                                        if (isSource) {
                                            Modifier
                                                .background(TacticalEmerald.copy(alpha = pulseAlpha * 0.45f))
                                                .border(2.dp, TacticalEmerald, RoundedCornerShape(4.dp))
                                        } else if (isTarget) {
                                            Modifier
                                                .background(TacticalEmerald.copy(alpha = pulseAlpha * 0.55f))
                                                .border(2.dp, TacticalEmerald, RoundedCornerShape(4.dp))
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTarget) {
                                    // Target Bullseye ring
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(TacticalEmerald)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating AR Move Badge
            if (bestMove != null) {
                val pieceName = bestMove.piece.type.name
                val fromName = bestMove.from.name
                val toName = bestMove.to.name
                val eval = analysisResult?.formattedEval ?: ""

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-36).dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianBg.copy(alpha = 0.92f))
                        .border(1.5.dp, TacticalEmerald, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TacticalEmerald)
                        )
                        Text(
                            text = "BEST MOVE:",
                            color = TacticalEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$pieceName $fromName -> $toName",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (eval.isNotBlank()) {
                            Text(
                                text = "($eval)",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
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

        // 4. Bottom Section: Monospace Terminal Console & Quick Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hacker Terminal Console Box (matches the video)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Fast Demo Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Blackburne Shilling Demo Preset (from the video!)
                SuggestionChip(
                    onClick = {
                        // 1. e4 e5 2. Nf3 Nc6 3. Bc4 Nd4 4. Nxe5
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

                // Simulate Opponent's Move (allows real-time turn stepping without physical hands)
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
