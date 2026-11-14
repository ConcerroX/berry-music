package com.berryos.music.core.datastore

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
    single { DataStoreAppSettingsRepository(androidContext()) } bind AppSettingsRepository::class
}
