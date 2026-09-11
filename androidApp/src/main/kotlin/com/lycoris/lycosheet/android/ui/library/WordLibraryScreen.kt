package com.lycoris.lycosheet.android.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.util.ALL_IPA_LANGUAGES
import com.lycoris.lycosheet.android.util.IpaLanguage
import com.lycoris.lycosheet.android.util.IpaLibraryManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLibraryScreen(onBack: () -> Unit) {
    val library: IpaLibraryManager = koinInject()
    val scope = rememberCoroutineScope()

    var downloadedCodes by remember {
        mutableStateOf(library.downloadedCodes())
    }
    var downloadStates by remember {
        mutableStateOf(emptyMap<String, DownloadState>())
    }
    var confirmDeleteLang by remember { mutableStateOf<IpaLanguage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Libraries") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header description ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Offline pronunciation dictionaries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Download a language to look up IPA phonetics instantly without internet. " +
                            "Native-speaker audio requires an internet connection and is available for English only (Free Dictionary API).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Text(
                    "Available languages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // ── Language cards ─────────────────────────────────────────────────
            items(ALL_IPA_LANGUAGES, key = { it.code }) { lang ->
                val isDownloaded = lang.code in downloadedCodes
                val state = downloadStates[lang.code] ?: DownloadState()

                LanguageLibraryCard(
                    language = lang,
                    isDownloaded = isDownloaded,
                    fileSizeMb = library.fileSizeBytes(lang.code) / 1_000_000f,
                    downloadState = state,
                    onDownload = {
                        scope.launch {
                            downloadStates = downloadStates +
                                    (lang.code to DownloadState(isDownloading = true, progress = 0f))
                            runCatching {
                                library.download(lang) { p ->
                                    downloadStates = downloadStates +
                                            (lang.code to DownloadState(isDownloading = true, progress = p))
                                }
                                downloadedCodes = downloadedCodes + lang.code
                                downloadStates = downloadStates - lang.code
                            }.onFailure { e ->
                                downloadStates = downloadStates +
                                        (lang.code to DownloadState(error = "Download failed: ${e.message}"))
                            }
                        }
                    },
                    onDelete = { confirmDeleteLang = lang }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // ── Confirm delete dialog ──────────────────────────────────────────────────
    confirmDeleteLang?.let { lang ->
        AlertDialog(
            onDismissRequest = { confirmDeleteLang = null },
            title = { Text("Delete ${lang.flag} ${lang.displayName}?") },
            text = {
                Text(
                    "The offline IPA dictionary will be removed (${
                        "%.1f".format(library.fileSizeBytes(lang.code) / 1_000_000f)
                    } MB freed). You can re-download it any time."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        library.delete(lang.code)
                        downloadedCodes = downloadedCodes - lang.code
                        confirmDeleteLang = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteLang = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Language card ──────────────────────────────────────────────────────────────

@Composable
private fun LanguageLibraryCard(
    language: IpaLanguage,
    isDownloaded: Boolean,
    fileSizeMb: Float,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    language.flag,
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        language.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isDownloaded)
                            "Downloaded · ${"%.1f".format(fileSizeMb)} MB"
                        else
                            "~${"%.1f".format(language.sizeEstimateMb)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDownloaded) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Feature chips ──────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FeatureChip(label = "IPA", active = true)
                if (language.hasOnlineAudio) {
                    FeatureChip(label = "🔊 Audio (online)", active = false)
                }
            }

            // ── Progress bar ───────────────────────────────────────────────────
            AnimatedVisibility(visible = downloadState.isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Downloading… ${(downloadState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Error ──────────────────────────────────────────────────────────
            downloadState.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ── Action button ──────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    downloadState.isDownloading -> {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                            Text("Downloading…")
                        }
                    }
                    isDownloaded -> {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
