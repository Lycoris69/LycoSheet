package com.lycoris.lycosheet.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lycoris.lycosheet.db.LycoSheetDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(LycoSheetDatabase.Schema, context, "lycosheet.db")
}
