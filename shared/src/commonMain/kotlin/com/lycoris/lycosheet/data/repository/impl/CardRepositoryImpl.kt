package com.lycoris.lycosheet.data.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.repository.CardRepository
import com.lycoris.lycosheet.db.LycoSheetDatabase
import com.lycoris.lycosheet.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CardRepositoryImpl(private val db: LycoSheetDatabase) : CardRepository {

    private val queries = db.cardQueries

    override fun getCardsForDeck(deckId: Long): Flow<List<Card>> =
        queries.selectByDeckId(deckId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toCard() } }

    override suspend fun getCardById(id: Long): Card? =
        withContext(Dispatchers.Default) {
            queries.selectById(id).executeAsOneOrNull()?.toCard()
        }

    override suspend fun createCard(deckId: Long, front: String, back: String): Long =
        withContext(Dispatchers.Default) {
            queries.insert(deckId, front, back, currentTimeMillis())
            queries.lastInsertRowId().executeAsOne()
        }

    override suspend fun updateCard(card: Card) =
        withContext(Dispatchers.Default) {
            queries.update(card.front, card.back, card.id)
        }

    override suspend fun deleteCard(id: Long) =
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }

    override suspend fun getCardCountForDeck(deckId: Long): Long =
        withContext(Dispatchers.Default) {
            queries.countByDeckId(deckId).executeAsOne()
        }

    private fun com.lycoris.lycosheet.db.CardEntity.toCard() = Card(
        id = id,
        deckId = deck_id,
        front = front,
        back = back,
        createdAt = created_at
    )
}
