package com.lycoris.lycosheet.presentation.settings

data class SettingsState(
    val isDarkTheme: Boolean = false,
    val shuffleByDefault: Boolean = true,
    val showProgressBar: Boolean = true
)
