package com.lycoris.lycosheet.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lycoris.lycosheet.domain.usecase.card.GetCardsForDeckUseCase
import com.lycoris.lycosheet.domain.usecase.deck.DeleteDeckUseCase
import com.lycoris.lycosheet.domain.usecase.deck.GetAllDecksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val getAllDecks: GetAllDecksUseCase,
    private val getCardsForDeck: GetCardsForDeckUseCase,
    private val deleteDeck: DeleteDeckUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getAllDecks().collect { decks ->
                // Enrich each deck with its real card count
                val enriched = decks.map { deck ->
                    var count = 0
                    getCardsForDeck(deck.id).collect { cards -> count = cards.size }
                    deck.copy(cardCount = count)
                }
                _state.update { it.copy(decks = enriched, isLoading = false) }
            }
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            try {
                deleteDeck.invoke(deckId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun onErrorConsumed() = _state.update { it.copy(error = null) }
}
