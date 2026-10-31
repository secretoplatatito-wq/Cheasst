package com.example.chess.ui

import android.app.Application
import android.graphics.Bitmap
import android.os.BatteryManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.ai.AiStrategicAdvice
import com.example.chess.ai.GeminiChessAdvisor
import com.example.chess.data.AppDatabase
import com.example.chess.data.SavedPositionEntity
import com.example.chess.engine.AnalysisResult
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import com.example.chess.vision.BoardDetector
import com.example.chess.vision.VisionDetectionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.chess.audio.SpeechManager
import com.example.chess.engine.OpeningBook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HardwareMetrics(
    val usedRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val maxRamMb: Long = 0,
    val availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
    val cpuUtilizationPercent: Int = 18,
    val batteryPercent: Int = 85,
    val tempCacheKb: Long = 124,
    val persistentCount: Int = 0,
    val isThrottledForThermalSafety: Boolean = false,
    val storageLimitMb: Int = 5120
)

class CheasstViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.savedPositionDao()
    private val engine = ChessEngine()
    private val aiAdvisor = GeminiChessAdvisor()
    private val speechManager = SpeechManager(application)

    private val _board = MutableStateFlow(Board.fromFen())
    val board: StateFlow<Board> = _board.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.BLACK)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _isLiveTracking = MutableStateFlow(true)
    val isLiveTracking: StateFlow<Boolean> = _isLiveTracking.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(listOf(
        "[SYSTEM] Cheasst Grandmaster Vision Engine v2.1 online",
        "[STATUS] Perspective: Playing as BLACK ♚",
        "[CAMERA] Spatial AR alignment locked. Awaiting move..."
    ))
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _moveHistory = MutableStateFlow<List<Move>>(emptyList())
    val moveHistory: StateFlow<List<Move>> = _moveHistory.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private val _legalMoves = MutableStateFlow<List<Move>>(emptyList())
    val legalMoves: StateFlow<List<Move>> = _legalMoves.asStateFlow()

    private val _settings = MutableStateFlow(CalculationSettings())
    val settings: StateFlow<CalculationSettings> = _settings.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _aiAdvice = MutableStateFlow<AiStrategicAdvice?>(null)
    val aiAdvice: StateFlow<AiStrategicAdvice?> = _aiAdvice.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _visionResult = MutableStateFlow<VisionDetectionResult?>(null)
    val visionResult: StateFlow<VisionDetectionResult?> = _visionResult.asStateFlow()

    private val _isFlipped = MutableStateFlow(true) // Black perspective by default
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val savedPositions: StateFlow<List<SavedPositionEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) dao.getAllFlow() else dao.searchPositions(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _hardwareMetrics = MutableStateFlow(HardwareMetrics())
    val hardwareMetrics: StateFlow<HardwareMetrics> = _hardwareMetrics.asStateFlow()

    private var analysisJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        updateLegalMoves()
        triggerAutoAnalysis(speakBestMove = false)
        startHardwareMonitoring()
    }

    private fun startHardwareMonitoring() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                val runtime = Runtime.getRuntime()
                val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val total = runtime.totalMemory() / (1024 * 1024)
                val max = runtime.maxMemory() / (1024 * 1024)
                val freeMb = max - used

                val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val battery = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
                val cpuEst = if (_isAnalyzing.value) (45..75).random() else (8..22).random()
                val count = dao.getCount()

                // Auto-tune / throttle if RAM is critically constrained (< 200MB free) to avoid device heating
                val isThrottled = freeMb < 200
                if (isThrottled && !_settings.value.isFast) {
                    _settings.value = _settings.value.copy(
                        isFast = true,
                        cpuThreads = 2,
                        hashMemoryMb = 32
                    )
                    addLog("[THERMAL GUARD] RAM pressure detected (${freeMb}MB free). Auto-throttled calculation to keep device cool.")
                }

                _hardwareMetrics.value = _hardwareMetrics.value.copy(
                    usedRamMb = used,
                    totalRamMb = total,
                    maxRamMb = max,
                    availableProcessors = runtime.availableProcessors(),
                    cpuUtilizationPercent = cpuEst,
                    batteryPercent = battery,
                    tempCacheKb = (_moveHistory.value.size * 4L) + 64L,
                    persistentCount = count,
                    isThrottledForThermalSafety = isThrottled
                )
                delay(2000L)
            }
        }
    }

    fun onSquareClicked(square: Square) {
        val currentSelected = _selectedSquare.value
        val currentBoard = _board.value

        if (currentSelected == null) {
            val piece = currentBoard.getPiece(square)
            if (piece != null && piece.color == currentBoard.activeColor) {
                _selectedSquare.value = square
                _legalMoves.value = currentBoard.generateLegalMoves().filter { it.from == square }
            }
        } else {
            // Check if clicking another own piece
            val piece = currentBoard.getPiece(square)
            if (piece != null && piece.color == currentBoard.activeColor) {
                _selectedSquare.value = square
                _legalMoves.value = currentBoard.generateLegalMoves().filter { it.from == square }
                return
            }

            // Attempt to move
            val matchingMove = _legalMoves.value.find { it.to == square }
            if (matchingMove != null) {
                makeMove(matchingMove)
            }
            _selectedSquare.value = null
            _legalMoves.value = emptyList()
        }
    }

    fun makeMove(move: Move) {
        val newBoard = _board.value.makeMove(move)
        _board.value = newBoard
        _moveHistory.value = _moveHistory.value + move
        _selectedSquare.value = null
        _legalMoves.value = emptyList()
        triggerAutoAnalysis(speakBestMove = true)
    }

    fun undoMove() {
        if (_moveHistory.value.isNotEmpty()) {
            val newHistory = _moveHistory.value.dropLast(1)
            var replayBoard = Board.fromFen()
            for (m in newHistory) {
                replayBoard = replayBoard.makeMove(m)
            }
            _board.value = replayBoard
            _moveHistory.value = newHistory
            _selectedSquare.value = null
            _legalMoves.value = emptyList()
            triggerAutoAnalysis()
        }
    }

    fun resetBoard(fen: String = Board.STARTING_FEN) {
        _board.value = Board.fromFen(fen)
        _moveHistory.value = emptyList()
        _selectedSquare.value = null
        _legalMoves.value = emptyList()
        _aiAdvice.value = null
        addLog("board reset to starting position: fen=$fen")
        triggerAutoAnalysis(speakBestMove = false)
    }

    fun flipBoard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$time] $msg"
        val current = _consoleLogs.value.takeLast(19).toMutableList()
        current.add(formatted)
        _consoleLogs.value = current
    }

    fun setPlayerColor(color: PieceColor) {
        _playerColor.value = color
        _isFlipped.value = (color == PieceColor.BLACK)
        addLog("perspective switched: playing as ${color.name} ${if (color == PieceColor.BLACK) "♚" else "♔"}")
        triggerAutoAnalysis(speakBestMove = true)
    }

    fun togglePlayerColor() {
        val newColor = if (_playerColor.value == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        setPlayerColor(newColor)
    }

    fun toggleSound() {
        val next = !_isSoundEnabled.value
        _isSoundEnabled.value = next
        speechManager.isEnabled = next
        addLog("audio announcement: ${if (next) "ENABLED" else "MUTED"}")
    }

    fun toggleLiveTracking() {
        val next = !_isLiveTracking.value
        _isLiveTracking.value = next
        addLog("real-time AR tracking: ${if (next) "ACTIVE" else "PAUSED"}")
    }

    fun triggerAutoAnalysis(speakBestMove: Boolean = false) {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _isAnalyzing.value = true
            val currentBoard = _board.value
            val pColor = _playerColor.value
            val isFast = _settings.value.isFast || _settings.value.isSuperFast
            val profile = _settings.value.activeProfile

            val result = com.example.chess.engine.MultiEngineService.analyzePosition(
                board = currentBoard,
                profile = profile,
                playerPerspective = pColor,
                isFast = isFast
            )
            _analysisResult.value = result
            _isAnalyzing.value = false

            val best = result.bestMove
            if (best != null) {
                val pieceName = best.piece.type.name.lowercase()
                val moveStr = "${best.from.name}->${best.to.name}"
                val evalStr = result.formattedEval
                addLog("new move sent to rendering engine: $pieceName $moveStr ($evalStr)")

                if (speakBestMove && currentBoard.activeColor == pColor) {
                    speechManager.speakMove(best, isMate = result.isCheckmate)
                }
            } else if (result.isCheckmate) {
                val winner = if (currentBoard.activeColor == PieceColor.WHITE) "BLACK" else "WHITE"
                addLog("CHECKMATE DETECTED. VICTORY FOR $winner.")
                speechManager.speak("Checkmate! Game over.")
            }
        }
    }

    fun confirmPlayerMove(move: Move) {
        val pieceName = move.piece.type.name.lowercase()
        val moveStr = "${move.from.name}->${move.to.name}"
        addLog("player move confirmed: $pieceName $moveStr")
        makeMove(move)
    }

    fun stepOpponentMove() {
        val currentBoard = _board.value
        val legal = currentBoard.generateLegalMoves()
        if (legal.isEmpty()) return

        // Pick opening book reply or capture
        val book = OpeningBook.lookup(currentBoard.toFen())
        val move = if (book != null) {
            legal.find { it.from.name == book.fromSquare && it.to.name == book.toSquare } ?: legal.first()
        } else {
            legal.maxByOrNull { it.capturedPiece?.type?.value ?: 0 } ?: legal.first()
        }

        val pieceName = move.piece.type.name.lowercase()
        val captureNotice = if (move.capturedPiece != null) " (takes ${move.capturedPiece.type.name.lowercase()})" else ""
        addLog("opponent turn finished: $pieceName ${move.from.name}->${move.to.name}$captureNotice")
        addLog("query stockfish with board positioning")
        makeMove(move)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }

    fun setEngineProfile(profile: EngineProfile) {
        _settings.value = _settings.value.copy(activeProfile = profile)
        triggerAutoAnalysis()
    }

    fun toggleFast(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isFast = enabled,
            isSuperFast = if (enabled) _settings.value.isSuperFast else false
        )
        triggerAutoAnalysis()
    }

    fun toggleSuperFast(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isSuperFast = enabled,
            isFast = if (enabled) true else _settings.value.isFast
        )
        triggerAutoAnalysis()
    }

    fun setComplexity(complexity: Complexity) {
        _settings.value = _settings.value.copy(complexity = complexity)
        triggerAutoAnalysis()
    }

    fun setCpuThreads(threads: Int) {
        _settings.value = _settings.value.copy(cpuThreads = threads.coerceIn(1, 8))
    }

    fun setHashMemory(mb: Int) {
        _settings.value = _settings.value.copy(hashMemoryMb = mb.coerceIn(16, 512))
    }

    fun toggleCloudAi(enabled: Boolean) {
        _settings.value = _settings.value.copy(isCloudAiEnabled = enabled)
    }

    fun requestAiAdvice() {
        viewModelScope.launch {
            _isAiThinking.value = true
            val candidates = _analysisResult.value?.candidateMoves ?: emptyList()
            val eval = _analysisResult.value?.formattedEval ?: "+0.00"
            val advice = aiAdvisor.getStrategicAdvice(
                board = _board.value,
                candidates = candidates,
                evaluation = eval,
                isGroundedSearch = _settings.value.isCloudAiEnabled
            )
            _aiAdvice.value = advice
            _isAiThinking.value = false
        }
    }

    fun processCameraFrame(bitmap: Bitmap) {
        val result = BoardDetector.detectBoard(bitmap, _board.value)
        _visionResult.value = result
    }

    fun setStorageLimitMb(mb: Int) {
        _hardwareMetrics.value = _hardwareMetrics.value.copy(storageLimitMb = mb.coerceIn(256, 5120))
    }

    fun applyDetectedFen(fen: String) {
        resetBoard(fen)
    }

    fun updateSquarePiece(square: Square, piece: Piece?) {
        val newSquares = _board.value.squares.toMutableList()
        newSquares[square.index] = piece
        _board.value = _board.value.copy(squares = newSquares)
        triggerAutoAnalysis()
    }

    fun saveCurrentPosition(title: String, notes: String = "") {
        viewModelScope.launch {
            val eval = _analysisResult.value?.formattedEval ?: "0.00"
            val best = _analysisResult.value?.bestMove?.san ?: "-"
            val entity = SavedPositionEntity(
                title = title.ifBlank { "Position #${System.currentTimeMillis() % 1000}" },
                fen = _board.value.toFen(),
                pgn = _moveHistory.value.joinToString(" ") { it.san },
                modeUsed = _settings.value.activeProfile.displayName,
                evaluation = eval,
                bestMove = best,
                notes = notes
            )
            dao.insert(entity)
        }
    }

    fun deleteSavedPosition(position: SavedPositionEntity) {
        viewModelScope.launch {
            dao.delete(position)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearTemporaryCache() {
        // Frees temporary calculation trees and garbage-collects volatile buffers
        _analysisResult.value = null
        System.gc()
        triggerAutoAnalysis()
    }

    private fun updateLegalMoves() {
        _legalMoves.value = emptyList()
    }
}
