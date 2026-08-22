package com.lycoris.lycosheet.data.model

data class Card(
    val id: Long = 0L,
    val deckId: Long,
    val front: String,
    val back: String,
    val createdAt: Long = 0L,
    val seenCount: Int = 0
)
