package com.example

import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChessEngineUnitTest {

    @Test
    fun testStartingPositionFen() {
        val board = Board.fromFen()
        assertEquals(Board.STARTING_FEN, board.toFen())
        assertEquals(PieceColor.WHITE, board.activeColor)
    }

    @Test
    fun testInitialLegalMovesCount() {
        val board = Board.fromFen()
        val legalMoves = board.generateLegalMoves()
        // Standard chess has exactly 20 opening moves (16 pawn moves + 4 knight moves)
        assertEquals(20, legalMoves.size)
    }

    @Test
    fun testFoolsMateCheckmate() {
        // 1. f3 e5 2. g4 Qh4#
        var board = Board.fromFen()
        // f2f3
        val f2f3 = board.generateLegalMoves().first { it.from == Square.fromName("f2") && it.to == Square.fromName("f3") }
        board = board.makeMove(f2f3)

        // e7e5
        val e7e5 = board.generateLegalMoves().first { it.from == Square.fromName("e7") && it.to == Square.fromName("e5") }
        board = board.makeMove(e7e5)

        // g2g4
        val g2g4 = board.generateLegalMoves().first { it.from == Square.fromName("g2") && it.to == Square.fromName("g4") }
        board = board.makeMove(g2g4)

        // d8h4#
        val d8h4 = board.generateLegalMoves().first { it.from == Square.fromName("d8") && it.to == Square.fromName("h4") }
        board = board.makeMove(d8h4)

        assertTrue("White should be in check", board.isInCheck(PieceColor.WHITE))
        assertTrue("White should be checkmated", board.isCheckmate())
    }

    @Test
    fun testEngineAnalysisFindsWinningMove() = runBlocking {
        val engine = ChessEngine()
        val settings = CalculationSettings(
            activeProfile = EngineProfile.getById(ModeId.PROFAST),
            isFast = true,
            isSuperFast = true
        )

        // Scholar's mate position: White has Qxf7#
        // 1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6
        val board = Board.fromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        val result = engine.analyze(board, settings)

        assertNotNull("Engine should return best move", result.bestMove)
        assertEquals("f7", result.bestMove?.to?.name)
        assertTrue("Best move should evaluate as mate or massive advantage", result.evaluationCp > 5000)
    }
}
