package com.lycoris.lycosheet.android.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object DeckDetail : Screen("deck/{deckId}") {
        fun createRoute(deckId: Long) = "deck/$deckId"
    }
    data object Study : Screen("study/{deckId}") {
        fun createRoute(deckId: Long) = "study/$deckId"
    }
    data object Settings : Screen("settings")
}
