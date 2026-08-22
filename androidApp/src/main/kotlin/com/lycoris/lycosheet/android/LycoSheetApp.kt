package com.lycoris.lycosheet.android

import android.app.Application
import com.lycoris.lycosheet.android.di.androidModule
import com.lycoris.lycosheet.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LycoSheetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LycoSheetApp)
            modules(sharedModule, androidModule)
        }
    }
}
