package com.lycoris.lycosheet.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lycoris.lycosheet.android.util.ALL_IPA_LANGUAGES
import com.lycoris.lycosheet.android.util.IpaLanguage
import com.lycoris.lycosheet.android.util.IpaLibraryManager
import com.lycoris.lycosheet.android.util.IpaLookupService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * IPA phonetics field with:
 *  - Editable text field for the IPA transcription
 *  - Language picker (dropdown) showing all downloaded offline libraries
 *  - 🔍 search button that calls [IpaLookupService] using the selected language
 *  - Spinner while the lookup is running
 *  - Animated preview of the IPA below the field
 *
 * If no library is downloaded the picker is hidden and the search goes online
 * (English only, via Free Dictionary API).
 */
@Composable
fun IpaLookupField(
    lookupWord: String,
    phoneticText: String,
    onPhoneticChanged: (String) -> Unit,
    onPronunciationDownloaded: (String) -> Unit,
    isLooking: Boolean,
    onLookupStarted: () -> Unit,
    onLookupFinished: (ipa: String?, audioPath: String?, error: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val service: IpaLookupService = koinInject()
    val library: IpaLibraryManager = koinInject()
    val scope = rememberCoroutineScope()

    // Build the list of downloaded languages (shown in the picker)
    val downloadedLanguages: List<IpaLanguage> = remember {
        val codes = library.downloadedCodes()
        ALL_IPA_LANGUAGES.filter { it.code in codes }
    }

    // Selected language — default to en_US if downloaded, else first downloaded, else null
    var selectedLang by remember {
        mutableStateOf(
            downloadedLanguages.firstOrNull { it.code == "en_US" }
                ?: downloadedLanguages.firstOrNull()
        )
    }
    var langMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {

        // ── Language picker row (only when at least one library is downloaded) ──
        if (downloadedLanguages.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Search in:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    AssistChip(
                        onClick = { langMenuExpanded = true },
                        label = {
                            Text(
                                if (selectedLang != null)
                                    "${selectedLang!!.flag}  ${selectedLang!!.displayName}"
                                else
                                    "🌐  Online (EN)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false }
                    ) {
                        downloadedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${lang.flag}  ${lang.displayName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    selectedLang = lang
                                    langMenuExpanded = false
                                },
                                trailingIcon = if (lang == selectedLang) {
                                    { Text("✓", color = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // ── IPA text field + search button ─────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = phoneticText,
                onValueChange = onPhoneticChanged,
                label = { Text("Phonetics (IPA)") },
                placeholder = { Text("/ˈwɜːd/", fontStyle = FontStyle.Italic) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    if (isLooking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )

            FilledTonalIconButton(
                onClick = {
                    if (!isLooking && lookupWord.isNotBlank()) {
                        onLookupStarted()
                        scope.launch {
                            val result = service.lookup(lookupWord, selectedLang?.code)
                            if (result != null) {
                                result.localAudioPath?.let { onPronunciationDownloaded(it) }
                                onLookupFinished(result.ipa, result.localAudioPath, null)
                            } else {
                                val langName = selectedLang?.displayName ?: "English (online)"
                                onLookupFinished(
                                    null, null,
                                    "\"$lookupWord\" not found in $langName"
                                )
                            }
                        }
                    }
                },
                enabled = !isLooking && lookupWord.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = "Look up IPA")
            }
        }

        // ── IPA preview ─────────────────────────────────────────────────────────
        AnimatedVisibility(visible = phoneticText.isNotBlank()) {
            Text(
                phoneticText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
