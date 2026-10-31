package com.example.chess.engine

import com.example.chess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

data class CandidateMove(
    val move: Move,
    val scoreCp: Int,
    val pvMoves: List<Move>,
    val badge: String,
    val explanation: String
) {
    val formattedScore: String
        get() {
            return if (kotlin.math.abs(scoreCp) > 15000) {
                val mateIn = (20000 - kotlin.math.abs(scoreCp) + 1) / 2
                if (scoreCp > 0) "+M$mateIn" else "-M$mateIn"
            } else {
                val pawns = scoreCp / 100.0
                if (pawns >= 0) "+%.2f".format(pawns) else "%.2f".format(pawns)
            }
        }
}

data class AnalysisResult(
    val bestMove: Move?,
    val evaluationCp: Int,
    val candidateMoves: List<CandidateMove>,
    val depthReached: Int,
    val nodesEvaluated: Long,
    val calculationTimeMs: Long,
    val nodesPerSecond: Long,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false
) {
    val formattedEval: String
        get() {
            if (isCheckmate) return if (evaluationCp > 0) "+Mate" else "-Mate"
            if (isStalemate) return "0.00 (Stalemate)"
            return if (kotlin.math.abs(evaluationCp) > 15000) {
                val mateIn = (20000 - kotlin.math.abs(evaluationCp) + 1) / 2
                if (evaluationCp > 0) "+M$mateIn" else "-M$mateIn"
            } else {
                val pawns = evaluationCp / 100.0
                if (pawns >= 0) "+%.2f".format(pawns) else "%.2f".format(pawns)
            }
        }

    val winProbabilityPercent: Int
        get() {
            // Sigmoid conversion from centipawns to win probability
            val p = 1.0 / (1.0 + Math.exp(-evaluationCp / 400.0))
            return (p * 100).toInt().coerceIn(1, 99)
        }
}

class ChessEngine {

    private var nodeCount: Long = 0
    private var startTimeMs: Long = 0
    private var maxTimeMs: Long = 2000L
    private val transpositionTable = HashMap<String, Pair<Int, Int>>(8192)

    // Piece-Square Tables for positional scoring
    private val PAWN_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_PST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_PST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val QUEEN_PST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_PST = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    suspend fun analyze(
        board: Board,
        settings: CalculationSettings
    ): AnalysisResult = withContext(Dispatchers.Default) {
        nodeCount = 0
        startTimeMs = System.currentTimeMillis()
        maxTimeMs = settings.effectiveTimeLimitMs
        transpositionTable.clear()

        val legalMoves = board.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            val isMate = board.isInCheck(board.activeColor)
            return@withContext AnalysisResult(
                bestMove = null,
                evaluationCp = if (isMate) -20000 else 0,
                candidateMoves = emptyList(),
                depthReached = 0,
                nodesEvaluated = 1,
                calculationTimeMs = 0,
                nodesPerSecond = 0,
                isCheckmate = isMate,
                isStalemate = !isMate
            )
        }

        val targetDepth = settings.effectiveDepth
        val multiPvCount = min(settings.effectiveMultiPv, legalMoves.size)

        // Order root moves by simple capture heuristic first
        val sortedMoves = orderMoves(board, legalMoves)

        val evaluatedCandidates = mutableListOf<Pair<Move, Int>>()

        // Iterative deepening up to targetDepth
        var currentDepth = 1
        var bestOverallMove = sortedMoves.first()
        var bestOverallScore = -30000

        while (currentDepth <= targetDepth) {
            coroutineContext.ensureActive()
            if (System.currentTimeMillis() - startTimeMs >= maxTimeMs && currentDepth > 3) {
                break
            }

            val depthResults = mutableListOf<Pair<Move, Int>>()
            val isWhite = board.activeColor == PieceColor.WHITE

            for (move in sortedMoves) {
                coroutineContext.ensureActive()
                val nextBoard = board.makeMove(move)
                val score = -alphaBeta(
                    board = nextBoard,
                    depth = currentDepth - 1,
                    alpha = -30000,
                    beta = 30000,
                    isMaximizing = false,
                    profile = settings.activeProfile
                )
                val scoreFromWhitePerspective = if (isWhite) score else -score
                depthResults.add(Pair(move, scoreFromWhitePerspective))
            }

            depthResults.sortByDescending { it.second * (if (isWhite) 1 else -1) }

            if (depthResults.isNotEmpty()) {
                bestOverallMove = depthResults.first().first
                bestOverallScore = depthResults.first().second
                evaluatedCandidates.clear()
                evaluatedCandidates.addAll(depthResults)
            }

            currentDepth++
        }

        val elapsedTimeMs = max(1L, System.currentTimeMillis() - startTimeMs)
        val nps = (nodeCount * 1000L) / elapsedTimeMs

