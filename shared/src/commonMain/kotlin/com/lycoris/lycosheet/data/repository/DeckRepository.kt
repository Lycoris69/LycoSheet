package com.lycoris.lycosheet.data.repository

import com.lycoris.lycosheet.data.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun getAllDecks(): Flow<List<Deck>>
    suspend fun getDeckById(id: Long): Deck?
    suspend fun createDeck(name: String, description: String = ""): Long
    suspend fun updateDeck(deck: Deck)
    suspend fun deleteDeck(id: Long)
}
