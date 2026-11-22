package com.berryos.music.core.database

import org.koin.dsl.module

val databaseModule = module {
    single { createBerryDatabase(get()) }
}
