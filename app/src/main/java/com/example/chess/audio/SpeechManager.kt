package com.example.chess.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.chess.model.Move
import com.example.chess.model.PieceType
import java.util.Locale

class SpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    var isEnabled: Boolean = true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
            }
        }
    }

    fun speak(text: String) {
        if (!isEnabled || !isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cheasst_speech_${System.currentTimeMillis()}")
    }

    fun speakMove(move: Move, isMate: Boolean = false) {
        if (!isEnabled || !isReady) return
        val pieceName = when (move.piece.type) {
            PieceType.PAWN -> "Pawn"
            PieceType.KNIGHT -> "Knight"
            PieceType.BISHOP -> "Bishop"
            PieceType.ROOK -> "Rook"
            PieceType.QUEEN -> "Queen"
            PieceType.KING -> "King"
        }
        val target = formatSquareName(move.to.name)
        val action = if (move.capturedPiece != null) "takes" else "to"
        val mateSuffix = if (isMate) ", checkmate" else ""

        val phrase = "$pieceName $action $target$mateSuffix"
        speak(phrase)
    }

    private fun formatSquareName(name: String): String {
        if (name.length != 2) return name
        val fileLetter = name[0].uppercaseChar()
        val rankNumber = name[1]
        return "$fileLetter $rankNumber"
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
