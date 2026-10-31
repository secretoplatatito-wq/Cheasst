package com.example.chess.model

data class Board(
    val squares: List<Piece?> = List(64) { null },
    val activeColor: PieceColor = PieceColor.WHITE,
    val whiteKingSideCastle: Boolean = true,
    val whiteQueenSideCastle: Boolean = true,
    val blackKingSideCastle: Boolean = true,
    val blackQueenSideCastle: Boolean = true,
    val enPassantSquare: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {
    fun getPiece(square: Square): Piece? = squares[square.index]
    fun getPiece(file: Int, rank: Int): Piece? = squares[rank * 8 + file]

    fun isInCheck(color: PieceColor): Boolean {
        val kingSquare = findKing(color) ?: return false
        return isSquareAttacked(kingSquare, color.opposite())
    }

    fun findKing(color: PieceColor): Square? {
        for (i in 0 until 64) {
            val p = squares[i]
            if (p != null && p.color == color && p.type == PieceType.KING) {
                return Square.fromIndex(i)
            }
        }
        return null
    }

    fun isSquareAttacked(target: Square, byColor: PieceColor): Boolean {
        // Attacked by Pawns
        val pawnPushes = if (byColor == PieceColor.WHITE) -1 else 1
        val pawnAttackRank = target.rank + pawnPushes
        if (pawnAttackRank in 0..7) {
            if (target.file > 0) {
                val p = getPiece(target.file - 1, pawnAttackRank)
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
            if (target.file < 7) {
                val p = getPiece(target.file + 1, pawnAttackRank)
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Attacked by Knights
        val knightOffsets = arrayOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for ((df, dr) in knightOffsets) {
            val f = target.file + df
            val r = target.rank + dr
            if (f in 0..7 && r in 0..7) {
                val p = getPiece(f, r)
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // Attacked by King
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val f = target.file + df
                val r = target.rank + dr
                if (f in 0..7 && r in 0..7) {
                    val p = getPiece(f, r)
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Orthogonal rays (Rook / Queen)
        val orthDirections = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        for ((df, dr) in orthDirections) {
            var f = target.file + df
            var r = target.rank + dr
            while (f in 0..7 && r in 0..7) {
                val p = getPiece(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += df
                r += dr
            }
        }

        // Diagonal rays (Bishop / Queen)
        val diagDirections = arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
        for ((df, dr) in diagDirections) {
            var f = target.file + df
            var r = target.rank + dr
            while (f in 0..7 && r in 0..7) {
                val p = getPiece(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += df
                r += dr
            }
        }

        return false
    }

    fun generateLegalMoves(): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(activeColor)
        val legalMoves = mutableListOf<Move>()
        for (move in pseudoMoves) {
            val newBoard = makeMove(move)
            if (!newBoard.isInCheck(activeColor)) {
                legalMoves.add(move)
            }
        }
        return legalMoves
    }

    private fun generatePseudoLegalMoves(color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val forwardDir = if (color == PieceColor.WHITE) 1 else -1
        val startRank = if (color == PieceColor.WHITE) 1 else 6
        val promoRank = if (color == PieceColor.WHITE) 7 else 0

        for (i in 0 until 64) {
            val piece = squares[i] ?: continue
            if (piece.color != color) continue
            val sq = Square.fromIndex(i)

            when (piece.type) {
                PieceType.PAWN -> {
                    // 1 square forward
                    val f1 = sq.file
                    val r1 = sq.rank + forwardDir
                    if (r1 in 0..7 && getPiece(f1, r1) == null) {
                        val toSq = Square.at(f1, r1)
                        if (r1 == promoRank) {
                            listOf(PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP).forEach { promo ->
                                moves.add(Move(sq, toSq, piece, promotion = promo))
                            }
                        } else {
                            moves.add(Move(sq, toSq, piece))
                            // 2 squares forward from start rank
                            if (sq.rank == startRank) {
                                val r2 = sq.rank + 2 * forwardDir
                                if (getPiece(f1, r2) == null) {
                                    moves.add(Move(sq, Square.at(f1, r2), piece))
                                }
                            }
                        }
                    }

                    // Captures
                    for (df in listOf(-1, 1)) {
                        val capF = sq.file + df
                        val capR = sq.rank + forwardDir
                        if (capF in 0..7 && capR in 0..7) {
                            val toSq = Square.at(capF, capR)
                            val targetPiece = getPiece(capF, capR)
                            if (targetPiece != null && targetPiece.color != color) {
                                if (capR == promoRank) {
                                    listOf(PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP).forEach { promo ->
                                        moves.add(Move(sq, toSq, piece, capturedPiece = targetPiece, promotion = promo))
                                    }
                                } else {
                                    moves.add(Move(sq, toSq, piece, capturedPiece = targetPiece))
                                }
                            } else if (enPassantSquare != null && enPassantSquare == toSq) {
                                val epCaptured = getPiece(capF, sq.rank)
                                moves.add(Move(sq, toSq, piece, capturedPiece = epCaptured, isEnPassant = true))
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    val knightOffsets = arrayOf(
                        Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                        Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                    )
                    for ((df, dr) in knightOffsets) {
                        val f = sq.file + df
                        val r = sq.rank + dr
                        if (f in 0..7 && r in 0..7) {
                            val target = getPiece(f, r)
                            if (target == null || target.color != color) {
                                moves.add(Move(sq, Square.at(f, r), piece, capturedPiece = target))
                            }
                        }
                    }
                }

                PieceType.BISHOP -> generateRayMoves(sq, piece, arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)), moves)
                PieceType.ROOK -> generateRayMoves(sq, piece, arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)), moves)
                PieceType.QUEEN -> generateRayMoves(sq, piece, arrayOf(
                    Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                ), moves)

                PieceType.KING -> {
                    for (df in -1..1) {
                        for (dr in -1..1) {
                            if (df == 0 && dr == 0) continue
                            val f = sq.file + df
                            val r = sq.rank + dr
                            if (f in 0..7 && r in 0..7) {
                                val target = getPiece(f, r)
                                if (target == null || target.color != color) {
                                    moves.add(Move(sq, Square.at(f, r), piece, capturedPiece = target))
                                }
                            }
                        }
                    }

                    // Castling
                    val kingRank = if (color == PieceColor.WHITE) 0 else 7
                    if (sq.rank == kingRank && sq.file == 4 && !isInCheck(color)) {
                        val kingSide = if (color == PieceColor.WHITE) whiteKingSideCastle else blackKingSideCastle
                        val queenSide = if (color == PieceColor.WHITE) whiteQueenSideCastle else blackQueenSideCastle

                        // King-side O-O
                        if (kingSide && getPiece(5, kingRank) == null && getPiece(6, kingRank) == null) {
                            if (!isSquareAttacked(Square.at(5, kingRank), color.opposite()) &&
                                !isSquareAttacked(Square.at(6, kingRank), color.opposite())
                            ) {
                                moves.add(Move(sq, Square.at(6, kingRank), piece, isCastling = true))
                            }
                        }

                        // Queen-side O-O-O
                        if (queenSide && getPiece(3, kingRank) == null && getPiece(2, kingRank) == null && getPiece(1, kingRank) == null) {
                            if (!isSquareAttacked(Square.at(3, kingRank), color.opposite()) &&
                                !isSquareAttacked(Square.at(2, kingRank), color.opposite())
                            ) {
                                moves.add(Move(sq, Square.at(2, kingRank), piece, isCastling = true))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun generateRayMoves(sq: Square, piece: Piece, directions: Array<Pair<Int, Int>>, moves: MutableList<Move>) {
        for ((df, dr) in directions) {
            var f = sq.file + df
            var r = sq.rank + dr
            while (f in 0..7 && r in 0..7) {
                val target = getPiece(f, r)
                if (target == null) {
                    moves.add(Move(sq, Square.at(f, r), piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(sq, Square.at(f, r), piece, capturedPiece = target))
                    }
                    break
                }
                f += df
                r += dr
            }
        }
    }

    fun makeMove(move: Move): Board {
        val newSquares = squares.toMutableList()
        var newWhiteKingSide = whiteKingSideCastle
        var newWhiteQueenSide = whiteQueenSideCastle
        var newBlackKingSide = blackKingSideCastle
        var newBlackQueenSide = blackQueenSideCastle
        var newEnPassant: Square? = null

        // Remove from start square
        newSquares[move.from.index] = null

        // Handle en passant capture
        if (move.isEnPassant) {
            val capRank = move.from.rank
            val capFile = move.to.file
            newSquares[capRank * 8 + capFile] = null
        }

        // Handle castling rook movement
        if (move.isCastling) {
            val r = move.from.rank
            if (move.to.file == 6) { // Kingside
                val rook = newSquares[r * 8 + 7]
                newSquares[r * 8 + 7] = null
                newSquares[r * 8 + 5] = rook
            } else if (move.to.file == 2) { // Queenside
                val rook = newSquares[r * 8 + 0]
                newSquares[r * 8 + 0] = null
                newSquares[r * 8 + 3] = rook
            }
        }

        // Place moved or promoted piece
        val finalPiece = if (move.promotion != null) Piece(move.promotion, move.piece.color) else move.piece
        newSquares[move.to.index] = finalPiece

        // Set en passant target if pawn moved 2 squares
        if (move.piece.type == PieceType.PAWN && kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            val epRank = (move.from.rank + move.to.rank) / 2
            newEnPassant = Square.at(move.from.file, epRank)
        }

        // Invalidate castling rights if King or Rooks move/are captured
        if (move.piece.type == PieceType.KING) {
            if (move.piece.color == PieceColor.WHITE) {
                newWhiteKingSide = false
                newWhiteQueenSide = false
            } else {
                newBlackKingSide = false
                newBlackQueenSide = false
            }
        }
        if (move.from == Square.at(0, 0) || move.to == Square.at(0, 0)) newWhiteQueenSide = false
        if (move.from == Square.at(7, 0) || move.to == Square.at(7, 0)) newWhiteKingSide = false
        if (move.from == Square.at(0, 7) || move.to == Square.at(0, 7)) newBlackQueenSide = false
        if (move.from == Square.at(7, 7) || move.to == Square.at(7, 7)) newBlackKingSide = false

        val nextColor = activeColor.opposite()
        val nextFullMove = if (activeColor == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber
        val nextHalfMove = if (move.piece.type == PieceType.PAWN || move.capturedPiece != null) 0 else halfMoveClock + 1

        return Board(
            squares = newSquares,
            activeColor = nextColor,
            whiteKingSideCastle = newWhiteKingSide,
            whiteQueenSideCastle = newWhiteQueenSide,
            blackKingSideCastle = newBlackKingSide,
            blackQueenSideCastle = newBlackQueenSide,
            enPassantSquare = newEnPassant,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove
        )
    }

    fun isCheckmate(): Boolean = isInCheck(activeColor) && generateLegalMoves().isEmpty()

    fun isStalemate(): Boolean = !isInCheck(activeColor) && generateLegalMoves().isEmpty()

    fun toFen(): String {
        val fen = StringBuilder()
        for (r in 7 downTo 0) {
            var emptyCount = 0
            for (f in 0..7) {
                val p = getPiece(f, r)
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount)
                        emptyCount = 0
                    }
                    fen.append(p.fenChar)
                }
            }
            if (emptyCount > 0) fen.append(emptyCount)
            if (r > 0) fen.append('/')
        }

        fen.append(' ')
        fen.append(if (activeColor == PieceColor.WHITE) 'w' else 'b')

        fen.append(' ')
        var castleStr = ""
        if (whiteKingSideCastle) castleStr += 'K'
        if (whiteQueenSideCastle) castleStr += 'Q'
        if (blackKingSideCastle) castleStr += 'k'
        if (blackQueenSideCastle) castleStr += 'q'
        fen.append(castleStr.ifEmpty { "-" })

        fen.append(' ')
        fen.append(enPassantSquare?.name ?: "-")

        fen.append(' ')
        fen.append(halfMoveClock)

        fen.append(' ')
        fen.append(fullMoveNumber)

        return fen.toString()
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String = STARTING_FEN): Board {
            val parts = fen.trim().split("\\s+".toRegex())
            val rows = parts[0].split('/')
            val squares = MutableList<Piece?>(64) { null }

            for (r in 0..7) {
                val fenRow = rows[7 - r]
                var f = 0
                for (c in fenRow) {
                    if (c.isDigit()) {
                        f += c.digitToInt()
                    } else {
                        val piece = Piece.fromFenChar(c)
                        if (piece != null && f in 0..7) {
                            squares[r * 8 + f] = piece
                        }
                        f++
                    }
                }
            }

            val activeColor = if (parts.size > 1 && parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
            val castle = if (parts.size > 2) parts[2] else "-"
            val wK = castle.contains('K')
            val wQ = castle.contains('Q')
            val bK = castle.contains('k')
            val bQ = castle.contains('q')

            val ep = if (parts.size > 3 && parts[3] != "-") Square.fromName(parts[3]) else null
            val halfMove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullMove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

            return Board(
                squares = squares,
                activeColor = activeColor,
                whiteKingSideCastle = wK,
                whiteQueenSideCastle = wQ,
                blackKingSideCastle = bK,
                blackQueenSideCastle = bQ,
                enPassantSquare = ep,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove
            )
        }
    }
}
