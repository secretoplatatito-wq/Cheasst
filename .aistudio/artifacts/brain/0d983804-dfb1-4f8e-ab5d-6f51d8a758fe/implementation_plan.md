# Cheasst — AI-Powered Mobile Chess Vision & Engine Assistant

Cheasst transforms any Android smartphone into an ultra-fast, offline-first portable chess analysis computer and grandmaster assistant. Combining real-time camera chessboard vision, an embedded high-performance chess calculation engine, memory persistence, and optional Gemini cloud intelligence, Cheasst enables players to scan physical or digital boards, calculate the best tactical lines across 6 calibrated engine modes, and learn strategic master concepts.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed in Phase 1 and will govern the implementation:

- **Confirmed Decision 1 (Calculation Pipeline)**: **Hybrid Local-First with optional Gemini Cloud AI**. The app operates 100% offline using an embedded bitboard chess calculation engine (supporting UCI concepts, alpha-beta search, quiescence, MVV-LVA, transposition table, and piece-square tables). When enabled, Gemini AI (via Firebase AI Logic / Gemini models) provides grandmaster natural-language move explanations, strategic summaries, opening theory lookup with Google Search data grounding, and deep thinking analysis.
- **Confirmed Decision 2 (Camera Vision System)**: **Automatic live board detection with manual correction overlay**. CameraX preview with real-time frame gating (sharpness/stability analysis), adaptive perspective correction, 64-square grid extraction, piece classifier with confidence scoring, and an interactive tap-to-correct board editor before committing moves.
- **Confirmed Decision 3 (Visual Aesthetic)**: **Grandmaster Dark with sleek obsidian and tactical emerald**. Deep midnight carbon background (`#0A0D0E`), elevated slate cards (`#141A1C`), tactical emerald highlights (`#00E676` / `#00B0FF`), crisp ivory piece notation, and rich tactical evaluation bars.

---

### 1. Overview & Core Concept

- **What It Does**: Cheasst enables chess players, tournament competitors, and learners to instantly analyze games in real life or on screen. Pointing the phone camera at any physical board reconstructs the FEN position; the engine evaluates the candidate moves according to the selected mode (from beginner-friendly Normal to Complex Grandmaster); and interactive controls allow exploring principal variations (PVs), blunders, and master explanations.
- **Target Audience / Persona**:
  - Competitive club & tournament players reviewing physical over-the-board games.
  - Casual learners seeking non-suspicious, sound, and easily understandable moves without overwhelming complexity.
  - Advanced players who want deep tactical calculation, candidate lines, and opening/endgame theory on the go.
- **Key Value**: Operates strictly within a lightweight 3–4 GB RAM budget, completely private and offline by default, with instant response times, zero cloud lock-in, and persistent memory that survives app force-stops.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Dashboard & Quick Start**:
   - Status header displaying active Engine Mode (e.g. *ProFast* or *Grandmaster*), Hardware Telemetry (RAM usage, CPU threads, Nodes/sec), and Connectivity status (Offline Local / Gemini AI).
   - Instant action buttons: **Open Vision Scanner**, **Interactive Analysis Board**, **Quick Setup / FEN**, and **Recent Saved Games**.
2. **Vision Camera Scanner**:
   - Full-screen CameraX live viewfinder with square bounding guides and corner alignment reticles.
   - Live visual feedback: Board Lock indicator (Green when stable, Amber when adjusting), grid overlay showing detected pieces with confidence indicators.
   - Freeze & Verify drawer: Displays detected FEN and interactive mini-board allowing the user to tap any square and swap pieces before pushing to Analysis.
3. **Interactive Analysis & Engine Board**:
   - Vector-rendered 8x8 chessboard with legal move highlighting, last move markers, and check alerts.
   - Dual-sided evaluation bar (centipawns or mate count) with smooth animated fill.
   - Engine HUD: Depth, Nodes/sec, Hash usage, Best Move arrow overlay.
   - Multi-PV Candidate Move cards: Top 3 candidate lines with algebraic notation, tactical labels (Best, Blunder, Inaccuracy, Brilliant), and "Explain with AI" trigger.
