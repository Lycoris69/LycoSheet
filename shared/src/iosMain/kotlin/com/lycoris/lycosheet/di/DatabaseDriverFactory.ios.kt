package com.lycoris.lycosheet.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.lycoris.lycosheet.db.LycoSheetDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(LycoSheetDatabase.Schema, "lycosheet.db")
}
