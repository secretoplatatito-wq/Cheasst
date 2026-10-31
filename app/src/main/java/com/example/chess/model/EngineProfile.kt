package com.example.chess.model

enum class ModeId {
    NORMAL,
    PRO,
    PROFAST,
    MASTER,
    GRANDMASTER,
    COMPLEX_GRANDMASTER
}

enum class Complexity {
    PRO,
    COMPLEX
}

data class EngineProfile(
    val id: ModeId,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val baseDepth: Int,
    val maxTimeMs: Long,
    val multiPvCount: Int,
    val strategicWeight: Float,
    val tacticalWeight: Float,
    val humanLikePruning: Boolean
) {
    companion object {
        val ALL_PROFILES = listOf(
            EngineProfile(
                id = ModeId.NORMAL,
                displayName = "Normal",
                subtitle = "Human-Like & Practical",
                description = "Generates natural, solid, and non-suspicious moves that avoid extreme engine lines. Easy to understand and play.",
                baseDepth = 6,
                maxTimeMs = 800L,
                multiPvCount = 2,
                strategicWeight = 1.0f,
                tacticalWeight = 0.8f,
                humanLikePruning = true
            ),
            EngineProfile(
                id = ModeId.PRO,
                displayName = "Pro",
                subtitle = "Positional Mastery",
                description = "Balanced positional analysis and sharp tactical safety. Evaluates key candidate moves with strategic depth.",
                baseDepth = 10,
                maxTimeMs = 1500L,
                multiPvCount = 3,
                strategicWeight = 1.2f,
                tacticalWeight = 1.2f,
                humanLikePruning = false
            ),
            EngineProfile(
                id = ModeId.PROFAST,
                displayName = "ProFast",
                subtitle = "Instant Response Speed",
                description = "Optimized for ultra-responsive calculation. Aggressively prunes dead branches to return strong moves in milliseconds.",
                baseDepth = 8,
                maxTimeMs = 400L,
                multiPvCount = 2,
                strategicWeight = 1.0f,
                tacticalWeight = 1.0f,
                humanLikePruning = false
            ),
            EngineProfile(
                id = ModeId.MASTER,
                displayName = "Master",
                subtitle = "Deep Tactical Sight",
                description = "Deep multi-variation search for attacking combinations, subtle defensive resources, and critical game-ending breaks.",
                baseDepth = 14,
                maxTimeMs = 3000L,
                multiPvCount = 3,
                strategicWeight = 1.3f,
                tacticalWeight = 1.5f,
                humanLikePruning = false
            ),
            EngineProfile(
                id = ModeId.GRANDMASTER,
                displayName = "Grandmaster",
                subtitle = "Tournament Grade Calculation",
                description = "Simulates grandmaster-level positional foresight, king safety, bishop pair values, and pawn structure evaluation.",
                baseDepth = 16,
                maxTimeMs = 5000L,
                multiPvCount = 4,
                strategicWeight = 1.5f,
                tacticalWeight = 1.6f,
                humanLikePruning = false
            ),
            EngineProfile(
                id = ModeId.COMPLEX_GRANDMASTER,
                displayName = "Complex Grandmaster",
                subtitle = "Exhaustive Deep Exploration",
                description = "Maximum search resources. Deep principal variations, rigorous endgame tables, and comprehensive candidate line comparisons.",
                baseDepth = 18,
                maxTimeMs = 8000L,
                multiPvCount = 5,
                strategicWeight = 1.8f,
                tacticalWeight = 1.8f,
                humanLikePruning = false
            )
        )

        fun getById(id: ModeId): EngineProfile =
            ALL_PROFILES.find { it.id == id } ?: ALL_PROFILES[0]
    }
}

data class CalculationSettings(
    val activeProfile: EngineProfile = EngineProfile.getById(ModeId.PROFAST),
    val isFast: Boolean = true,
    val isSuperFast: Boolean = false,
    val complexity: Complexity = Complexity.PRO,
    val cpuThreads: Int = 4,
    val hashMemoryMb: Int = 64,
    val isLocalAiEnabled: Boolean = true,
    val isCloudAiEnabled: Boolean = false
) {
    val effectiveDepth: Int
        get() {
            var d = activeProfile.baseDepth
            if (isSuperFast) d = (d - 3).coerceAtLeast(4)
            else if (isFast) d = (d - 1).coerceAtLeast(5)

            if (complexity == Complexity.COMPLEX) d += 2
            return d
        }

    val effectiveTimeLimitMs: Long
        get() {
            var t = activeProfile.maxTimeMs
            if (isSuperFast) t = (t / 3).coerceAtLeast(300L)
            else if (isFast) t = (t / 2).coerceAtLeast(500L)

            if (complexity == Complexity.COMPLEX) t = (t * 1.5).toLong()
            return t
        }

    val effectiveMultiPv: Int
        get() = if (complexity == Complexity.COMPLEX) {
            (activeProfile.multiPvCount + 1).coerceAtMost(5)
        } else {
            activeProfile.multiPvCount.coerceAtLeast(2)
        }
}
