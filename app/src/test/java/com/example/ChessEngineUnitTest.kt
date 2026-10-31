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

    @Test
    fun testBlackburneShillingGambitDetection() = runBlocking {
        val engine = ChessEngine()
        val settings = CalculationSettings(
            activeProfile = EngineProfile.getById(ModeId.PROFAST),
            isFast = true,
            isSuperFast = true
        )

        // Position after 1. e4 e5 2. Nf3 Nc6 3. Bc4 Nd4 4. Nxe5?
        val board = Board.fromFen("r1bqkbnr/pppp1ppp/8/4N3/2Bn4/8/PPPP1PPP/RNBQK2R b KQkq - 0 4")
        val result = engine.analyze(board, settings, playerPerspective = PieceColor.BLACK)

        assertNotNull("Engine should return best move for Black", result.bestMove)
        assertEquals("d8", result.bestMove?.from?.name)
        assertEquals("g5", result.bestMove?.to?.name)
        assertTrue("Qg5 evaluation should be positive for Black", result.evaluationCp > 0)
    }

    @Test
    fun testSmotheredMateDetection() = runBlocking {
        val engine = ChessEngine()
        val settings = CalculationSettings(
            activeProfile = EngineProfile.getById(ModeId.PROFAST),
            isFast = true,
            isSuperFast = true
        )

        // Position after 1. e4 e5 2. Nf3 Nc6 3. Bc4 Nd4 4. Nxe5 Qg5 5. Nxf7 Qxg2 6. Rf1 Qxe4+ 7. Be2
        // Black to play and mate in 1: Nf3#
        val board = Board.fromFen("r1b1k1nr/pppp1Npp/8/8/3nq3/8/PPPPBP1P/RNBQKR2 b Qkq - 0 7")
        val result = engine.analyze(board, settings, playerPerspective = PieceColor.BLACK)

        assertNotNull("Engine should find checkmating move", result.bestMove)
        assertEquals("d4", result.bestMove?.from?.name)
        assertEquals("f3", result.bestMove?.to?.name)
        assertTrue("Mate in 1 should evaluate to >15000", result.evaluationCp >= 15000)
    }
}
