package com.lycoris.lycosheet.domain.usecase.deck

import com.lycoris.lycosheet.data.repository.DeckRepository

class DeleteDeckUseCase(private val repository: DeckRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteDeck(id)
}
