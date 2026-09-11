package com.lycoris.lycosheet.android.di

import com.lycoris.lycosheet.android.audio.AudioRecorderHelper
import com.lycoris.lycosheet.audio.AudioPlayer
import com.lycoris.lycosheet.di.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { AudioPlayer(androidContext()) }
    single { AudioRecorderHelper(androidContext()) }
}
