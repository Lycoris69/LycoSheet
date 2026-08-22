package com.lycoris.lycosheet.android.ui.study

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.ui.components.FlashCard
import com.lycoris.lycosheet.presentation.study.StudyViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    deckId: Long,
    onBack: () -> Unit,
    viewModel: StudyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(deckId) { viewModel.loadDeck(deckId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::restartSession) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                state.isComplete -> CompletionScreen(onRestart = viewModel::restartSession, onBack = onBack)

                state.session != null -> {
                    val session = state.session!!
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { session.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${session.currentIndex + 1} / ${session.cards.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Card — tap to flip
                        session.currentCard?.let { card ->
                            FlashCard(
                                front = card.front,
                                back = card.back,
                                isFrontVisible = session.isFrontVisible,
                                onFlip = viewModel::flipCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )
                        }

                        Text(
                            "Tap the card to flip",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = viewModel::previousCard,
                                enabled = session.hasPrevious
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Previous")
                            }
                            Button(onClick = viewModel::nextCard) {
                                Text(if (session.hasNext) "Next" else "Finish")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionScreen(onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Session complete! 🎉", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("You reviewed all cards in this deck.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Study again") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to library") }
    }
}
