package com.lycoris.lycosheet.data.repository

import com.lycoris.lycosheet.data.model.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsForDeck(deckId: Long): Flow<List<Card>>
    suspend fun getCardById(id: Long): Card?
    suspend fun createCard(deckId: Long, front: String, back: String): Long
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(id: Long)
    suspend fun getCardCountForDeck(deckId: Long): Long
}
