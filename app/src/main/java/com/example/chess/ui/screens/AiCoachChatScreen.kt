package com.example.chess.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ui.CheasstViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val isSystemKnowledge: Boolean = false
)

@Composable
fun AiCoachChatScreen(
    viewModel: CheasstViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val board by viewModel.board.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "ai",
                text = "Welcome to the Cheasst Grandmaster AI Coach. I'm connected to Stockfish 16, Leela Chess Zero (Lc0) positional neural networks, and the Gemini strategic reasoning model. Ask me about checkmate rules, opening refutations, or tap an option below to analyze your live position!"
            )
        )
    }

    val quickQuestions = listOf(
        "Explain Checkmate & Stalemate (Wikipedia)",
        "How does En Passant work?",
        "Explain Smothered Mate tactic",
        "Stockfish vs Leela Chess Zero (Lc0)",
        "A.C.A.S Anti-Cheat accuracy",
        "Analyze current live board position"
    )

    fun sendUserMessage(query: String) {
        if (query.isBlank()) return
        messages.add(ChatMessage(sender = "user", text = query))
        inputMessage = ""

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)

            // Generate contextual grandmaster answer
            val reply = when {
                query.contains("Checkmate", ignoreCase = true) || query.contains("Wikipedia", ignoreCase = true) -> {
                    """
                    ♟️ CHECKMATE & STALEMATE RULES (FIDE & Wikipedia Standard):
                    • Checkmate (from Persian 'shāh māt', 'the King is helpless'): The King is in check (under immediate attack) and there is no legal move to escape the threat. The game ends immediately; the player delivering checkmate wins.
                    • Three ways to escape check:
                      1. Capture the attacking piece.
                      2. Interpose (block) a piece between the attacker and King (cannot block knights).
                      3. Move the King to an unattacked adjacent square.
                    • Stalemate: The player whose turn it is has NO legal moves and their King is NOT in check. Stalemate is an immediate draw (1/2 - 1/2).
                    """.trimIndent()
                }
                query.contains("En Passant", ignoreCase = true) -> {
                    """
                    ⚡ EN PASSANT ('In Passing'):
                    A special pawn capture rule occurring when a pawn moves two squares forward from its starting square, landing directly adjacent (same rank) to an enemy pawn.
                    • The enemy pawn may capture it 'in passing' as if it had only advanced one square.
                    • CRITICAL: Must be played on the very next turn immediately following the two-square advance, otherwise the right is permanently lost!
                    """.trimIndent()
                }
                query.contains("Smothered", ignoreCase = true) -> {
                    """
                    🐴 SMOTHERED CHECKMATE:
                    A tactical checkmate delivered exclusively by a Knight where the defending King cannot move because it is completely boxed in ('smothered') by its own friendly pieces!
                    • Famous in the Blackburne Shilling Gambit:
                      1. e4 e5 2. Nf3 Nc6 3. Bc4 Nd4 4. Nxe5? Qg5! 5. Nxf7 Qxg2 6. Rf1 Qxe4+ 7. Be2 Nf3#!!
                    The White King on e1 is surrounded by its own Queen, Bishop, and Rook, leaving no escape!
                    """.trimIndent()
                }
                query.contains("Stockfish", ignoreCase = true) || query.contains("Lc0", ignoreCase = true) -> {
                    """
                    ⚔️ STOCKFISH 16 VS LEELA CHESS ZERO (Lc0):
                    • Stockfish: Alpha-Beta minimax search with NNUE (efficiently updatable neural networks). Calculates 40-50M nodes/sec with superhuman tactical precision and deep brute force calculations.
                    • Leela Chess Zero (Lc0): Deep convolutional policy and value networks trained via self-play (AlphaZero architecture). Excels in deep positional intuition, intuitive piece sacrifices, and subtle pawn structures.
                    • Cheasst Hybrid Mode fuses both: Lc0 evaluates positional candidate moves, while Stockfish calculates the tactical branch safety.
                    """.trimIndent()
                }
                query.contains("A.C.A.S", ignoreCase = true) || query.contains("Anti-Cheat", ignoreCase = true) -> {
                    """
                    🛡️ A.C.A.S & CHESALYSER ACCURACY METRICS:
                    • ACPL (Average Centipawn Loss): Measures how closely moves align with the theoretical optimum. Human Grandmasters average 15-25 ACPL; engines average < 8 ACPL.
                    • Move Precision: Categorizes moves into Brilliant (!!), Great (!), Best, Inaccuracy, Mistake, or Blunder.
                    • Anti-Cheat Flagging: Analyzes move time variance, deep tactical complexity, and centipawn consistency.
                    """.trimIndent()
                }
                query.contains("Analyze current", ignoreCase = true) || query.contains("board", ignoreCase = true) -> {
                    val fen = board.toFen()
                    val best = analysisResult?.bestMove?.san ?: "N/A"
                    val eval = analysisResult?.formattedEval ?: "+0.00"
                    val turn = if (board.activeColor == com.example.chess.model.PieceColor.WHITE) "White" else "Black"
                    """
                    🔍 CURRENT BOARD ANALYSIS:
                    • Active Turn: $turn to move
                    • FEN: $fen
                    • Best Move: $best
                    • Position Evaluation: $eval
                    • Positional Recommendation: Focus on controlling central files and maintaining king shelter before launching an assault.
                    """.trimIndent()
                }
                else -> {
                    "Grandmaster Coach Advice: In this position, maintain dynamic piece activity and calculate checks, captures, and threats (CCT) before committing to pawn breaks. What specific tactical line would you like to explore?"
                }
            }

            messages.add(ChatMessage(sender = "ai", text = reply))
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("ai_coach_chat_screen")
    ) {
        // Coach Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedSlate)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TacticalEmerald.copy(alpha = 0.2f))
                        .border(1.5.dp, TacticalEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Coach",
                        tint = TacticalEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Grandmaster AI Coach",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gemini + Stockfish 16 + Lc0 Neural Policy",
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                }
            }

            // Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F2618))
                    .border(1.dp, TacticalEmerald, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("● ONLINE", color = TacticalEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickQuestions) { question ->
                SuggestionChip(
                    onClick = { sendUserMessage(question) },
                    label = { Text(question, fontSize = 11.sp, color = Color.White) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ElevatedSlate),
                    border = BorderStroke(1.dp, CardBorderSlate)
                )
            }
        }

        // Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 16.dp
                                )
                            )
                            .background(if (isUser) Color(0xFF1E3A2F) else ElevatedSlate)
                            .border(
                                1.dp,
                                if (isUser) TacticalEmerald.copy(alpha = 0.5f) else CardBorderSlate,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "You" else "Cheasst Coach",
                                    color = if (isUser) TacticalEmerald else CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = msg.timestamp,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedSlate)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Ask about chess moves, rules, tactics...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalEmerald,
                    unfocusedBorderColor = CardBorderSlate,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            IconButton(
                onClick = { sendUserMessage(inputMessage) },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(TacticalEmerald)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
