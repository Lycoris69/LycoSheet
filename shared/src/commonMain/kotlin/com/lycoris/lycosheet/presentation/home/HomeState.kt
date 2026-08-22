package com.lycoris.lycosheet.presentation.home

import com.lycoris.lycosheet.data.model.Deck

data class HomeState(
    val frontText: String = "",
    val backText: String = "",
    val deckName: String = "",
    val selectedDeckId: Long? = null,
    val availableDecks: List<Deck> = emptyList(),
    val isLoading: Boolean = false,
    val cardSaved: Boolean = false,
    val error: String? = null
)
