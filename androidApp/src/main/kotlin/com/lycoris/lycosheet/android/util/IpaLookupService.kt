package com.lycoris.lycosheet.android.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Result of an IPA lookup.
 *
 * [ipa]          — phonetic transcription string, e.g. "/ˈwɔːtər/"
 * [localAudioPath] — absolute path of the downloaded native-speaker MP3, or null
 */
data class IpaResult(
    val ipa: String,
    val localAudioPath: String? = null
)

/**
 * Looks up IPA phonetic transcription (and optionally downloads a native-speaker pronunciation)
 * for a given word.
 *
 * Strategy:
 *  1. Offline library (instant, no network) — if [IpaLibraryManager.isDownloaded]
 *  2. Free Dictionary API (online, no key) — https://api.dictionaryapi.dev
 *     Also downloads the MP3 audio when an audio URL is present.
 *
 * All network I/O runs on [Dispatchers.IO].
 */
class IpaLookupService(
    private val context: Context,
    private val library: IpaLibraryManager
) {
    /**
     * Look up [word] and return an [IpaResult], or null if nothing was found.
     *
     * [languageCode] — ISO code of the offline library to search (e.g. "en_US", "fr_FR").
     *   - If non-null, that library is searched first (instant when downloaded).
     *   - Online Free Dictionary API is only tried when [languageCode] is null or starts with "en".
     *   - If null, falls back to en_US → en_UK offline then online.
     *
     * Never throws; all errors are swallowed and return null.
     */
    suspend fun lookup(word: String, languageCode: String? = null): IpaResult? {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return null

        // 1. Offline library
        val offlineIpa: String? = if (languageCode != null) {
            library.lookup(clean, languageCode)
        } else {
            // No specific language — default to en_US → en_UK
            library.lookup(clean, "en_US") ?: library.lookup(clean, "en_UK")
        }

        // 2. Online (English only — Free Dictionary API doesn't support other languages)
        val isEnglish = languageCode == null || languageCode.startsWith("en")
        val online = if (isEnglish) runCatching { fetchOnline(clean) }.getOrNull() else null

        return when {
            online != null -> {
                val ipa = online.ipa.ifBlank { offlineIpa } ?: return null
                IpaResult(ipa, online.localAudioPath)
            }
            offlineIpa != null -> IpaResult(offlineIpa)
            else -> null
        }
    }

    // ── private ───────────────────────────────────────────────────────────────

    private suspend fun fetchOnline(word: String): IpaResult? = withContext(Dispatchers.IO) {
        val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/${word}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LycoSheet-Android")
            connectTimeout = 6_000
            readTimeout    = 6_000
        }

        if (connection.responseCode != 200) return@withContext null

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseResponse(body)
    }

    private suspend fun parseResponse(json: String): IpaResult? {
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return null
        if (arr.length() == 0) return null

        val entry = arr.getJSONObject(0)
        val phonetics = entry.optJSONArray("phonetics") ?: return null

        var bestIpa = ""
        var bestAudioUrl: String? = null

        // Prefer an entry that has both text and audio
        for (i in 0 until phonetics.length()) {
            val ph = phonetics.getJSONObject(i)
            val text  = ph.optString("text")
            val audio = ph.optString("audio").takeIf { it.isNotBlank() }
            if (text.isNotBlank() && audio != null && bestAudioUrl == null) {
                bestIpa      = text
                bestAudioUrl = audio
                break
            }
            if (text.isNotBlank() && bestIpa.isBlank()) bestIpa = text
        }

        if (bestIpa.isBlank()) return null

        val localAudio = bestAudioUrl?.let { downloadAudio(it) }
        return IpaResult(bestIpa, localAudio)
    }

    private suspend fun downloadAudio(audioUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(audioUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout    = 15_000
            }
            if (connection.responseCode != 200) return@runCatching null

            val ext = audioUrl.substringAfterLast('.', "mp3")
            val outFile = File(context.filesDir, "audio/${UUID.randomUUID()}.$ext")
            outFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                outFile.outputStream().use { it.write(input.readBytes()) }
            }
            outFile.absolutePath
        }.getOrNull()
    }
}
