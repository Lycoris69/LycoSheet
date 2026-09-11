package com.lycoris.lycosheet.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Modal dialog shown when a newer GitHub release is detected.
 *
 * [currentVersion] and [latestVersion] are bare semver strings (no "v" prefix).
 * [releaseUrl] is the GitHub release page HTML URL.
 * [onDismiss] is called when the user taps "Later" or dismisses the dialog.
 */
@Composable
fun UpdateDialog(
    currentVersion: String,
    latestVersion: String,
    releaseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Update available") },
        text = {
            Text(
                buildAnnotatedString {
                    append("A new version of LycoSheet is available.\n\n")
                    append("Current version: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("v$currentVersion") }
                    append("\nLatest version:  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)) {
                        append("v$latestVersion")
                    }
                }
            )
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                )
            }) {
                Text("Download update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
