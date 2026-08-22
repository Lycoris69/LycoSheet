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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.ui.components.FlashCard
import com.lycoris.lycosheet.data.model.CardGrade
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

                state.isComplete -> CompletionScreen(
                    grades = state.grades,
                    onRestart = viewModel::restartSession,
                    onBack = onBack
                )

                state.session != null -> {
                    val session = state.session!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Progress
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
                            Box(modifier = Modifier.fillMaxWidth()) {
                                FlashCard(
                                    front = card.front,
                                    back = card.back,
                                    isFrontVisible = session.isFrontVisible,
                                    onFlip = viewModel::flipCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                )
                                if (card.seenCount > 0) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        tonalElevation = 2.dp
                                    ) {
                                        Text(
                                            text = "seen ${card.seenCount}×",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom area: hint or grade buttons depending on flip state
                        AnimatedContent(
                            targetState = session.isFrontVisible,
                            transitionSpec = {
                                fadeIn() + slideInVertically { it / 2 } togetherWith
                                        fadeOut() + slideOutVertically { it / 2 }
                            },
                            label = "bottom_area"
                        ) { frontVisible ->
                            if (frontVisible) {
                                // Front: navigation row
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Tap the card to reveal the answer",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
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
                                        OutlinedButton(onClick = viewModel::nextCard) {
                                            Text("Skip")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                        }
                                    }
                                }
                            } else {
                                // Back: grade buttons
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "How did you do?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Again — red tint
                                        OutlinedButton(
                                            onClick = { viewModel.gradeCard(CardGrade.AGAIN) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Again", fontWeight = FontWeight.SemiBold)
                                                Text("< 1 min", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        // Ok — neutral
                                        OutlinedButton(
                                            onClick = { viewModel.gradeCard(CardGrade.OK) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Ok", fontWeight = FontWeight.SemiBold)
                                                Text("got it", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        // Good — green tint
                                        Button(
                                            onClick = { viewModel.gradeCard(CardGrade.GOOD) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF388E3C)
                                            )
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Good", fontWeight = FontWeight.SemiBold)
                                                Text("easy", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    grades: List<CardGrade>,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val again = grades.count { it == CardGrade.AGAIN }
    val ok    = grades.count { it == CardGrade.OK }
    val good  = grades.count { it == CardGrade.GOOD }

    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Session complete! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        if (grades.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GradeStat(label = "Again", count = again, color = MaterialTheme.colorScheme.error)
                    GradeStat(label = "Ok",    count = ok,    color = MaterialTheme.colorScheme.onSurface)
                    GradeStat(label = "Good",  count = good,  color = Color(0xFF388E3C))
                }
            }
        }

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Study again") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to library") }
    }
}

@Composable
private fun GradeStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
