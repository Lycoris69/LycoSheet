package com.lycoris.lycosheet.android.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages offline IPA pronunciation dictionaries for multiple languages.
 *
 * Source: https://github.com/open-dict-data/ipa-dict
 * Format: one "word\t/IPA/" per line, alphabetically sorted.
 *
 * Files are stored at [filesDir]/ipa_dict/{code}.txt and kept until [delete] is called.
 * Each language's words are loaded into a HashMap on first [lookup] (~12 MB per language).
 */
class IpaLibraryManager(private val context: Context) {

    private val dictDir = File(context.filesDir, "ipa_dict").also { it.mkdirs() }

    /** In-memory caches, keyed by language code. Null = not loaded yet. */
    private val caches = mutableMapOf<String, HashMap<String, String>?>()

    // ── Status ────────────────────────────────────────────────────────────────

    fun fileFor(code: String): File = File(dictDir, "$code.txt")
    fun isDownloaded(code: String): Boolean = fileFor(code).let { it.exists() && it.length() > 0 }
    fun fileSizeBytes(code: String): Long = if (isDownloaded(code)) fileFor(code).length() else 0L

    /** Returns the codes of all currently downloaded libraries. */
    fun downloadedCodes(): Set<String> =
        (dictDir.listFiles() ?: emptyArray())
            .filter { it.extension == "txt" && it.length() > 0 }
            .map { it.nameWithoutExtension }
            .toSet()

    // ── Lookup ────────────────────────────────────────────────────────────────

    /**
     * Look up IPA for [word] in the given [code] library (defaults to "en_US").
     * Returns null when not found or the library is not downloaded.
     */
    fun lookup(word: String, code: String = "en_US"): String? {
        if (!isDownloaded(code)) return null
        val map = caches[code] ?: loadCache(code)
        val raw = map[word.lowercase().trim()] ?: return null
        // Take the first pronunciation when multiple exist, e.g. "/ˈwɔtɚ/, /ˈwɑtɚ/"
        return raw.substringBefore(",").trim()
    }

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Download the IPA dictionary for [language] from GitHub.
     * [onProgress] receives values from 0f to 1f.
     * Throws on network / IO error.
     */
    suspend fun download(language: IpaLanguage, onProgress: (Float) -> Unit) =
        withContext(Dispatchers.IO) {
            val connection = (URL(language.downloadUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "LycoSheet-Android")
                connectTimeout = 15_000
                readTimeout    = 30_000
            }

            val totalBytes = connection.contentLengthLong
                .takeIf { it > 0 }
                ?: (language.sizeEstimateKb * 1_000L)
            var bytesRead = 0L

            connection.inputStream.use { input ->
                fileFor(language.code).outputStream().use { output ->
                    val buf = ByteArray(8_192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        bytesRead += n
                        onProgress((bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }
            }
            caches[language.code] = null   // invalidate; reloaded on next lookup
            onProgress(1f)
        }

    // ── Delete ────────────────────────────────────────────────────────────────

    /** Remove the downloaded file and evict the in-memory cache for [code]. */
    fun delete(code: String) {
        fileFor(code).delete()
        caches.remove(code)
    }

    // ── private ───────────────────────────────────────────────────────────────

    private fun loadCache(code: String): HashMap<String, String> {
        val map = HashMap<String, String>(130_000)
        fileFor(code).forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) map[line.substring(0, tab)] = line.substring(tab + 1)
        }
        caches[code] = map
        return map
    }
}
