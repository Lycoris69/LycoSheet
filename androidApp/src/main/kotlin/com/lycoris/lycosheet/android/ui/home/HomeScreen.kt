package com.lycoris.lycosheet.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.presentation.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    // Show snackbar on save
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.cardSaved) {
        if (state.cardSaved) {
            snackbarHostState.showSnackbar("Card saved!")
            viewModel.onCardSavedConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Card") })
        },
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

            // Deck selector / name field
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

            // Front face
            OutlinedTextField(
                value = state.frontText,
                onValueChange = viewModel::onFrontChanged,
                label = { Text("Front (recto)") },
                placeholder = { Text("Word, phrase, question…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 5
            )

            // Back face
            OutlinedTextField(
                value = state.backText,
                onValueChange = viewModel::onBackChanged,
                label = { Text("Back (verso)") },
                placeholder = { Text("Definition, translation, answer…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 5
            )

            Button(
                onClick = viewModel::saveCard,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.frontText.isNotBlank() && state.backText.isNotBlank() && !state.isLoading
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
