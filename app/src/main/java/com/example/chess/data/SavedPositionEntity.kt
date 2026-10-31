package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_positions")
data class SavedPositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fen: String,
    val pgn: String = "",
    val modeUsed: String,
    val evaluation: String,
    val bestMove: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
