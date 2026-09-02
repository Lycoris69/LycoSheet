package com.lycoris.lycosheet.presentation.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.domain.usecase.card.DeleteCardUseCase
import com.lycoris.lycosheet.domain.usecase.card.GetCardsForDeckUseCase
import com.lycoris.lycosheet.domain.usecase.card.UpdateCardUseCase
import com.lycoris.lycosheet.domain.usecase.deck.GetDeckByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeckDetailViewModel(
    private val getDeckById: GetDeckByIdUseCase,
    private val getCardsForDeck: GetCardsForDeckUseCase,
    private val updateCard: UpdateCardUseCase,
    private val deleteCard: DeleteCardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeckDetailState())
    val state: StateFlow<DeckDetailState> = _state.asStateFlow()

    fun loadDeck(deckId: Long) {
        viewModelScope.launch {
            // One-shot fetch of deck metadata (name, description)
            val deck = getDeckById(deckId)
            _state.update { it.copy(deck = deck) }
            // Reactive card list — re-emits on any insert, update, or delete
            getCardsForDeck(deckId).collect { cards ->
                _state.update { it.copy(cards = cards, isLoading = false) }
            }
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch {
            updateCard.invoke(card)
            _state.update { it.copy(cardSaved = true) }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            deleteCard.invoke(cardId)
        }
    }

    fun onCardSavedConsumed() = _state.update { it.copy(cardSaved = false) }
}
