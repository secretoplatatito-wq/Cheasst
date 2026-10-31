package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.chess.data.SavedPositionEntity
import com.example.chess.ui.CheasstViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(
    viewModel: CheasstViewModel,
    onNavigateToAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedPositions by viewModel.savedPositions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hardwareMetrics by viewModel.hardwareMetrics.collectAsState()

    var positionToDelete by remember { mutableStateOf<SavedPositionEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
            .testTag("memory_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOCAL PERSISTENT MEMORY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saved Games & Positions",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${savedPositions.size} saved",
                color = TacticalEmerald,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by title, notes, or FEN...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_memory_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalEmerald,
                unfocusedBorderColor = CardBorderSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = ElevatedSlate,
                unfocusedContainerColor = ElevatedSlate
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // List of Saved Positions
        if (savedPositions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "No saved positions",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No saved positions in memory",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Save any game or position from the Analysis board",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedPositions, key = { it.id }) { position ->
                    SavedPositionCard(
                        position = position,
                        onLoad = {
                            viewModel.applyDetectedFen(position.fen)
                            onNavigateToAnalysis()
                        },
                        onDelete = { positionToDelete = position }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (positionToDelete != null) {
        AlertDialog(
            onDismissRequest = { positionToDelete = null },
            containerColor = ElevatedSlate,
            title = { Text("Delete Position", color = Color.White) },
            text = { Text("Are you sure you want to remove \"${positionToDelete?.title}\" from memory?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        positionToDelete?.let { viewModel.deleteSavedPosition(it) }
                        positionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { positionToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SavedPositionCard(
    position: SavedPositionEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }
    val dateStr = remember(position.timestamp) { dateFormat.format(Date(position.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSlate)
            .border(1.dp, CardBorderSlate, RoundedCornerShape(12.dp))
            .clickable { onLoad() }
            .padding(14.dp)
            .testTag("saved_position_card_${position.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = position.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$dateStr · ${position.modeUsed} Mode",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // FEN string preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianBg)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = position.fen,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        if (position.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = position.notes,
                color = TacticalAmber,
                fontSize = 12.sp,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Eval: ${position.evaluation} | Best: ${position.bestMove}",
                color = TacticalEmerald,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Tap to Load ➔",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
