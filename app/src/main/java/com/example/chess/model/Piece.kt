package com.example.chess.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val char: Char, val value: Int) {
    PAWN('p', 100),
    KNIGHT('n', 320),
    BISHOP('b', 330),
    ROOK('r', 500),
    QUEEN('q', 900),
    KING('k', 20000);

    val uppercaseChar: Char get() = char.uppercaseChar()
}

data class Piece(val type: PieceType, val color: PieceColor) {
    val fenChar: Char get() = if (color == PieceColor.WHITE) type.uppercaseChar else type.char

    val unicodeSymbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.PAWN -> "♙"
                PieceType.KNIGHT -> "♘"
                PieceType.BISHOP -> "♗"
                PieceType.ROOK -> "♖"
                PieceType.QUEEN -> "♕"
                PieceType.KING -> "♔"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.PAWN -> "♟"
                PieceType.KNIGHT -> "♞"
                PieceType.BISHOP -> "♝"
                PieceType.ROOK -> "♜"
                PieceType.QUEEN -> "♛"
                PieceType.KING -> "♚"
            }
        }

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }
    }
}
