package com.lycoris.lycosheet.di

import com.lycoris.lycosheet.data.repository.CardRepository
import com.lycoris.lycosheet.data.repository.DeckRepository
import com.lycoris.lycosheet.data.repository.impl.CardRepositoryImpl
import com.lycoris.lycosheet.data.repository.impl.DeckRepositoryImpl
import com.lycoris.lycosheet.db.LycoSheetDatabase
import com.lycoris.lycosheet.domain.usecase.card.CreateCardUseCase
import com.lycoris.lycosheet.domain.usecase.card.DeleteCardUseCase
import com.lycoris.lycosheet.domain.usecase.card.GetCardsForDeckUseCase
import com.lycoris.lycosheet.domain.usecase.card.UpdateCardUseCase
import com.lycoris.lycosheet.domain.usecase.deck.CreateDeckUseCase
import com.lycoris.lycosheet.domain.usecase.deck.DeleteDeckUseCase
import com.lycoris.lycosheet.domain.usecase.deck.GetAllDecksUseCase
import com.lycoris.lycosheet.domain.usecase.deck.GetDeckByIdUseCase
import com.lycoris.lycosheet.presentation.home.HomeViewModel
import com.lycoris.lycosheet.presentation.library.LibraryViewModel
import com.lycoris.lycosheet.presentation.settings.SettingsViewModel
import com.lycoris.lycosheet.presentation.study.StudyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule = module {
    // Database
    single { LycoSheetDatabase(get<DatabaseDriverFactory>().createDriver()) }

    // Repositories
    single<DeckRepository> { DeckRepositoryImpl(get()) }
    single<CardRepository> { CardRepositoryImpl(get()) }

    // Use cases – deck
    factory { GetAllDecksUseCase(get()) }
    factory { GetDeckByIdUseCase(get()) }
    factory { CreateDeckUseCase(get()) }
    factory { DeleteDeckUseCase(get()) }

    // Use cases – card
    factory { GetCardsForDeckUseCase(get()) }
    factory { CreateCardUseCase(get()) }
    factory { UpdateCardUseCase(get()) }
    factory { DeleteCardUseCase(get()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { StudyViewModel(get()) }
    viewModel { SettingsViewModel() }
}
