package com.berryos.music.core.metadata

import com.berryos.music.feature.metadata.TrackTagViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val metadataModule = module {
    single<TrackTagRepository> { AndroidTrackTagRepository(androidContext()) }
    viewModel { TrackTagViewModel(get()) }
}
