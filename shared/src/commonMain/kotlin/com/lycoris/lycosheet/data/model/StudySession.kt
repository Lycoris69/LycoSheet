package com.lycoris.lycosheet.data.model

data class StudySession(
    val deckId: Long,
    val cards: List<Card>,
    val currentIndex: Int = 0,
    val isFrontVisible: Boolean = true
) {
    val currentCard: Card? get() = cards.getOrNull(currentIndex)
    val isComplete: Boolean get() = cards.isNotEmpty() && currentIndex >= cards.size
    val progress: Float get() = if (cards.isEmpty()) 0f else currentIndex.toFloat() / cards.size
    val hasNext: Boolean get() = currentIndex < cards.size - 1
    val hasPrevious: Boolean get() = currentIndex > 0
}
