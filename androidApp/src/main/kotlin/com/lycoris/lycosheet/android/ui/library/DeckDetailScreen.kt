package com.lycoris.lycosheet.android.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Stop
import com.lycoris.lycosheet.android.ui.components.IpaLookupField
import com.lycoris.lycosheet.android.ui.components.PronunciationRecorder
import com.lycoris.lycosheet.android.ui.study.PronunciationButton
import com.lycoris.lycosheet.audio.AudioPlayer
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.presentation.deck.DeckDetailViewModel
import org.koin.compose.koinInject
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
            onConfirm = { front, back, type, extra, pronunciationPath, phoneticText ->
                viewModel.updateCard(
                    card.copy(
                        front = front,
                        back = back,
                        cardType = type,
                        extraData = extra,
                        pronunciationPath = pronunciationPath,
                        phoneticText = phoneticText
                    )
                )
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
                        if (!state.isLoading) {
                            val count = state.cards.size
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
                modifier = Modifier.fillMaxSize().padding(padding),
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
    val player: AudioPlayer = koinInject()
    var isPlaying by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(card.id) {
        onDispose { player.stop() }
    }

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
            // Type badge + seen badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeBadge(card.cardType)
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
                }
            }

            Spacer(Modifier.height(6.dp))

            // Front
            Text(
                card.front,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Back / answer
            Text(
                card.back,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Extra info per type
            when (card.cardType) {
                CardType.MULTIPLE_CHOICE -> {
                    val wrongCount = card.extraData.split("|").count { it.isNotBlank() }
                    if (wrongCount > 0) {
                        Text(
                            "+ $wrongCount wrong option${if (wrongCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                CardType.LISTENING -> {
                    // Play button for audio preview
                    OutlinedButton(
                        onClick = {
                            if (isPlaying) {
                                player.stop(); isPlaying = false
                            } else {
                                isPlaying = true
                                player.play(card.extraData) { isPlaying = false }
                            }
                        },
                        enabled = card.extraData.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isPlaying) "Stop" else "Play",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pronunciation playback if clip exists
                if (card.pronunciationPath.isNotBlank()) {
                    PronunciationButton(
                        path = card.pronunciationPath,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: CardType) {
    val (label, container, content) = when (type) {
        CardType.CLASSIC -> Triple(
            "Classic",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        CardType.MULTIPLE_CHOICE -> Triple(
            "MC",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        CardType.FILL_IN -> Triple(
            "Fill-in",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        CardType.LISTENING -> Triple(
            "🎧 Audio",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}

// ── Edit dialog — non-destructive type switching ───────────────────────────────
// Each type keeps its own local state; switching the type selector only changes
// which fields are displayed, never clears data for another type.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (front: String, back: String, type: CardType, extra: String, pronunciationPath: String, phoneticText: String) -> Unit
) {
    var currentType by remember(card.id) { mutableStateOf(card.cardType) }

    // Classic state
    var classicFront by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.CLASSIC) card.front else "")
    }
    var classicBack by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.CLASSIC) card.back else "")
    }

    // Multiple-choice state
    var mcFront by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.MULTIPLE_CHOICE) card.front else "")
    }
    var mcBack by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.MULTIPLE_CHOICE) card.back else "")
    }
    val mcWrong = remember(card.id) {
        val choices = if (card.cardType == CardType.MULTIPLE_CHOICE)
            card.extraData.split("|") else emptyList()
        mutableStateListOf(
            choices.getOrElse(0) { "" },
            choices.getOrElse(1) { "" },
            choices.getOrElse(2) { "" }
        )
    }

    // Fill-in state
    var fillFront by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.FILL_IN) card.front else "")
    }
    var fillBack by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.FILL_IN) card.back else "")
    }

    // Listening state — audio path cannot be re-recorded in the dialog (read-only)
    var listeningHint by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.LISTENING) card.front else "")
    }
    var listeningBack by remember(card.id) {
        mutableStateOf(if (card.cardType == CardType.LISTENING) card.back else "")
    }

    // Pronunciation — shared across all types; pre-filled from card
    var pronunciationPath by remember(card.id) { mutableStateOf(card.pronunciationPath) }

    // Phonetic text — editable, shared across all non-Listening types
    var phoneticText by remember(card.id) { mutableStateOf(card.phoneticText) }
    var isLookingUpIpa by remember(card.id) { mutableStateOf(false) }

    val saveEnabled = when (currentType) {
        CardType.CLASSIC -> classicFront.isNotBlank() && classicBack.isNotBlank()
        CardType.MULTIPLE_CHOICE -> mcFront.isNotBlank() && mcBack.isNotBlank() &&
                (mcWrong[0].isNotBlank() || mcWrong[1].isNotBlank() || mcWrong[2].isNotBlank())
        CardType.FILL_IN -> fillFront.isNotBlank() && fillBack.isNotBlank()
        CardType.LISTENING -> listeningBack.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit card") },
        text = {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                val types = listOf(
                    CardType.CLASSIC to "Classic",
                    CardType.MULTIPLE_CHOICE to "MC",
                    CardType.FILL_IN to "Fill-in",
                    CardType.LISTENING to "🎧"
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = currentType == type,
                            onClick = { currentType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Type-specific fields
                when (currentType) {
                    CardType.CLASSIC -> {
                        OutlinedTextField(
                            value = classicFront, onValueChange = { classicFront = it },
                            label = { Text("Front (recto)") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 4
                        )
                        OutlinedTextField(
                            value = classicBack, onValueChange = { classicBack = it },
                            label = { Text("Back (verso)") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 4
                        )
                    }
                    CardType.MULTIPLE_CHOICE -> {
                        OutlinedTextField(
                            value = mcFront, onValueChange = { mcFront = it },
                            label = { Text("Question") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 3
                        )
                        OutlinedTextField(
                            value = mcBack, onValueChange = { mcBack = it },
                            label = { Text("Correct answer") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = mcWrong[0], onValueChange = { mcWrong[0] = it },
                            label = { Text("Wrong choice 1") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = mcWrong[1], onValueChange = { mcWrong[1] = it },
                            label = { Text("Wrong choice 2") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = mcWrong[2], onValueChange = { mcWrong[2] = it },
                            label = { Text("Wrong choice 3") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                    CardType.FILL_IN -> {
                        OutlinedTextField(
                            value = fillFront, onValueChange = { fillFront = it },
                            label = { Text("Sentence") },
                            supportingText = { Text("Use ___ to mark the blank") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 4
                        )
                        OutlinedTextField(
                            value = fillBack, onValueChange = { fillBack = it },
                            label = { Text("Correct answer") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                    CardType.LISTENING -> {
                        // Audio cannot be re-recorded from the edit dialog —
                        // re-record by deleting and creating a new card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Audio clip is preserved. To replace it, delete this card and create a new one.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = listeningHint, onValueChange = { listeningHint = it },
                            label = { Text("Hint (optional)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = listeningBack, onValueChange = { listeningBack = it },
                            label = { Text("Transcript / answer") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }

                // IPA phonetics + pronunciation recorder — not applicable to Listening cards
                if (currentType != CardType.LISTENING) {
                    val lookupWord = when (currentType) {
                        CardType.CLASSIC          -> classicFront
                        CardType.MULTIPLE_CHOICE  -> mcBack
                        CardType.FILL_IN          -> fillBack
                        else                      -> ""
                    }
                    IpaLookupField(
                        lookupWord = lookupWord,
                        phoneticText = phoneticText,
                        onPhoneticChanged = { phoneticText = it },
                        onPronunciationDownloaded = { path ->
                            if (pronunciationPath.isBlank()) pronunciationPath = path
                        },
                        isLooking = isLookingUpIpa,
                        onLookupStarted = { isLookingUpIpa = true },
                        onLookupFinished = { ipa, _, _ ->
                            isLookingUpIpa = false
                            if (ipa != null) phoneticText = ipa
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    PronunciationRecorder(
                        pronunciationPath = pronunciationPath,
                        onPathChanged = { pronunciationPath = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = saveEnabled,
                onClick = {
                    val (front, back, extra) = when (currentType) {
                        CardType.CLASSIC -> Triple(classicFront, classicBack, "")
                        CardType.MULTIPLE_CHOICE -> Triple(
                            mcFront, mcBack,
                            mcWrong.filter { it.isNotBlank() }.joinToString("|")
                        )
                        CardType.FILL_IN -> Triple(fillFront, fillBack, "")
                        // Keep original audio path (extraData) unchanged
                        CardType.LISTENING -> Triple(listeningHint, listeningBack, card.extraData)
                    }
                    onConfirm(front, back, currentType, extra, pronunciationPath, phoneticText)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
