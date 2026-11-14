package com.berryos.music.core.library

import com.berryos.music.app.LibraryViewModel
import com.berryos.music.core.auth.AuthController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryModule = module {
    single {
        LibraryRepository(
            listOf(
                LocalLibrarySource(androidContext()),
                SubsonicLibrarySource(account = { get<AuthController>().state.value.activeAccount }),
            ),
        )
    }
    viewModel { LibraryViewModel(get(), mediaStoreChanges(androidContext())) }
}