4. **Analysis Profiles & Calculation Tuning**:
   - 6 engine profiles with distinct search parameters:
     1. **Normal**: Human-like, practical, low suspicion, easy to understand.
     2. **Pro**: Balanced tactical and positional depth (depth 12–16).
     3. **ProFast**: Speed-optimized aggressive pruning for sub-second moves.
     4. **Master**: Deep tactical calculation & multi-variation search.
     5. **Grandmaster**: High-depth search with opening books & endgame tables.
     6. **Complex Grandmaster**: Maximum depth & multi-PV candidate evaluation.
   - Calculation toggles: Fast mode, Super Fast mode, Pro/Complex complexity, CPU threads (1–8), and Hash memory limits.
5. **Memory & Knowledge Bank**:
   - Room-backed local repository storing saved FEN/PGN games, analysis logs, opening book references, and personal notes.
   - Searchable history with export/import capabilities.
6. **Performance & Hardware Dashboard**:
   - Real Android Runtime metrics: allocated memory, free memory, max heap, thread pool saturation, and engine throughput.

#### Visual Identity & Theme Tokens

- **Palette**:
  - `Background`: Deep Midnight Obsidian (`#0A0D0E`)
  - `Surface`: Elevated Charcoal Slate (`#13191C`)
  - `SurfaceVariant`: Subtle Border Slate (`#1F272B`)
  - `Primary / Accent`: Tactical Emerald (`#00E676`)
  - `Secondary Accent`: Cyber Cyan (`#00E5FF`)
  - `Warning / Tactical Alert`: Amber Gold (`#FFD600`)
  - `Board Light Square`: Cream Ivory (`#EEEED2`)
  - `Board Dark Square`: Tournament Forest Slate (`#769656`)
- **Typography**: Clean high-legibility geometric sans with monospace font for FEN strings, PGN moves, and engine telemetry.
- **Accessibility & Touch Targets**: Minimum 48dp touch targets, TalkBack content descriptions on all chess squares and buttons, and edge-to-edge window insets padding.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Embedded Bitboard Rules & Calculation Engine vs. External Binary**:
  - *Chosen Approach*: Implement a pure Kotlin 64-bit Bitboard chess rules and calculation engine featuring minimax alpha-beta pruning, quiescence search, MVV-LVA move ordering, piece-square evaluation tables, and UCI-compliant state management.
  - *Why*: Eliminates ABI architecture crashes (x86_64 vs arm64-v8a in emulator vs physical phones), requires zero external binary download, runs 100% offline within memory limits, and guarantees 100% reliable compilation across all devices.
- **Decision 2: Vision Pipeline Architecture**:
  - *Chosen Approach*: CameraX image analysis pipeline using computer vision techniques (luminance edge detection, contour corner localization, 8x8 adaptive threshold square sampling, and template piece classification) coupled with an immediate interactive verification sheet.
  - *Why*: Provides high speed and zero-latency feedback without massive multi-gigabyte neural weight downloads, while giving the user instant 1-tap correction for 100% accurate game reconstruction.
- **Decision 3: Local Memory Durability (Room + JSON)**:
  - *Chosen Approach*: Room SQLite database with transactional writes for persistent game history, settings, and opening knowledge, paired with in-memory volatile transposition tables that cleanly reset upon force-stop.
  - *Why*: Guarantees zero data loss for saved games while preventing runaway memory accumulation.
- **Decision 4: Optional Gemini AI Intelligence**:
  - *Chosen Approach*: Optional Gemini integration (using `gemini-3.8-flash` with Google Search grounding for chess opening theory and `gemini-4-argon` with high thinking for complex tactical reviews).
  - *Why*: Meets the user's explicit metadata requirements for Gemini intelligence, high thinking, search grounding, and image understanding without compromising the offline-first core promise.

---

### 4. Technical Architecture & Data Strategy

