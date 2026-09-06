package com.lycoris.lycosheet.presentation.home

import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.data.model.Deck

data class HomeState(
    val frontText: String = "",
    val backText: String = "",
    val cardType: CardType = CardType.CLASSIC,
    // Wrong-choice slots for MULTIPLE_CHOICE (up to 3; joined with '|' on save)
    val wrongChoice1: String = "",
    val wrongChoice2: String = "",
    val wrongChoice3: String = "",
    val deckName: String = "",
    val selectedDeckId: Long? = null,
    val availableDecks: List<Deck> = emptyList(),
    val isLoading: Boolean = false,
    val cardSaved: Boolean = false,
    val error: String? = null
)
