package com.lycoris.lycosheet.domain.usecase.deck

import com.lycoris.lycosheet.data.repository.DeckRepository

class CreateDeckUseCase(private val repository: DeckRepository) {
    suspend operator fun invoke(name: String, description: String = ""): Long =
        repository.createDeck(name, description)
}
