package com.lycoris.lycosheet.presentation.deck

import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.model.Deck

data class DeckDetailState(
    val deck: Deck? = null,
    val cards: List<Card> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val cardSaved: Boolean = false
)
