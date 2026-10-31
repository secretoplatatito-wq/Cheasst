package com.example.chess.engine

import com.example.chess.model.Board
import com.example.chess.model.CalculationSettings
import com.example.chess.model.EngineProfile
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class AcasAccuracyMetrics(
    val accuracyScore: Float,         // 0.0 - 100.0%
    val averageCentipawnLoss: Int,    // ACPL
    val moveClassification: String,   // "Brilliant", "Great", "Best", "Good", "Inaccuracy", "Mistake", "Blunder"
    val cheatSuspicionLevel: String,  // "Low (Human)", "Moderate", "High (Engine-like)"
    val winProbability: Float        // 0.0 - 1.0
)

data class Lc0PolicyDistribution(
    val topMoves: List<Pair<String, Float>>, // SAN move to policy probability (e.g. "Nf6" -> 0.42f)
    val winRate: Float,                     // Win rate 0.0 to 1.0
    val drawRate: Float,
    val lossRate: Float,
    val positionalThemes: List<String>       // e.g. ["Outpost Knight", "Open d-file", "King Shelter"]
)

data class EnhancedAnalysisResult(
    val primaryEngine: String,
    val bestMove: Move?,
    val candidateMoves: List<CandidateMove>,
    val centipawnScore: Int,
    val formattedEval: String,
    val depth: Int,
    val nodesEvaluated: Long,
    val acasMetrics: AcasAccuracyMetrics,
    val lc0Policy: Lc0PolicyDistribution?,
    val isCheckmate: Boolean,
    val engineSource: String // "Stockfish 16 Cloud", "Lc0 Neural Policy", "Local Fast Minimax"
)

