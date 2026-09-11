package com.lycoris.lycosheet.data.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lycoris.lycosheet.data.model.Card
import com.lycoris.lycosheet.data.model.CardType
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

    override suspend fun createCard(
        deckId: Long,
        front: String,
        back: String,
        cardType: CardType,
        extraData: String,
        pronunciationPath: String,
        phoneticText: String
    ): Long = withContext(Dispatchers.Default) {
        queries.insert(deckId, front, back, cardType.name, extraData, pronunciationPath, phoneticText, currentTimeMillis())
        queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun updateCard(card: Card) =
        withContext(Dispatchers.Default) {
            queries.update(
                front = card.front,
                back = card.back,
                card_type = card.cardType.name,
                extra_data = card.extraData,
                pronunciation_path = card.pronunciationPath,
                phonetic_text = card.phoneticText,
                id = card.id
            )
        }

    override suspend fun deleteCard(id: Long) =
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }

    override suspend fun getCardCountForDeck(deckId: Long): Long =
        withContext(Dispatchers.Default) {
            queries.countByDeckId(deckId).executeAsOne()
        }

    override suspend fun incrementSeenCount(cardId: Long) =
        withContext(Dispatchers.Default) {
            queries.incrementSeenCount(cardId)
        }

    private fun com.lycoris.lycosheet.db.CardEntity.toCard() = Card(
        id = id,
        deckId = deck_id,
        front = front,
        back = back,
        cardType = runCatching { CardType.valueOf(card_type) }.getOrDefault(CardType.CLASSIC),
        extraData = extra_data,
        pronunciationPath = pronunciation_path,
        phoneticText = phonetic_text,
        createdAt = created_at,
        seenCount = seen_count.toInt()
    )
}
