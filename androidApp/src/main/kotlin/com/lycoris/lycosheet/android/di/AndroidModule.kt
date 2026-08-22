package com.lycoris.lycosheet.android.di

import com.lycoris.lycosheet.di.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
}