object MultiEngineService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    private val localEngine = ChessEngine()

    /**
     * Executes multi-engine analysis combining Stockfish Cloud, Lc0 Positional Evaluation,
     * and ACAS / Chesalyser accuracy metrics.
     */
    suspend fun analyzePosition(
        board: Board,
        profile: EngineProfile,
        playerPerspective: PieceColor,
        isFast: Boolean = false
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val fen = board.toFen()
        val isWhite = board.activeColor == PieceColor.WHITE

        // 1. Try Stockfish Cloud API if network is available
        var cloudMove: String? = null
        var cloudEval: Float? = null
        var engineSource = "Local Fast Minimax"

        if (profile.id == com.example.chess.model.ModeId.GRANDMASTER ||
            profile.id == com.example.chess.model.ModeId.COMPLEX_GRANDMASTER ||
            profile.id == com.example.chess.model.ModeId.MASTER
        ) {
            try {
                val encodedFen = java.net.URLEncoder.encode(fen, "UTF-8")
                val request = Request.Builder()
                    .url("https://stockfish.online/api/s/v2.php?fen=$encodedFen&depth=13")
                    .header("User-Agent", "Cheasst-Android-AI/2.1")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        if (json.optBoolean("success", false)) {
                            val data = json.optString("data", "")
                            // Example data format: "bestmove e7e5 ponder g1f3" or evaluation
                            val parts = data.split(" ")
                            if (parts.size >= 2 && parts[0] == "bestmove") {
                                cloudMove = parts[1]
                                engineSource = "Stockfish 16 Cloud"
                            }
                            if (json.has("evaluation")) {
                                cloudEval = json.optDouble("evaluation", 0.0).toFloat()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Graceful fallback to local engine
                engineSource = "Local Fast Minimax"
            }
        }

        // 2. Perform local engine search to guarantee candidate lines, tactical verification, and mate search
        val depthToSearch = if (isFast) 4 else 6
        val localResult = localEngine.analyze(
            board = board,
            settings = CalculationSettings(
                activeProfile = profile,
                isFast = isFast
            ),
            playerPerspective = playerPerspective
        )

        // 3. Compute Leela Chess Zero (Lc0) Positional Policy
        val lc0Policy = computeLc0PositionalPolicy(board)

        // 4. Compute Chesalyser & A.C.A.S Accuracy Metrics
        val acas = computeAcasMetrics(
            evalCp = localResult.centipawnScore,
            candidates = localResult.candidateMoves,
            board = board
        )

        // Determine best move (Cloud Stockfish or Local Negamax)
        var resolvedBestMove = localResult.bestMove
        if (cloudMove != null && cloudMove.length >= 4) {
            val fromStr = cloudMove.substring(0, 2)
            val toStr = cloudMove.substring(2, 4)
            val matchingMove = board.generateLegalMoves().find {
                it.from.name.equals(fromStr, ignoreCase = true) && it.to.name.equals(toStr, ignoreCase = true)
            }
            if (matchingMove != null) {
                resolvedBestMove = matchingMove
            }
        }

        val evalString = if (cloudEval != null) {
            val sign = if (cloudEval >= 0) "+" else ""
            "$sign%.2f".format(cloudEval)
        } else {
            localResult.formattedEval
        }

        AnalysisResult(
            bestMove = resolvedBestMove,
            evaluationCp = localResult.centipawnScore,
            candidateMoves = localResult.candidateMoves,
            depthReached = if (engineSource.contains("Stockfish")) 13 else depthToSearch,
            nodesEvaluated = localResult.nodesEvaluated,
            calculationTimeMs = localResult.calculationTimeMs,
            nodesPerSecond = localResult.nodesPerSecond,
            isCheckmate = localResult.isCheckmate,
            isStalemate = localResult.isStalemate,
            acasMetrics = acas,
            lc0Policy = lc0Policy,
            engineSource = engineSource
        )
    }

    /**
     * Positional Policy Evaluator inspired by Leela Chess Zero (Lc0) neural networks:
     * Evaluates space, king shelter, outpost knights, open bishop diagonals, and pawn structures.
     */
    private fun computeLc0PositionalPolicy(board: Board): Lc0PolicyDistribution {
        val legalMoves = board.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return Lc0PolicyDistribution(emptyList(), 0.0f, 0.0f, 1.0f, listOf("No legal moves"))
        }

        val themes = mutableListOf<String>()
        val scoredMoves = legalMoves.map { move ->
            var policyWeight = 10.0f

            // 1. Center control bonus (d4, d5, e4, e5)
            if (move.to.file in 3..4 && move.to.rank in 3..4) {
                policyWeight += 25.0f
                if (!themes.contains("Central Domination")) themes.add("Central Domination")
            }

            // 2. Knight outposts on rank 4/5/6
            if (move.piece.type == PieceType.KNIGHT && (move.to.rank in 3..5)) {
                policyWeight += 20.0f
                if (!themes.contains("Knight Outpost")) themes.add("Knight Outpost")
            }

            // 3. Captures and tactical tension
            if (move.capturedPiece != null) {
                policyWeight += move.capturedPiece.type.value * 5.0f
            }

            // 4. King safety / Castling
            if (move.isCastling) {
                policyWeight += 35.0f
                if (!themes.contains("King Shelter Safety")) themes.add("King Shelter Safety")
            }

            // 5. Piece development in opening
            if (board.fullMoveNumber <= 10 && (move.piece.type == PieceType.KNIGHT || move.piece.type == PieceType.BISHOP)) {
                policyWeight += 15.0f
                if (!themes.contains("Rapid Development")) themes.add("Rapid Development")
            }

            move.san to policyWeight
        }

        val totalWeight = scoredMoves.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1.0f)
        val sortedPolicies = scoredMoves
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first to (it.second / totalWeight) }

        val bestPolicy = sortedPolicies.firstOrNull()?.second ?: 0.5f
        val winRate = (bestPolicy * 0.8f + 0.1f).coerceIn(0.05f, 0.95f)
        val drawRate = ((1.0f - winRate) * 0.6f).coerceIn(0.05f, 0.6f)
        val lossRate = (1.0f - winRate - drawRate).coerceAtLeast(0.0f)

        if (themes.isEmpty()) themes.add("Positional Harmony")

        return Lc0PolicyDistribution(
            topMoves = sortedPolicies,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            positionalThemes = themes
        )
    }

    /**
     * Chesalyser & A.C.A.S Accuracy Engine:
     * Derives centipawn loss, move precision, and anti-cheat suspicion level.
     */
    private fun computeAcasMetrics(
        evalCp: Int,
        candidates: List<CandidateMove>,
        board: Board
    ): AcasAccuracyMetrics {
        val topScore = candidates.firstOrNull()?.scoreCp ?: evalCp
        val secondScore = candidates.getOrNull(1)?.scoreCp ?: (topScore - 60)
        val delta = abs(topScore - secondScore)

        // Average centipawn loss estimation based on depth variance
        val acpl = (35 - (delta / 8)).coerceIn(4, 85)

        // Accuracy score mapping (formula inspired by Chesalyser / Lichess accuracy)
        val accuracy = (103.5f - (acpl * 1.15f)).coerceIn(45.0f, 99.4f)

        // Move classification
        val classification = when {
            delta >= 300 && topScore > 100 -> "Brilliant (!!)"
            delta >= 150 -> "Great (!)"
            acpl < 12 -> "Best"
            acpl < 25 -> "Excellent"
            acpl < 45 -> "Good"
            acpl < 70 -> "Inaccuracy (?!)"
            acpl < 120 -> "Mistake (?)"
            else -> "Blunder (??)"
        }

        // Anti-cheat suspicion (A.C.A.S style)
        val suspicion = when {
            accuracy >= 98.0f && acpl <= 6 -> "High (Engine-like 99%)"
            accuracy >= 94.0f -> "Moderate (Master-level)"
            else -> "Low (Human Play)"
        }

        // Win probability from centipawn score: 1 / (1 + 10^(-eval/400))
        val winProb = (1.0 / (1.0 + Math.pow(10.0, -evalCp.toDouble() / 400.0))).toFloat()

        return AcasAccuracyMetrics(
            accuracyScore = accuracy,
            averageCentipawnLoss = acpl,
            moveClassification = classification,
            cheatSuspicionLevel = suspicion,
            winProbability = winProb
        )
    }
}
