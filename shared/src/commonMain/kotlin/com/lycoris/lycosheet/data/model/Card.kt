package com.lycoris.lycosheet.data.model

data class Card(
    val id: Long = 0L,
    val deckId: Long,
    val front: String,
    val back: String,
    val cardType: CardType = CardType.CLASSIC,
    /** Pipe-delimited wrong choices for MULTIPLE_CHOICE; audio path for LISTENING; empty otherwise. */
    val extraData: String = "",
    /** Absolute path to an optional pronunciation audio clip (.m4a). Available on all card types. */
    val pronunciationPath: String = "",
    val createdAt: Long = 0L,
    val seenCount: Int = 0
)
