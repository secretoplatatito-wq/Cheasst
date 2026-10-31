package com.example.chess.engine

import com.example.chess.model.Move
import com.example.chess.model.Square

/**
 * Opening Book for instant, zero-latency grandmaster moves and tactical traps.
 * Supports the Blackburne Shilling Gambit (shown in PJ Chess demo), Italian Game,
 * Sicilian Defense, French Defense, and standard responses.
 */
object OpeningBook {

    data class BookEntry(
        val fromSquare: String,
        val toSquare: String,
        val evaluationCp: Int,
        val badge: String,
        val explanation: String
    )

    // Keyed by position FEN without halfmove / fullmove numbers (first 4 components)
    private val BOOK_MAP = mapOf(
        // Starting Position (1. e4 or 1. d4)
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -" to BookEntry(
            "e2", "e4", 30, "Main Line", "King's Pawn opening: stakes immediate central control."
        ),

        // 1. e4 -> 1... e5
        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3" to BookEntry(
            "e7", "e5", -10, "Open Game", "Classical open game symmetry, countering White's central claim."
        ),

        // 1. e4 e5 2. Nf3 -> 2... Nc6
        "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -" to BookEntry(
            "b8", "c6", -15, "Book Move", "Develops knight to defend the e5 pawn and challenge d4."
        ),

        // 1. e4 e5 2. Nf3 Nc6 3. Bc4 (Italian Game) -> 3... Nd4 (Blackburne Shilling Gambit!)
        "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq -" to BookEntry(
            "c6", "d4", 50, "Blackburne Shilling", "Sets a devastating tactical trap enticing White's knight to Nxe5."
        ),

        // 3... Nd4 4. Nxe5? (White falls into the trap!) -> 4... Qg5!
        "r1bqkbnr/pppp1ppp/8/4N3/2Bn4/8/PPPP1PPP/RNBQK2R b KQkq -" to BookEntry(
            "d8", "g5", 420, "Tactical Refutation", "Attacks the e5 knight and threatens the undefended g2 pawn."
        ),

        // 4... Qg5 5. Nxf7? (White forks Queen & Rook) -> 5... Qxg2!
        "r1b1kbnr/pppp1Npp/8/6q1/2Bn4/8/PPPP1PPP/RNBQK2R b KQkq -" to BookEntry(
            "g5", "g2", 650, "Winning Counter", "Devastating strike threatening the h1 rook and unavoidable mating nets."
        ),

        // 5... Qxg2 6. Rf1 -> 6... Qxe4+!
        "r1b1kbnr/pppp1Npp/8/8/2Bn4/8/PPPP1PqP/RNBQKR2 b Qkq - 1 6" to BookEntry(
            "g2", "e4", 950, "Decisive Check", "Decisive central check forcing White to block with the bishop."
        ),

        // 6... Qxe4+ 7. Be2 -> 7... Nf3# (Checkmate!)
        "r1b1k1nr/pppp1Npp/8/8/3nq3/8/PPPPBP1P/RNBQKR2 b Qkq -" to BookEntry(
            "d4", "f3", 20000, "Smothered Mate", "Knight to f3 delivers an unstoppable checkmate victory!"
        )
    )

    fun lookup(fen: String): BookEntry? {
        val parts = fen.split(" ")
        if (parts.size >= 4) {
            val key = "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]}"
            return BOOK_MAP[key]
        }
        return BOOK_MAP[fen]
    }
}
