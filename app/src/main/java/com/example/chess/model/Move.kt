package com.example.chess.model

data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false
) {
    val uci: String
        get() = "${from.name}${to.name}${promotion?.char ?: ""}"

    val san: String
        get() {
            if (isCastling) {
                return if (to.file > from.file) "O-O" else "O-O-O"
            }
            val pieceChar = if (piece.type == PieceType.PAWN) "" else piece.type.uppercaseChar.toString()
            val captureChar = if (capturedPiece != null || isEnPassant) {
                if (piece.type == PieceType.PAWN) "${from.name[0]}x" else "x"
            } else ""
            val promo = if (promotion != null) "=${promotion.uppercaseChar}" else ""
            return "$pieceChar$captureChar${to.name}$promo"
        }
}
