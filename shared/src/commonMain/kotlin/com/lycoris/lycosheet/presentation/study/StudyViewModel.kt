package com.lycoris.lycosheet.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lycoris.lycosheet.data.model.CardGrade
import com.lycoris.lycosheet.data.model.StudySession
import com.lycoris.lycosheet.domain.usecase.card.GetCardsForDeckUseCase
import com.lycoris.lycosheet.domain.usecase.card.IncrementCardSeenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    private val getCardsForDeck: GetCardsForDeckUseCase,
    private val incrementSeen: IncrementCardSeenUseCase
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
                    grades = emptyList(),
                    isLoading = false
                )
            }
            // Mark the first card as seen
            orderedCards.firstOrNull()?.let { incrementSeen(it.id) }
        }
    }

    fun flipCard() {
        _state.update { s ->
            val session = s.session ?: return@update s
            s.copy(session = session.copy(isFrontVisible = !session.isFrontVisible))
        }
    }

    /** Record a grade for the current card and advance.
     *  AGAIN re-queues the card at the end so it will be seen again this session. */
    fun gradeCard(grade: CardGrade) {
        viewModelScope.launch {
            _state.update { s ->
                val session = s.session ?: return@update s
                val newGrades = s.grades + grade

                // Re-queue if "Again"
                val newCards = if (grade == CardGrade.AGAIN) {
                    session.cards + session.currentCard!!
                } else {
                    session.cards
                }

                val newSession = session.copy(cards = newCards)

                if (!newSession.hasNext) {
                    s.copy(session = newSession, grades = newGrades, isComplete = true)
                } else {
                    s.copy(
                        session = newSession.copy(
                            currentIndex = newSession.currentIndex + 1,
                            isFrontVisible = true
                        ),
                        grades = newGrades
                    )
                }
            }
            // Increment seen count for the card we just moved to
            _state.value.session?.currentCard?.let { incrementSeen(it.id) }
        }
    }

    fun nextCard() {
        viewModelScope.launch {
            _state.update { s ->
                val session = s.session ?: return@update s
                if (!session.hasNext) return@update s.copy(isComplete = true)
                s.copy(session = session.copy(currentIndex = session.currentIndex + 1, isFrontVisible = true))
            }
            _state.value.session?.currentCard?.let { incrementSeen(it.id) }
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
                session = session.copy(
                    currentIndex = 0,
                    isFrontVisible = true,
                    cards = session.cards.shuffled()
                ),
                grades = emptyList(),
                isComplete = false
            )
        }
    }
}
