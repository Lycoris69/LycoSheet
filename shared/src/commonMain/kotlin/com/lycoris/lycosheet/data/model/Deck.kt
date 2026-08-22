package com.lycoris.lycosheet.data.model

data class Deck(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val cardCount: Int = 0,
    val createdAt: Long = 0L
)
