package com.lycoris.lycosheet.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,   // e.g. "0.1.7" (v-prefix already stripped)
    val releaseUrl: String        // e.g. "https://github.com/…/releases/tag/v0.1.7"
)

/**
 * Hits the GitHub releases API and returns [UpdateInfo] when [currentVersion] is behind
 * the latest release, or null if already up-to-date or on any error (network, parse, etc.).
 *
 * Call on a background coroutine; this function switches to [Dispatchers.IO] internally.
 */
suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/Lycoris69/LycoSheet/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LycoSheet-Android")
            connectTimeout = 6_000
            readTimeout   = 6_000
        }

        if (connection.responseCode != 200) return@withContext null

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val tagName  = json.getString("tag_name")          // "v0.1.7"
        val htmlUrl  = json.getString("html_url")
        val latest   = tagName.removePrefix("v")

        if (isNewer(latest, currentVersion)) UpdateInfo(latest, htmlUrl) else null
    } catch (_: Exception) {
        null   // silently swallow — network/parse errors must not crash the app
    }
}

/**
 * Returns true when [candidate] is a strictly newer semantic version than [current].
 * Both strings must be in "MAJOR.MINOR.PATCH" form; extra components are handled gracefully.
 */
private fun isNewer(candidate: String, current: String): Boolean {
    val c = candidate.split(".").map { it.toIntOrNull() ?: 0 }
    val r = current.split(".").map   { it.toIntOrNull() ?: 0 }
    val len = maxOf(c.size, r.size)
    for (i in 0 until len) {
        val cv = c.getOrElse(i) { 0 }
        val rv = r.getOrElse(i) { 0 }
        if (cv > rv) return true
        if (cv < rv) return false
    }
    return false
}