#### System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                           CHEASST APPLICATION                             │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│   ┌─────────────────────┐   ┌────────────────────┐   ┌────────────────┐   │
│   │   CameraX Scanner   │   │ Interactive Board  │   │   Engine Hub   │   │
│   │ (Live Feed + Vision)│   │ (Compose 8x8 Grid) │   │ (6 Modes/Speed)│   │
│   └──────────┬──────────┘   └─────────┬──────────┘   └───────┬────────┘   │
│              │                        │                      │            │
│              ▼                        ▼                      ▼            │
│   ┌───────────────────────────────────────────────────────────────────┐   │
│   │                        CheasstViewModel                           │   │
│   │   - Active FEN & Move History    - Search Depth & Multi-PV        │   │
│   │   - Engine Mode (Normal to CGM)  - Hardware Resource Metrics      │   │
│   └──────────────────────┬────────────────────────────────────┬───────┘   │
│                          │                                    │           │
│         ┌────────────────┴───────────────┐                    │           │
│         ▼                                ▼                    ▼           │
│   ┌──────────────────────────┐    ┌────────────────┐   ┌──────────────┐   │
│   │ Bitboard Calculation     │    │  Room Database │   │ Optional     │   │
│   │ Engine (Alpha-Beta,      │    │  (Saved Games, │   │ Gemini AI    │   │
│   │ Quiescence, Transposition│    │  FENs, Notes,  │   │ (Explanation,│   │
│   │ Table, 6 Mode Profiles)  │    │  Knowledge)    │   │ Search Tool) │   │
│   └──────────────────────────┘    └────────────────┘   └──────────────┘   │
└───────────────────────────────────────────────────────────────────────────┘
```

#### Core Components & File Structure Plan

1. **Model & Engine Core (`com.example.chess.engine`)**:
   - `Piece.kt`, `Square.kt`, `Move.kt`, `BoardState.kt`: Fast bitboard representations, FEN parser/generator, legal move generator (castling, en passant, promotions).
   - `ChessEngine.kt`: Multi-threaded evaluation engine, alpha-beta search, quiescence, heuristic evaluation (material + mobility + PST tables).
   - `EngineProfile.kt`: The 6 engine profiles (Normal, Pro, ProFast, Master, Grandmaster, Complex Grandmaster) with distinct search depths, time limits, and evaluation weights.
2. **Vision & Recognition (`com.example.chess.vision`)**:
   - `ChessboardDetector.kt`: Image luminance analysis, 8x8 square slicing, piece color & occupancy classification, confidence estimation.
   - `CameraXPreview.kt`: CameraX lifecycle-bound preview with live detection bounds and tap-to-freeze overlay.
3. **Database & Persistence (`com.example.chess.data`)**:
   - `AppDatabase.kt`, `SavedPositionDao.kt`, `SavedPositionEntity.kt`: Room database storing FEN, PGN, mode used, engine evaluation, notes, and timestamp.
   - `PreferencesRepository.kt`: Persistent engine and vision settings.
4. **AI & Explanations (`com.example.chess.ai`)**:
   - `GeminiChessAdvisor.kt`: Optional Gemini client for natural-language move explanations, strategic recommendations, and search-grounded opening reviews.
5. **UI & Presentation (`com.example.ui`)**:
   - `HomeScreen.kt`: Hero dashboard, mode indicator, hardware stats, quick access.
   - `CameraVisionScreen.kt`: Viewfinder, recognition overlay, manual correction bottom sheet.
   - `AnalysisScreen.kt`: 8x8 interactive board, eval bar, candidate moves, engine controls, AI explanation drawer.
   - `ModesScreen.kt`: Detailed switcher and explainer for the 6 modes.
   - `MemoryScreen.kt`: Saved games, FEN export/import, search.
   - `SettingsScreen.kt`: Hardware limits, thread allocation, calculation speed toggles, API keys.
   - `HardwareDashboard.kt`: Live CPU, RAM, and nodes/sec telemetry.
6. **Theme & Branding (`com.example.ui.theme`)**:
   - Custom palette with sleek obsidian (`#0A0D0E`), tactical emerald (`#00E676`), and tournament boards.

---

### Verification & Testing Plan

1. **Compilation Check**: Run `compile_applet` to guarantee complete error-free build.
2. **Engine Rules Test**: Unit tests verifying initial FEN setup, legal move generation (including castling, en passant, checks, and checkmate detection).
3. **Engine Evaluation Test**: Verify evaluation of classic tactical test positions (e.g. Scholar's mate, smothered mate, fork tactics) across different engine profiles.
4. **FEN Parsing & Generation**: Ensure round-trip fidelity between board state and FEN strings.
5. **Database Persistence**: Verify insertion and retrieval of saved chess positions and preferences in Room.
6. **UI & Navigation Verification**: Verify bottom navigation and back navigation across all 6 main screens with zero crashes.
