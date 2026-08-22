package com.lycoris.lycosheet.presentation.library

import com.lycoris.lycosheet.data.model.Deck

data class LibraryState(
    val decks: List<Deck> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
