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
import com.lycoris.lycosheet.android.util.IpaLookupService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * An IPA phonetics row shown below card creation forms.
 *
 * Shows:
 *  - Current IPA text (editable) or placeholder when empty
 *  - A 🔍 button that calls [IpaLookupService.lookup] for [lookupWord]
 *  - A spinner while the lookup is running
 *  - An error snack via [onError] on failure
 *
 * [lookupWord]         — the word to look up (front text / correct answer)
 * [phoneticText]       — current IPA value from state
 * [onPhoneticChanged]  — called when IPA text changes (typed or auto-filled)
 * [onPronunciationDownloaded] — called with local audio path when the API returns one
 * [isLooking]          — true while an in-progress lookup is running (from ViewModel state)
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
    val scope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )

            // Look-up button
            FilledTonalIconButton(
                onClick = {
                    if (!isLooking && lookupWord.isNotBlank()) {
                        onLookupStarted()
                        scope.launch {
                            val result = service.lookup(lookupWord)
                            if (result != null) {
                                result.localAudioPath?.let { onPronunciationDownloaded(it) }
                                onLookupFinished(result.ipa, result.localAudioPath, null)
                            } else {
                                onLookupFinished(null, null, "No phonetics found for \"$lookupWord\"")
                            }
                        }
                    }
                },
                enabled = !isLooking && lookupWord.isNotBlank()
            ) {
                Icon(Icons.Default.Search, contentDescription = "Look up IPA")
            }
        }

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
