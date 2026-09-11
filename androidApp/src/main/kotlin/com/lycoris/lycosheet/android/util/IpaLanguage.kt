package com.lycoris.lycosheet.android.util

/**
 * Describes one downloadable IPA pronunciation library.
 *
 * Source: https://github.com/open-dict-data/ipa-dict
 *
 * [code]              — file code used in the repo, e.g. "en_US"
 * [displayName]       — human-readable name shown in the UI
 * [flag]              — emoji flag for the language/region
 * [sizeEstimateKb]    — rough download size in kilobytes
 * [hasOnlineAudio]    — whether the Free Dictionary API can supply native-speaker
 *                       audio for this language (currently English-only)
 */
data class IpaLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val sizeEstimateKb: Int,
    val hasOnlineAudio: Boolean = false
) {
    val sizeEstimateMb: Float get() = sizeEstimateKb / 1_000f
    val downloadUrl: String
        get() = "https://raw.githubusercontent.com/open-dict-data/ipa-dict/master/data/$code.txt"
}

/** Complete catalog of available IPA dictionaries. */
val ALL_IPA_LANGUAGES: List<IpaLanguage> = listOf(
    IpaLanguage("en_US",   "English (US)",            "🇺🇸", 2_400, hasOnlineAudio = true),
    IpaLanguage("en_UK",   "English (UK)",            "🇬🇧", 2_100, hasOnlineAudio = true),
    IpaLanguage("fr_FR",   "French",                  "🇫🇷", 1_800),
    IpaLanguage("de",      "German",                  "🇩🇪", 2_200),
    IpaLanguage("es_ES",   "Spanish (Spain)",         "🇪🇸", 1_600),
    IpaLanguage("es_MX",   "Spanish (Mexico)",        "🇲🇽", 1_400),
    IpaLanguage("nl",      "Dutch",                   "🇳🇱",   900),
    IpaLanguage("sv",      "Swedish",                 "🇸🇪",   800),
    IpaLanguage("nb",      "Norwegian",               "🇳🇴",   700),
    IpaLanguage("fi",      "Finnish",                 "🇫🇮", 1_100),
    IpaLanguage("ro",      "Romanian",                "🇷🇴",   900),
    IpaLanguage("tr",      "Turkish",                 "🇹🇷",   700),
    IpaLanguage("ar",      "Arabic",                  "🇸🇦", 1_200),
    IpaLanguage("fa",      "Persian",                 "🇮🇷",   600),
    IpaLanguage("zh_hans", "Chinese (Simplified)",    "🇨🇳", 2_000),
    IpaLanguage("yue",     "Chinese (Cantonese)",     "🇭🇰", 1_800),
    IpaLanguage("ja",      "Japanese",                "🇯🇵", 3_000),
    IpaLanguage("ko",      "Korean",                  "🇰🇷", 1_500),
    IpaLanguage("vi",      "Vietnamese",              "🇻🇳", 1_000),
    IpaLanguage("tl",      "Tagalog",                 "🇵🇭",   400),
    IpaLanguage("is",      "Icelandic",               "🇮🇸",   500),
    IpaLanguage("eo",      "Esperanto",               "🟢",   600),
)
