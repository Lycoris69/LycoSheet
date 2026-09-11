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
    // Absolute file path for LISTENING cards; persists across type switches
    val audioPath: String = "",
    // Optional pronunciation clip available on all non-Listening card types
    val pronunciationPath: String = "",
    // IPA phonetic string, e.g. "/ˈwɔːtər/" — auto-filled by IPA lookup
    val phoneticText: String = "",
    // IPA lookup state
    val isLookingUpIpa: Boolean = false,
    val ipaLookupError: String? = null,
    val deckName: String = "",
    val selectedDeckId: Long? = null,
    val availableDecks: List<Deck> = emptyList(),
    val isLoading: Boolean = false,
    val cardSaved: Boolean = false,
    val error: String? = null
)
