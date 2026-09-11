package com.lycoris.lycosheet.android.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages the offline IPA pronunciation dictionary.
 *
 * Source: https://github.com/open-dict-data/ipa-dict (en_US, ~115K entries, ~2.4 MB)
 * Format: one "word\t/IPA/" per line, alphabetically sorted.
 *
 * The file is downloaded once to [filesDir]/ipa_dict/en_US.txt and kept until [delete] is called.
 * On first [lookup] after download the file is parsed into an in-memory HashMap (~12 MB heap).
 */
class IpaLibraryManager(context: Context) {

    private val dictFile = File(context.filesDir, "ipa_dict/en_US.txt").also {
        it.parentFile?.mkdirs()
    }

    /** Whether the library has been downloaded. */
    val isDownloaded: Boolean get() = dictFile.exists() && dictFile.length() > 0

    /** File size in bytes, or 0 if not downloaded. */
    val fileSizeBytes: Long get() = if (isDownloaded) dictFile.length() else 0L

    private var cache: HashMap<String, String>? = null

    /**
     * Look up the IPA string for a single word (case-insensitive).
     * Returns the first pronunciation if there are multiple (separated by ", ").
     * Returns null when not in the library or library not downloaded.
     */
    fun lookup(word: String): String? {
        if (!isDownloaded) return null
        val map = cache ?: loadCache()
        val raw = map[word.lowercase().trim()] ?: return null
        // Some entries have multiple pronunciations: "/ˈwɔtɚ/, /ˈwɑtɚ/" → take first
        return raw.substringBefore(",").trim()
    }

    /**
     * Download the English IPA dictionary from GitHub.
     * [onProgress] receives values from 0f to 1f.
     * Throws on network/IO error.
     */
    suspend fun download(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val src = URL("https://raw.githubusercontent.com/open-dict-data/ipa-dict/master/data/en_US.txt")
        val connection = (src.openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "LycoSheet-Android")
            connectTimeout = 15_000
            readTimeout    = 30_000
        }

        val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: 2_500_000L
        var bytesRead = 0L

        connection.inputStream.use { input ->
            dictFile.outputStream().use { output ->
                val buf = ByteArray(8192)
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    output.write(buf, 0, n)
                    bytesRead += n
                    onProgress((bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                }
            }
        }
        cache = null   // invalidate so next lookup reloads from fresh file
        onProgress(1f)
    }

    /** Remove the downloaded file and clear the in-memory cache. */
    fun delete() {
        dictFile.delete()
        cache = null
    }

    // ── private ───────────────────────────────────────────────────────────────

    private fun loadCache(): HashMap<String, String> {
        val map = HashMap<String, String>(130_000)
        dictFile.forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) map[line.substring(0, tab)] = line.substring(tab + 1)
        }
        cache = map
        return map
    }
}
