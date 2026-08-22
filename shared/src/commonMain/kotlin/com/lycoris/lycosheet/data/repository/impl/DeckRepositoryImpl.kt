package com.lycoris.lycosheet.data.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lycoris.lycosheet.data.model.Deck
import com.lycoris.lycosheet.data.repository.DeckRepository
import com.lycoris.lycosheet.db.LycoSheetDatabase
import com.lycoris.lycosheet.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DeckRepositoryImpl(private val db: LycoSheetDatabase) : DeckRepository {

    private val queries = db.deckEntityQueries

    override fun getAllDecks(): Flow<List<Deck>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDeck() } }

    override suspend fun getDeckById(id: Long): Deck? =
        withContext(Dispatchers.Default) {
            queries.selectById(id).executeAsOneOrNull()?.toDeck()
        }

    override suspend fun createDeck(name: String, description: String): Long =
        withContext(Dispatchers.Default) {
            queries.insert(name, description, currentTimeMillis())
            queries.lastInsertRowId().executeAsOne()
        }

    override suspend fun updateDeck(deck: Deck) =
        withContext(Dispatchers.Default) {
            queries.update(deck.name, deck.description, deck.id)
        }

    override suspend fun deleteDeck(id: Long) =
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }

    private fun com.lycoris.lycosheet.db.DeckEntity.toDeck() = Deck(
        id = id,
        name = name,
        description = description,
        createdAt = created_at
    )
}
