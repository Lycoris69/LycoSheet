package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.model.CardType
import com.lycoris.lycosheet.data.repository.CardRepository

class CreateCardUseCase(private val repository: CardRepository) {
    suspend operator fun invoke(
        deckId: Long,
        front: String,
        back: String,
        cardType: CardType = CardType.CLASSIC,
        extraData: String = ""
    ): Long = repository.createCard(deckId, front, back, cardType, extraData)
}
