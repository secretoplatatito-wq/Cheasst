package com.example.chess.model

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Invalid square coordinates: ($file, $rank)" }
    }

    val index: Int get() = rank * 8 + file

    val name: String get() = "${('a' + file)}${rank + 1}"

    val isLight: Boolean get() = (file + rank) % 2 != 0

    companion object {
        private val ALL_SQUARES = Array(64) { i ->
            Square(file = i % 8, rank = i / 8)
        }

        fun at(file: Int, rank: Int): Square = ALL_SQUARES[rank * 8 + file]

        fun fromIndex(index: Int): Square = ALL_SQUARES[index]

        fun fromName(name: String): Square? {
            if (name.length != 2) return null
            val f = name[0].lowercaseChar() - 'a'
            val r = name[1].digitToIntOrNull()?.minus(1) ?: return null
            if (f !in 0..7 || r !in 0..7) return null
            return at(f, r)
        }
    }
}
