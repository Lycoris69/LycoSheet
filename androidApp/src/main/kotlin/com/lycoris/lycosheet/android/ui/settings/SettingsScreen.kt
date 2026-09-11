package com.lycoris.lycosheet.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.util.IpaLibraryManager
import com.lycoris.lycosheet.presentation.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                SettingsToggleRow(
                    title = "Dark theme",
                    subtitle = "Use dark colours throughout the app",
                    checked = state.isDarkTheme,
                    onToggle = viewModel::toggleDarkTheme
                )
            }
            item { HorizontalDivider() }
            item {
                Text(
                    "Study",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsToggleRow(
                    title = "Shuffle cards",
                    subtitle = "Randomise card order at the start of each session",
                    checked = state.shuffleByDefault,
                    onToggle = viewModel::toggleShuffle
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show progress bar",
                    subtitle = "Display session progress while studying",
                    checked = state.showProgressBar,
                    onToggle = viewModel::toggleProgressBar
                )
            }
            item { HorizontalDivider() }
            item {
                Text(
                    "Pronunciation library",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            item { IpaLibraryCard() }
        }
    }
}

// ── IPA Library Card ───────────────────────────────────────────────────────────

@Composable
private fun IpaLibraryCard() {
    val library: IpaLibraryManager = koinInject()
    val scope = rememberCoroutineScope()

    var isDownloaded by remember { mutableStateOf(library.isDownloaded) }
    var fileSizeMb by remember { mutableStateOf(library.fileSizeBytes / 1_000_000f) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun refresh() {
        isDownloaded = library.isDownloaded
        fileSizeMb = library.fileSizeBytes / 1_000_000f
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("English IPA dictionary", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when {
                            isDownloaded -> "Downloaded · %.1f MB · ~115 000 words".format(fileSizeMb)
                            else         -> "~2.5 MB · 115 000 English words · offline lookup"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDownloaded) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete library",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Progress bar while downloading
            AnimatedVisibility(visible = downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Downloading… ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMsg?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isDownloaded) {
                    Button(
                        onClick = {
                            if (!downloading) {
                                downloading = true
                                errorMsg = null
                                progress = 0f
                                scope.launch {
                                    runCatching {
                                        library.download { p -> progress = p }
                                        refresh()
                                    }.onFailure { e ->
                                        errorMsg = "Download failed: ${e.message}"
                                    }
                                    downloading = false
                                }
                            }
                        },
                        enabled = !downloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                } else {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete dictionary?") },
            text = { Text("The offline IPA library will be removed. IPA lookup will still work when online.") },
            confirmButton = {
                Button(
                    onClick = { library.delete(); refresh(); confirmDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Shared row ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
