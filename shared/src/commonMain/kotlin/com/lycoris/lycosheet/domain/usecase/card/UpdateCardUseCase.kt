package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.repository.CardRepository

class UpdateCardUseCase(private val repository: CardRepository) {
    suspend operator fun invoke(card: Card) = repository.updateCard(card)
}
