package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.example.chess.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualBoardCorrectionSheet(
    initialFen: String,
    onDismiss: () -> Unit,
    onConfirmPosition: (String) -> Unit
) {
    var board by remember { mutableStateOf(Board.fromFen(initialFen)) }
    var selectedPieceType by remember { mutableStateOf<PieceType?>(null) }
    var selectedPieceColor by remember { mutableStateOf(PieceColor.WHITE) }
    var isEraserMode by remember { mutableStateOf(false) }

    val palettePieces = listOf(
        PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP,
        PieceType.ROOK, PieceType.QUEEN, PieceType.KING
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSlate,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CardBorderSlate)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("manual_correction_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BOARD CORRECTION & VERIFY",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_correction_sheet")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Text(
                text = "Tap a palette piece below, then tap any square to place or correct pieces.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mini board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, CardBorderSlate, RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (rank in 7 downTo 0) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            for (file in 0..7) {
                                val sq = Square.at(file, rank)
                                val piece = board.getPiece(sq)
                                val bg = if (sq.isLight) BoardLightSquare else BoardDarkSquare

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(bg)
                                        .clickable {
                                            val newSquares = board.squares.toMutableList()
                                            newSquares[sq.index] = if (isEraserMode) {
                                                null
                                            } else if (selectedPieceType != null) {
                                                Piece(selectedPieceType!!, selectedPieceColor)
                                            } else {
                                                piece // unchanged
                                            }
                                            board = board.copy(squares = newSquares)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (piece != null) {
                                        Text(
                                            text = piece.unicodeSymbol,
                                            fontSize = 20.sp,
                                            color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF1A1A1A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Palette Color Switcher (White vs Black)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    FilterChip(
                        selected = !isEraserMode && selectedPieceColor == PieceColor.WHITE,
                        onClick = {
                            isEraserMode = false
                            selectedPieceColor = PieceColor.WHITE
                            if (selectedPieceType == null) selectedPieceType = PieceType.PAWN
                        },
                        label = { Text("White Pieces", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalEmerald,
                            selectedLabelColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isEraserMode && selectedPieceColor == PieceColor.BLACK,
                        onClick = {
                            isEraserMode = false
                            selectedPieceColor = PieceColor.BLACK
                            if (selectedPieceType == null) selectedPieceType = PieceType.PAWN
                        },
                        label = { Text("Black Pieces", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                IconButton(
                    onClick = {
                        isEraserMode = true
                        selectedPieceType = null
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isEraserMode) TacticalRed else CardBorderSlate)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eraser Mode",
                        tint = if (isEraserMode) Color.White else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Piece Type Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(palettePieces) { pType ->
                    val isSelected = !isEraserMode && selectedPieceType == pType
                    val previewPiece = Piece(pType, selectedPieceColor)

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ObsidianBg else CardBorderSlate)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TacticalEmerald else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                isEraserMode = false
                                selectedPieceType = pType
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewPiece.unicodeSymbol,
                            fontSize = 22.sp,
                            color = if (selectedPieceColor == PieceColor.WHITE) Color.White else Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Button(
                onClick = {
                    onConfirmPosition(board.toFen())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_corrected_board_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply Board & Calculate",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
