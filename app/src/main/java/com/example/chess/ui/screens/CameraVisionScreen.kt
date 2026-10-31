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
import androidx.compose.foundation.layout.*
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
import com.example.chess.ui.CheasstViewModel
import com.example.chess.ui.components.ManualBoardCorrectionSheet
import com.example.chess.vision.VisionDetectionResult
import com.example.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun CameraVisionScreen(
    viewModel: CheasstViewModel,
    onNavigateToAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var currentPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("camera_vision_screen")
    ) {
        if (hasCameraPermission) {
            // Live CameraX View
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
                        contentDescription = "Camera Required",
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

        // Viewfinder Alignment Bounding Box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .border(2.dp, TacticalEmerald.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
        ) {
            // Corner Reticles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // 8x8 Grid watermark lines
                Column(modifier = Modifier.fillMaxSize()) {
                    repeat(8) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            repeat(8) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, Color.White.copy(alpha = 0.08f))
                                )
                            }
                        }
                    }
                }

                // Laser scan line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.02f)
                        .offset(y = 280.dp * laserY)
                        .background(TacticalEmerald.copy(alpha = 0.7f))
                )
            }
        }

        // Top Status HUD
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ElevatedSlate.copy(alpha = 0.9f))
                .border(1.dp, CardBorderSlate, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (visionResult?.isBoardDetected == true) TacticalEmerald else TacticalAmber)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = visionResult?.statusMessage ?: "Align physical board inside reticle",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Controls HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick preset position loader for emulator / testing
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSlate.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = {
                        viewModel.applyDetectedFen(Board.STARTING_FEN)
                        showCorrectionSheet = true
                    }
                ) {
                    Text("Standard FEN", color = CyberCyan, fontSize = 11.sp)
                }
                TextButton(
                    onClick = {
                        viewModel.applyDetectedFen("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 5")
                        showCorrectionSheet = true
                    }
                ) {
                    Text("Giuoco Piano", color = CyberCyan, fontSize = 11.sp)
                }
                TextButton(
                    onClick = {
                        viewModel.applyDetectedFen("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2")
                        showCorrectionSheet = true
                    }
                ) {
                    Text("Sicilian", color = CyberCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Capture & Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Edit Button
                IconButton(
                    onClick = { showCorrectionSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                        .testTag("manual_edit_board_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manual Board Correction",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Button(
                    onClick = {
                        // Capture bitmap from preview or use synthetic frame
                        val bitmap = previewViewRef?.bitmap ?: Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                        viewModel.processCameraFrame(bitmap)
                        showCorrectionSheet = true
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .testTag("capture_chessboard_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Capture Chessboard",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Go to Analysis Button
                IconButton(
                    onClick = onNavigateToAnalysis,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, CardBorderSlate, CircleShape)
                        .testTag("jump_to_analysis_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Analyze Position",
                        tint = TacticalEmerald
                    )
                }
            }
        }

        // Manual Board Correction Bottom Sheet
        if (showCorrectionSheet) {
            val currentFen = visionResult?.detectedFen ?: viewModel.board.collectAsState().value.toFen()
            ManualBoardCorrectionSheet(
                initialFen = currentFen,
                onDismiss = { showCorrectionSheet = false },
                onConfirmPosition = { correctedFen ->
                    viewModel.applyDetectedFen(correctedFen)
                    showCorrectionSheet = false
                    onNavigateToAnalysis()
                }
            )
        }
    }
}
