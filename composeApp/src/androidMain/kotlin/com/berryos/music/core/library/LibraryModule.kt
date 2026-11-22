package com.berryos.music.core.library

import com.berryos.music.app.LibraryViewModel
import com.berryos.music.core.auth.AuthController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.merge
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryModule = module {
    single {
        LocalLibrarySync(
            context = androidContext(),
            dao = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
    single {
        LibraryRepository(
            listOf(
                LocalLibrarySource(get()),
                SubsonicLibrarySource(account = { get<AuthController>().state.value.activeAccount }),
            ),
        )
    }
    viewModel {
        val sync = get<LocalLibrarySync>()
        LibraryViewModel(get(), merge(mediaStoreChanges(androidContext()), sync.changes))
    }
}
