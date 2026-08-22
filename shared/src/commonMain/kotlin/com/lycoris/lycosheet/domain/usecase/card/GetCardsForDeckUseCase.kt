package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.repository.CardRepository
import kotlinx.coroutines.flow.Flow

class GetCardsForDeckUseCase(private val repository: CardRepository) {
    operator fun invoke(deckId: Long): Flow<List<Card>> = repository.getCardsForDeck(deckId)
}
