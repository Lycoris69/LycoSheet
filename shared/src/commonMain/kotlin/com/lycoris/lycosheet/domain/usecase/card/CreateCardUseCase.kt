package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.repository.CardRepository

class CreateCardUseCase(private val repository: CardRepository) {
    suspend operator fun invoke(deckId: Long, front: String, back: String): Long =
        repository.createCard(deckId, front, back)
}