        // Construct candidate moves
        val candidates = evaluatedCandidates.take(multiPvCount).mapIndexed { index, (move, score) ->
            val badge = when (index) {
                0 -> "Best Move"
                1 -> if (kotlin.math.abs(score - bestOverallScore) < 50) "Alternative" else "Good"
                2 -> "Solid"
                else -> "Playable"
            }
            val explanation = generateMoveExplanation(move, score, index, board)
            CandidateMove(
                move = move,
                scoreCp = score,
                pvMoves = listOf(move),
                badge = badge,
                explanation = explanation
            )
        }

        AnalysisResult(
            bestMove = bestOverallMove,
            evaluationCp = bestOverallScore,
            candidateMoves = candidates,
            depthReached = currentDepth - 1,
            nodesEvaluated = nodeCount,
            calculationTimeMs = elapsedTimeMs,
            nodesPerSecond = nps
        )
    }

    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        profile: EngineProfile
    ): Int {
        nodeCount++

        if (depth <= 0) {
            return quiescence(board, alpha, beta)
        }

        val legalMoves = board.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (board.isInCheck(board.activeColor)) -20000 - depth else 0
        }

        var currentAlpha = alpha
        val orderedMoves = orderMoves(board, legalMoves)

        for (move in orderedMoves) {
            val nextBoard = board.makeMove(move)
            val score = -alphaBeta(nextBoard, depth - 1, -beta, -currentAlpha, !isMaximizing, profile)
            if (score >= beta) return beta // Beta cutoff
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    private fun quiescence(board: Board, alpha: Int, beta: Int): Int {
        nodeCount++
        val standPat = evaluateStatic(board)
        if (standPat >= beta) return beta
        var currentAlpha = max(alpha, standPat)

        // Only search captures in quiescence
        val captures = board.generateLegalMoves().filter { it.capturedPiece != null || it.promotion != null }
        val orderedCaptures = orderMoves(board, captures)

        for (move in orderedCaptures) {
            val nextBoard = board.makeMove(move)
            val score = -quiescence(nextBoard, -beta, -currentAlpha)
            if (score >= beta) return beta
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    private fun orderMoves(board: Board, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) {
                // MVV-LVA: Most Valuable Victim - Least Valuable Attacker
                score += (move.capturedPiece.type.value * 10) - move.piece.type.value
            }
            if (move.promotion != null) {
                score += move.promotion.value
            }
            if (move.isCastling) score += 40
            score
        }
    }

    private fun evaluateStatic(board: Board): Int {
        var score = 0
        for (i in 0 until 64) {
            val piece = board.squares[i] ?: continue
            val sq = Square.fromIndex(i)
            val baseVal = piece.type.value

            val pstVal = when (piece.color) {
                PieceColor.WHITE -> getPstValue(piece.type, sq.file, 7 - sq.rank)
                PieceColor.BLACK -> getPstValue(piece.type, sq.file, sq.rank)
            }

            val totalPieceVal = baseVal + pstVal
            if (piece.color == PieceColor.WHITE) {
                score += totalPieceVal
            } else {
                score -= totalPieceVal
            }
        }

        return if (board.activeColor == PieceColor.WHITE) score else -score
    }

    private fun getPstValue(type: PieceType, file: Int, rank: Int): Int {
        val idx = rank * 8 + file
        return when (type) {
            PieceType.PAWN -> PAWN_PST[idx]
            PieceType.KNIGHT -> KNIGHT_PST[idx]
            PieceType.BISHOP -> BISHOP_PST[idx]
            PieceType.ROOK -> ROOK_PST[idx]
            PieceType.QUEEN -> QUEEN_PST[idx]
            PieceType.KING -> KING_PST[idx]
        }
    }

    private fun generateMoveExplanation(move: Move, score: Int, rankIndex: Int, board: Board): String {
        val dest = move.to.name
        val pieceName = when (move.piece.type) {
            PieceType.PAWN -> "Pawn"
            PieceType.KNIGHT -> "Knight"
            PieceType.BISHOP -> "Bishop"
            PieceType.ROOK -> "Rook"
            PieceType.QUEEN -> "Queen"
            PieceType.KING -> "King"
        }

        if (move.isCastling) {
            return "Secures king safety and connects rooks along the back rank."
        }
        if (move.capturedPiece != null) {
            val capName = when (move.capturedPiece.type) {
                PieceType.PAWN -> "pawn"
                PieceType.KNIGHT -> "knight"
                PieceType.BISHOP -> "bishop"
                PieceType.ROOK -> "rook"
                PieceType.QUEEN -> "queen"
                PieceType.KING -> "king"
            }
            return "$pieceName captures $capName on $dest with material gain."
        }
        if (move.piece.type == PieceType.PAWN && (dest == "e4" || dest == "d4" || dest == "e5" || dest == "d5")) {
            return "Claims central space and establishes pawn support."
        }
        if (move.piece.type == PieceType.KNIGHT && (dest == "f3" || dest == "c3" || dest == "f6" || dest == "c6")) {
            return "Develops knight to active central square."
        }
        return "Positions $pieceName on $dest to increase tactical mobility."
    }
}
