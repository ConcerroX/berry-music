package com.berryos.music.core.playback

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val playbackModule = module {
    single { MediaSessionPlaybackController(androidContext()) } bind PlaybackController::class
}
