package com.lycoris.lycosheet.domain.usecase.card

import com.lycoris.lycosheet.data.repository.CardRepository

class DeleteCardUseCase(private val repository: CardRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteCard(id)
}
