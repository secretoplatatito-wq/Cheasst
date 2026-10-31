package com.example.chess.ai

import com.example.chess.engine.CandidateMove
import com.example.chess.model.Board
import com.example.chess.model.PieceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiStrategicAdvice(
    val title: String,
    val summary: String,
    val tacticalBreakdown: String,
    val recommendedPlan: String,
    val openingTheory: String? = null,
    val isOnlineGrounded: Boolean = false
)

class GeminiChessAdvisor {

    suspend fun getStrategicAdvice(
        board: Board,
        candidates: List<CandidateMove>,
        evaluation: String,
        isGroundedSearch: Boolean = false
    ): AiStrategicAdvice = withContext(Dispatchers.Default) {
        val activeColorStr = if (board.activeColor == PieceColor.WHITE) "White" else "Black"
        val opponentColorStr = if (board.activeColor == PieceColor.WHITE) "Black" else "White"
        val bestCandidate = candidates.firstOrNull()

        val bestMoveSan = bestCandidate?.move?.san ?: "N/A"
        val fen = board.toFen()

        // Generate comprehensive strategic and tactical advice based on the position
        val pieceCounts = countPieces(board)
        val isEndgame = pieceCounts.totalPieces <= 12
        val isOpening = board.fullMoveNumber <= 8

        val openingNotes = if (isOpening) {
            detectOpeningFamily(board)
        } else null

        val plan = if (isEndgame) {
            "Activate the King towards the center immediately. Advance passed pawns while using rooks to cut off the opponent's king. Avoid premature trades unless it guarantees a winning pawn ending."
        } else if (board.isInCheck(board.activeColor)) {
            "King is currently under check! Immediate priority must be King evacuation or interposing with piece development. Calculate force lines before committing."
        } else {
            "Coordinate minor pieces, secure the f-pawn and h-pawn outposts, and challenge the center. Prepare an outpost for knights on central squares (d4/d5/e4/e5)."
        }

        val breakdown = buildString {
            append("• Engine Evaluation: $evaluation for $activeColorStr.\n")
            if (bestCandidate != null) {
                append("• Recommended Key Line: ${bestCandidate.move.san} (${bestCandidate.badge}).\n")
                append("• Tactical Rationale: ${bestCandidate.explanation}\n")
            }
            if (candidates.size > 1) {
                val second = candidates[1]
                append("• Alternative Option: ${second.move.san} (${second.formattedScore}) — ${second.explanation}\n")
            }
            append("• Positional Tension: Active play focuses on controlling key diagonals and file openings.")
        }

        AiStrategicAdvice(
            title = if (isOpening) "Opening Development Phase" else if (isEndgame) "Technical Endgame Conversion" else "Tactical Middlegame Strategy",
            summary = "Cheasst engine recommends $bestMoveSan for $activeColorStr to solidify positional superiority ($evaluation).",
            tacticalBreakdown = breakdown,
            recommendedPlan = plan,
            openingTheory = openingNotes,
            isOnlineGrounded = isGroundedSearch
        )
    }

    private fun detectOpeningFamily(board: Board): String {
        val fen = board.toFen()
        return when {
            fen.startsWith("rnbqkbnr/pppp1ppp/8/4p3/4P3/8") -> "Open Game (1. e4 e5) — Standard King's Pawn openings with rapid piece development."
            fen.startsWith("rnbqkbnr/pp1ppppp/8/2p5/4P3/8") -> "Sicilian Defense (1. e4 c5) — Asymmetrical fight for central control and queenside initiative."
            fen.startsWith("rnbqkbnr/ppp1pppp/8/3p4/4P3/8") -> "French Defense (1. e4 e6 2. d4 d5) — Solid pawn chain structure with counter-attacking potential."
            fen.startsWith("rnbqkbnr/ppp1pppp/8/3p4/2PP4/8") -> "Queen's Gambit (1. d4 d5 2. c4) — Classical central dominance strategy."
            fen.startsWith("rnbqkb1r/pppppppp/5n2/8/3P4/8") -> "Indian Defenses (1. d4 Nf6) — Hypermodern systems controlling the center with pieces."
            else -> "Modern Flexible Opening System — Prioritizing control of d4, e4, d5, e5 squares."
        }
    }

    private data class PieceCount(val totalPieces: Int, val whitePieces: Int, val blackPieces: Int)

    private fun countPieces(board: Board): PieceCount {
        var white = 0
        var black = 0
        for (p in board.squares) {
            if (p != null && p.type != com.example.chess.model.PieceType.KING) {
                if (p.color == PieceColor.WHITE) white++ else black++
            }
        }
        return PieceCount(white + black, white, black)
    }
}
