package com.lycoris.lycosheet.android.ui.study

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.ui.components.FlashCard
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.model.CardGrade
import com.lycoris.lycosheet.data.model.CardType
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
            modifier = Modifier.fillMaxSize().padding(padding),
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
                        modifier = Modifier.fillMaxSize().padding(20.dp),
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

                        // Card content — keyed by currentIndex so local state resets each card
                        session.currentCard?.let { card ->
                            key(session.currentIndex) {
                                when (card.cardType) {
                                    CardType.CLASSIC -> ClassicCard(
                                        card = card,
                                        session = session,
                                        viewModel = viewModel
                                    )
                                    CardType.MULTIPLE_CHOICE -> MultipleChoiceCard(
                                        card = card,
                                        onGrade = { viewModel.gradeCard(it) }
                                    )
                                    CardType.FILL_IN -> FillInCard(
                                        card = card,
                                        onGrade = { viewModel.gradeCard(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Classic ────────────────────────────────────────────────────────────────────

@Composable
private fun ClassicCard(
    card: Card,
    session: com.lycoris.lycosheet.data.model.StudySession,
    viewModel: StudyViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FlashCard(
                front = card.front,
                back = card.back,
                isFrontVisible = session.isFrontVisible,
                onFlip = viewModel::flipCard,
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
            if (card.seenCount > 0) SeenBadge(card.seenCount, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }

        AnimatedContent(
            targetState = session.isFrontVisible,
            transitionSpec = {
                fadeIn() + slideInVertically { it / 2 } togetherWith fadeOut() + slideOutVertically { it / 2 }
            },
            label = "bottom_area"
        ) { frontVisible ->
            if (frontVisible) {
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
                GradeRow(onGrade = { viewModel.gradeCard(it) })
            }
        }
    }
}

// ── Multiple Choice ────────────────────────────────────────────────────────────

@Composable
private fun MultipleChoiceCard(card: Card, onGrade: (CardGrade) -> Unit) {
    // Shuffle choices once per card presentation
    val choices = remember {
        (listOf(card.back) + card.extraData.split("|").filter { it.isNotBlank() }).shuffled()
    }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    val revealed = selectedAnswer != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    card.front,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                if (card.seenCount > 0) SeenBadge(card.seenCount, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }

        // Choice buttons
        choices.forEach { choice ->
            val isCorrect = choice == card.back
            val isSelected = choice == selectedAnswer
            val containerColor = when {
                !revealed -> MaterialTheme.colorScheme.surface
                isCorrect -> Color(0xFF2E7D32)   // green — always highlight correct
                isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val contentColor = when {
                !revealed -> MaterialTheme.colorScheme.onSurface
                isCorrect -> Color.White
                isSelected -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            OutlinedButton(
                onClick = { if (!revealed) selectedAnswer = choice },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                Text(choice, textAlign = TextAlign.Center)
            }
        }

        // Grade buttons appear after a choice is made
        AnimatedVisibility(visible = revealed) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                Text(
                    if (selectedAnswer == card.back) "Correct! ✓" else "Incorrect — answer: ${card.back}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedAnswer == card.back) Color(0xFF2E7D32)
                    else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                GradeRow(onGrade = onGrade)
            }
        }
    }
}

// ── Fill-in ────────────────────────────────────────────────────────────────────

@Composable
private fun FillInCard(card: Card, onGrade: (CardGrade) -> Unit) {
    var userInput by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Prompt card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    card.front,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                if (card.seenCount > 0) SeenBadge(card.seenCount, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }

        // Input
        OutlinedTextField(
            value = userInput,
            onValueChange = { if (!revealed) userInput = it },
            label = { Text("Your answer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !revealed,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (userInput.isNotBlank()) revealed = true })
        )

        if (!revealed) {
            Button(
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = userInput.isNotBlank()
            ) { Text("Check") }
        } else {
            // Reveal result
            val correct = userInput.trim().equals(card.back.trim(), ignoreCase = true)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (correct) Color(0xFF2E7D32).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (correct) "Correct! ✓" else "Not quite",
                        fontWeight = FontWeight.Bold,
                        color = if (correct) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                    Text("Your answer: $userInput", style = MaterialTheme.typography.bodySmall)
                    if (!correct) {
                        Text(
                            "Correct: ${card.back}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            GradeRow(onGrade = onGrade)
        }
    }
}

// ── Shared ─────────────────────────────────────────────────────────────────────

@Composable
private fun GradeRow(onGrade: (CardGrade) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            OutlinedButton(
                onClick = { onGrade(CardGrade.AGAIN) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Again", fontWeight = FontWeight.SemiBold)
                    Text("< 1 min", style = MaterialTheme.typography.labelSmall)
                }
            }
            OutlinedButton(
                onClick = { onGrade(CardGrade.OK) },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ok", fontWeight = FontWeight.SemiBold)
                    Text("got it", style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(
                onClick = { onGrade(CardGrade.GOOD) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Good", fontWeight = FontWeight.SemiBold)
                    Text("easy", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SeenBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = "seen $count×",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Completion ─────────────────────────────────────────────────────────────────

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
        Text("Session complete! 🎉", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

        if (grades.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GradeStat("Again", again, MaterialTheme.colorScheme.error)
                    GradeStat("Ok",    ok,    MaterialTheme.colorScheme.onSurface)
                    GradeStat("Good",  good,  Color(0xFF388E3C))
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
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
