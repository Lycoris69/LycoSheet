package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.repository.CardRepository

class IncrementCardSeenUseCase(private val repository: CardRepository) {
    suspend operator fun invoke(cardId: Long) = repository.incrementSeenCount(cardId)
}
