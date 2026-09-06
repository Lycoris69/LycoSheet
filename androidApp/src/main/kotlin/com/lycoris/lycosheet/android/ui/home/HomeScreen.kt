package com.lycoris.lycosheet.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.presentation.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.cardSaved) {
        if (state.cardSaved) {
            snackbarHostState.showSnackbar("Card saved!")
            viewModel.onCardSavedConsumed()
        }
    }

    val saveEnabled = state.frontText.isNotBlank() && state.backText.isNotBlank() && !state.isLoading &&
            (state.cardType != CardType.MULTIPLE_CHOICE ||
                    state.wrongChoice1.isNotBlank() || state.wrongChoice2.isNotBlank() || state.wrongChoice3.isNotBlank())

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Card") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Deck selector ──────────────────────────────────────────────
            if (state.availableDecks.isEmpty()) {
                OutlinedTextField(
                    value = state.deckName,
                    onValueChange = viewModel::onDeckNameChanged,
                    label = { Text("Deck name") },
                    placeholder = { Text("My Vocabulary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                var expanded by remember { mutableStateOf(false) }
                val selectedDeck = state.availableDecks.find { it.id == state.selectedDeckId }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedDeck?.name ?: "Create new deck…",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Deck") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Create new deck…") },
                            onClick = { viewModel.onDeckSelected(null); expanded = false }
                        )
                        state.availableDecks.forEach { deck ->
                            DropdownMenuItem(
                                text = { Text(deck.name) },
                                onClick = { viewModel.onDeckSelected(deck.id); expanded = false }
                            )
                        }
                    }
                }
                if (state.selectedDeckId == null) {
                    OutlinedTextField(
                        value = state.deckName,
                        onValueChange = viewModel::onDeckNameChanged,
                        label = { Text("New deck name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ── Card type selector ─────────────────────────────────────────
            val cardTypes = listOf(
                CardType.CLASSIC to "Classic",
                CardType.MULTIPLE_CHOICE to "Multiple Choice",
                CardType.FILL_IN to "Fill-in"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                cardTypes.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = state.cardType == type,
                        onClick = { viewModel.onCardTypeChanged(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = cardTypes.size),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // ── Type-specific form ─────────────────────────────────────────
            when (state.cardType) {
                CardType.CLASSIC -> ClassicCardForm(state, viewModel)
                CardType.MULTIPLE_CHOICE -> MultipleChoiceCardForm(state, viewModel)
                CardType.FILL_IN -> FillInCardForm(state, viewModel)
            }

            // ── Save button ────────────────────────────────────────────────
            Button(
                onClick = viewModel::saveCard,
                modifier = Modifier.fillMaxWidth(),
                enabled = saveEnabled
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save card")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClassicCardForm(state: com.lycoris.lycosheet.presentation.home.HomeState, viewModel: HomeViewModel) {
    OutlinedTextField(
        value = state.frontText,
        onValueChange = viewModel::onFrontChanged,
        label = { Text("Front (recto)") },
        placeholder = { Text("Word, phrase, question…") },
        modifier = Modifier.fillMaxWidth().height(130.dp),
        maxLines = 5
    )
    OutlinedTextField(
        value = state.backText,
        onValueChange = viewModel::onBackChanged,
        label = { Text("Back (verso)") },
        placeholder = { Text("Definition, translation, answer…") },
        modifier = Modifier.fillMaxWidth().height(130.dp),
        maxLines = 5
    )
}

@Composable
private fun MultipleChoiceCardForm(state: com.lycoris.lycosheet.presentation.home.HomeState, viewModel: HomeViewModel) {
    OutlinedTextField(
        value = state.frontText,
        onValueChange = viewModel::onFrontChanged,
        label = { Text("Question") },
        placeholder = { Text("What is the capital of France?") },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        maxLines = 4
    )
    OutlinedTextField(
        value = state.backText,
        onValueChange = viewModel::onBackChanged,
        label = { Text("Correct answer") },
        placeholder = { Text("Paris") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Text(
        "Wrong choices (add at least one)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = state.wrongChoice1,
        onValueChange = viewModel::onWrongChoice1Changed,
        label = { Text("Wrong choice 1") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = state.wrongChoice2,
        onValueChange = viewModel::onWrongChoice2Changed,
        label = { Text("Wrong choice 2") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = state.wrongChoice3,
        onValueChange = viewModel::onWrongChoice3Changed,
        label = { Text("Wrong choice 3") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun FillInCardForm(state: com.lycoris.lycosheet.presentation.home.HomeState, viewModel: HomeViewModel) {
    OutlinedTextField(
        value = state.frontText,
        onValueChange = viewModel::onFrontChanged,
        label = { Text("Sentence") },
        placeholder = { Text("The capital of France is ___.") },
        supportingText = { Text("Use ___ to mark the blank") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        maxLines = 5
    )
    OutlinedTextField(
        value = state.backText,
        onValueChange = viewModel::onBackChanged,
        label = { Text("Correct answer") },
        placeholder = { Text("Paris") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
