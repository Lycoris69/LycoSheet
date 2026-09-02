package com.lycoris.lycosheet.android.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.presentation.deck.DeckDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    onBack: () -> Unit,
    onStartStudy: () -> Unit,
    viewModel: DeckDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingCard by remember { mutableStateOf<Card?>(null) }

    LaunchedEffect(deckId) { viewModel.loadDeck(deckId) }

    LaunchedEffect(state.cardSaved) {
        if (state.cardSaved) {
            snackbarHostState.showSnackbar("Card updated!")
            viewModel.onCardSavedConsumed()
        }
    }

    editingCard?.let { card ->
        EditCardDialog(
            card = card,
            onDismiss = { editingCard = null },
            onConfirm = { front, back ->
                viewModel.updateCard(card.copy(front = front, back = back))
                editingCard = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.deck?.name ?: "Deck")
                        val count = state.cards.size
                        if (!state.isLoading) {
                            Text(
                                "$count card${if (count == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onStartStudy,
                        enabled = state.cards.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Study deck",
                            tint = if (state.cards.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.cards.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No cards yet.\nCreate some from the Home tab!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cards, key = { it.id }) { card ->
                    CardGridItem(
                        card = card,
                        onEdit = { editingCard = card },
                        onDelete = { viewModel.deleteCard(card.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardGridItem(
    card: Card,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete card?") },
            text = { Text("This card will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Seen badge
            if (card.seenCount > 0) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "seen ${card.seenCount}×",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            // Front
            Text(
                card.front,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Back
            Text(
                card.back,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit card",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete card",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (front: String, back: String) -> Unit
) {
    var front by remember(card.id) { mutableStateOf(card.front) }
    var back by remember(card.id) { mutableStateOf(card.back) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (recto)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (verso)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(front, back) },
                enabled = front.isNotBlank() && back.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
