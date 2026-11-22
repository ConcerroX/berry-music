package com.berryos.music.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

private const val DATABASE_NAME = "berry.db"

fun createBerryDatabase(context: Context): BerryDatabase {
    val appContext = context.applicationContext
    return Room.databaseBuilder<BerryDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}
