package com.lycoris.lycosheet.domain.usecase.deck

import com.lycoris.lycosheet.data.model.Deck
import com.lycoris.lycosheet.data.repository.DeckRepository

class GetDeckByIdUseCase(private val repository: DeckRepository) {
    suspend operator fun invoke(id: Long): Deck? = repository.getDeckById(id)
}
