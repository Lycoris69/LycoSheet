package com.lycoris.lycosheet.domain.usecase.deck

import com.lycoris.lycosheet.data.model.Deck
import com.lycoris.lycosheet.data.repository.DeckRepository
import kotlinx.coroutines.flow.Flow

class GetAllDecksUseCase(private val repository: DeckRepository) {
    operator fun invoke(): Flow<List<Deck>> = repository.getAllDecks()
}
