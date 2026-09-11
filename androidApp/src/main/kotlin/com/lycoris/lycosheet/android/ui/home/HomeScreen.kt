package com.lycoris.lycosheet.android.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lycoris.lycosheet.android.audio.AudioRecorderHelper
import com.lycoris.lycosheet.android.ui.components.PronunciationRecorder
import com.lycoris.lycosheet.audio.AudioPlayer
import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.presentation.home.HomeState
import com.lycoris.lycosheet.presentation.home.HomeViewModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
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

    val saveEnabled = !state.isLoading && when (state.cardType) {
        CardType.CLASSIC -> state.frontText.isNotBlank() && state.backText.isNotBlank()
        CardType.MULTIPLE_CHOICE -> state.frontText.isNotBlank() && state.backText.isNotBlank() &&
                (state.wrongChoice1.isNotBlank() || state.wrongChoice2.isNotBlank() || state.wrongChoice3.isNotBlank())
        CardType.FILL_IN -> state.frontText.isNotBlank() && state.backText.isNotBlank()
        CardType.LISTENING -> state.audioPath.isNotBlank() && state.backText.isNotBlank()
    }

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
                        modifier = Modifier.fillMaxWidth().menuAnchor()
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
                CardType.MULTIPLE_CHOICE to "MC",
                CardType.FILL_IN to "Fill-in",
                CardType.LISTENING to "Listening"
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
                CardType.LISTENING -> ListeningCardForm(state, viewModel)
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

// ── Forms ──────────────────────────────────────────────────────────────────────

@Composable
private fun ClassicCardForm(state: HomeState, viewModel: HomeViewModel) {
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
    PronunciationRecorder(
        pronunciationPath = state.pronunciationPath,
        onPathChanged = viewModel::onPronunciationPathChanged,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MultipleChoiceCardForm(state: HomeState, viewModel: HomeViewModel) {
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
    PronunciationRecorder(
        pronunciationPath = state.pronunciationPath,
        onPathChanged = viewModel::onPronunciationPathChanged,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FillInCardForm(state: HomeState, viewModel: HomeViewModel) {
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
    PronunciationRecorder(
        pronunciationPath = state.pronunciationPath,
        onPathChanged = viewModel::onPronunciationPathChanged,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ListeningCardForm(state: HomeState, viewModel: HomeViewModel) {
    val context = LocalContext.current
    val recorder: AudioRecorderHelper = koinInject()
    val player: AudioPlayer = koinInject()

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    // Tick the timer while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Stop player/recorder when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) recorder.stopRecording()
            player.stop()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recorder.startRecording()
            isRecording = true
        }
    }

    fun startRecording() {
        val path = recorder.startRecording()
        viewModel.onAudioPathChanged(path)
        isRecording = true
    }

    fun stopRecording() {
        recorder.stopRecording()
        isRecording = false
    }

    // Recording control card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Record / Stop button
                FilledIconButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) startRecording()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record"
                    )
                }

                Column {
                    Text(
                        when {
                            isRecording -> "Recording… ${elapsedSeconds}s"
                            state.audioPath.isNotBlank() -> "Recorded ✓"
                            else -> "Tap to record audio"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.audioPath.isNotBlank() && !isRecording) {
                        Text(
                            "Tap 🔁 to re-record",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Preview playback — only shown when a clip exists
            if (state.audioPath.isNotBlank() && !isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPlaying) {
                                player.stop()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                player.play(state.audioPath) { isPlaying = false }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPlaying) "Stop" else "Preview")
                    }
                }
            }
        }
    }

    // Optional context / hint
    OutlinedTextField(
        value = state.frontText,
        onValueChange = viewModel::onFrontChanged,
        label = { Text("Hint (optional)") },
        placeholder = { Text("e.g. What animal?") },
        supportingText = { Text("Shown before the audio plays") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Transcript / correct answer
    OutlinedTextField(
        value = state.backText,
        onValueChange = viewModel::onBackChanged,
        label = { Text("Transcript / answer") },
        placeholder = { Text("What the audio says") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    // Pronunciation clip is separate from the main audio (which is extraData for LISTENING)
    PronunciationRecorder(
        pronunciationPath = state.pronunciationPath,
        onPathChanged = viewModel::onPronunciationPathChanged,
        modifier = Modifier.fillMaxWidth()
    )
}
