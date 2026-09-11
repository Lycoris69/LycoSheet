package com.lycoris.lycosheet.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.domain.usecase.card.CreateCardUseCase
import com.lycoris.lycosheet.domain.usecase.deck.CreateDeckUseCase
import com.lycoris.lycosheet.domain.usecase.deck.GetAllDecksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val createCard: CreateCardUseCase,
    private val createDeck: CreateDeckUseCase,
    private val getAllDecks: GetAllDecksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllDecks().collect { decks ->
                _state.update { it.copy(availableDecks = decks) }
            }
        }
    }

    fun onFrontChanged(text: String) = _state.update { it.copy(frontText = text) }
    fun onBackChanged(text: String) = _state.update { it.copy(backText = text) }
    fun onDeckNameChanged(name: String) = _state.update { it.copy(deckName = name) }
    fun onDeckSelected(deckId: Long?) = _state.update { it.copy(selectedDeckId = deckId) }
    fun onCardTypeChanged(type: CardType) = _state.update { it.copy(cardType = type) }
    fun onWrongChoice1Changed(text: String) = _state.update { it.copy(wrongChoice1 = text) }
    fun onWrongChoice2Changed(text: String) = _state.update { it.copy(wrongChoice2 = text) }
    fun onWrongChoice3Changed(text: String) = _state.update { it.copy(wrongChoice3 = text) }
    fun onAudioPathChanged(path: String) = _state.update { it.copy(audioPath = path) }

    fun saveCard() {
        val s = _state.value
        // LISTENING: back (transcript) required + audio file must exist
        if (s.cardType == CardType.LISTENING) {
            if (s.backText.isBlank() || s.audioPath.isBlank()) return
        } else {
            if (s.frontText.isBlank() || s.backText.isBlank()) return
        }
        if (s.cardType == CardType.MULTIPLE_CHOICE &&
            s.wrongChoice1.isBlank() && s.wrongChoice2.isBlank() && s.wrongChoice3.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val deckId = s.selectedDeckId ?: createDeck(
                    name = s.deckName.ifBlank { "My Deck" }
                )
                val (front, extraData) = when (s.cardType) {
                    CardType.MULTIPLE_CHOICE -> s.frontText.trim() to
                            listOf(s.wrongChoice1, s.wrongChoice2, s.wrongChoice3)
                                .filter { it.isNotBlank() }.joinToString("|")
                    CardType.LISTENING -> s.frontText.trim() to s.audioPath // front = optional hint
                    else -> s.frontText.trim() to ""
                }
                createCard(deckId, front, s.backText.trim(), s.cardType, extraData)
                _state.update {
                    it.copy(
                        frontText = "",
                        backText = "",
                        wrongChoice1 = "",
                        wrongChoice2 = "",
                        wrongChoice3 = "",
                        audioPath = "",   // reset audio after save; user records a new clip for each card
                        isLoading = false,
                        cardSaved = true
                        // cardType kept so user can batch-create
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onCardSavedConsumed() = _state.update { it.copy(cardSaved = false) }
    fun onErrorConsumed() = _state.update { it.copy(error = null) }
}
