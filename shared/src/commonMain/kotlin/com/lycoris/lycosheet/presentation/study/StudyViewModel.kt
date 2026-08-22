package com.lycoris.lycosheet.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lycoris.lycosheet.data.model.StudySession
import com.lycoris.lycosheet.domain.usecase.card.GetCardsForDeckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    private val getCardsForDeck: GetCardsForDeckUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StudyState())
    val state: StateFlow<StudyState> = _state.asStateFlow()

    fun loadDeck(deckId: Long, shuffle: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val cards = getCardsForDeck(deckId).first()
            val orderedCards = if (shuffle) cards.shuffled() else cards
            _state.update {
                it.copy(
                    session = StudySession(deckId = deckId, cards = orderedCards),
                    isLoading = false
                )
            }
        }
    }

    fun flipCard() {
        _state.update { s ->
            val session = s.session ?: return@update s
            s.copy(session = session.copy(isFrontVisible = !session.isFrontVisible))
        }
    }

    fun nextCard() {
        _state.update { s ->
            val session = s.session ?: return@update s
            if (!session.hasNext) return@update s.copy(isComplete = true)
            s.copy(session = session.copy(currentIndex = session.currentIndex + 1, isFrontVisible = true))
        }
    }

    fun previousCard() {
        _state.update { s ->
            val session = s.session ?: return@update s
            if (!session.hasPrevious) return@update s
            s.copy(session = session.copy(currentIndex = session.currentIndex - 1, isFrontVisible = true))
        }
    }

    fun restartSession() {
        _state.update { s ->
            val session = s.session ?: return@update s
            s.copy(
                session = session.copy(currentIndex = 0, isFrontVisible = true, cards = session.cards.shuffled()),
                isComplete = false
            )
        }
    }
}
