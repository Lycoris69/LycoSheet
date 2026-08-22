package com.lycoris.lycosheet.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun toggleDarkTheme() = _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    fun toggleShuffle() = _state.update { it.copy(shuffleByDefault = !it.shuffleByDefault) }
    fun toggleProgressBar() = _state.update { it.copy(showProgressBar = !it.showProgressBar) }
}
