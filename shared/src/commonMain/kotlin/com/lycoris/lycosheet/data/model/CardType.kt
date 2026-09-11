package com.lycoris.lycosheet.data.model

enum class CardType {
    CLASSIC,          // tap-to-flip front ↔ back
    MULTIPLE_CHOICE,  // question + 4 shuffled choices (1 correct, up to 3 wrong in extraData)
    FILL_IN,          // sentence prompt; user types the answer
    LISTENING         // audio clip recorded in-app; extraData = absolute file path (.m4a)
}
