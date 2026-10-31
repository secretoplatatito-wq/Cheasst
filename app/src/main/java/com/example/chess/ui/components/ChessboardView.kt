package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*
import com.example.ui.theme.*

@Composable
fun ChessboardView(
    board: Board,
    selectedSquare: Square?,
    legalMoves: List<Move>,
    isFlipped: Boolean = false,
    lastMove: Move? = null,
    onSquareClicked: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranks = if (isFlipped) 0..7 else 7 downTo 0
    val files = if (isFlipped) 7 downTo 0 else 0..7

    val inCheckKingSquare = if (board.isInCheck(board.activeColor)) {
        board.findKing(board.activeColor)
    } else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, CardBorderSlate, RoundedCornerShape(8.dp))
            .background(ElevatedSlate)
            .testTag("interactive_chessboard")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (rank in ranks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (file in files) {
                        val sq = Square.at(file, rank)
                        val piece = board.getPiece(sq)
                        val isSelected = selectedSquare == sq
                        val isLegalTarget = legalMoves.any { it.to == sq }
                        val isLastMove = lastMove != null && (lastMove.from == sq || lastMove.to == sq)
                        val isKingInCheck = inCheckKingSquare == sq

                        val bg = when {
                            isKingInCheck -> BoardCheckSquare
                            isSelected -> BoardSelectedSquare
                            isLastMove -> BoardLastMoveSquare
                            sq.isLight -> BoardLightSquare
                            else -> BoardDarkSquare
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(bg)
                                .clickable { onSquareClicked(sq) }
                                .testTag("square_${sq.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // File coordinate label on bottom rank
                            if (rank == (if (isFlipped) 7 else 0)) {
                                Text(
                                    text = ('a' + file).toString(),
                                    color = if (sq.isLight) BoardDarkSquare else BoardLightSquare,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 2.dp, bottom = 1.dp)
                                )
                            }
                            // Rank coordinate label on left file
                            if (file == (if (isFlipped) 7 else 0)) {
                                Text(
                                    text = (rank + 1).toString(),
                                    color = if (sq.isLight) BoardDarkSquare else BoardLightSquare,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 2.dp, top = 1.dp)
                                )
                            }

                            // Piece Symbol
                            if (piece != null) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 28.sp,
                                    color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Legal move destination marker
                            if (isLegalTarget) {
                                if (piece != null) {
                                    // Capture target ring
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.85f)
                                            .border(3.dp, TacticalEmerald, CircleShape)
                                    )
                                } else {
                                    // Empty square move dot
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(TacticalEmerald.copy(alpha = 0.85f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
