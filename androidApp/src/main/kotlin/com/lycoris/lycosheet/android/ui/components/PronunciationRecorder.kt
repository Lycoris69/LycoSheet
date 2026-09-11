package com.lycoris.lycosheet.android.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.lycoris.lycosheet.audio.AudioPlayer
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Optional pronunciation recorder shown at the bottom of every card creation form.
 * Collapsed by default so it doesn't clutter simple Classic cards.
 *
 * [pronunciationPath] — current value (empty = no clip recorded yet)
 * [onPathChanged]     — called with the new file path after recording stops,
 *                       or with "" when the clip is discarded
 */
@Composable
fun PronunciationRecorder(
    pronunciationPath: String,
    onPathChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorder: AudioRecorderHelper = koinInject()
    val player: AudioPlayer = koinInject()

    var expanded by remember { mutableStateOf(pronunciationPath.isNotBlank()) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) { delay(1000); elapsedSeconds++ }
        }
    }

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
            val path = recorder.startRecording()
            onPathChanged(path)
            isRecording = true
        }
    }

    fun startRecording() {
        val path = recorder.startRecording()
        onPathChanged(path)
        isRecording = true
    }

    Column(modifier = modifier) {
        // ── Header row — always visible ────────────────────────────────────
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    pronunciationPath.isNotBlank() -> "Pronunciation recorded ✓"
                    else -> "Add pronunciation (optional)"
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        // ── Expanded panel ─────────────────────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecording)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Record / Stop
                        FilledIconButton(
                            onClick = {
                                if (isRecording) {
                                    recorder.stopRecording()
                                    isRecording = false
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            when {
                                isRecording -> "Recording… ${elapsedSeconds}s"
                                pronunciationPath.isNotBlank() -> "Clip recorded — tap to re-record"
                                else -> "Tap to record pronunciation"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Playback + discard — only when a clip exists and not recording
                    if (pronunciationPath.isNotBlank() && !isRecording) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isPlaying) { player.stop(); isPlaying = false }
                                    else { isPlaying = true; player.play(pronunciationPath) { isPlaying = false } }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Preview")
                            }
                            OutlinedButton(
                                onClick = {
                                    player.stop(); isPlaying = false
                                    onPathChanged("")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Discard") }
                        }
                    }
                }
            }
        }
    }
}
