package com.lycoris.lycosheet.data.repository

import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.model.CardType
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getCardsForDeck(deckId: Long): Flow<List<Card>>
    suspend fun getCardById(id: Long): Card?
    suspend fun createCard(
        deckId: Long,
        front: String,
        back: String,
        cardType: CardType = CardType.CLASSIC,
        extraData: String = "",
        pronunciationPath: String = "",
        phoneticText: String = ""
    ): Long
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(id: Long)
    suspend fun getCardCountForDeck(deckId: Long): Long
    suspend fun incrementSeenCount(cardId: Long)
}
