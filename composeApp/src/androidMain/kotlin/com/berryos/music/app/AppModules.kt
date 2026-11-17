package com.berryos.music.app

import com.berryos.music.core.auth.authModule
import com.berryos.music.core.datastore.settingsModule
import com.berryos.music.core.library.libraryModule
import com.berryos.music.core.metadata.metadataModule
import com.berryos.music.core.playback.playbackModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(authModule, libraryModule, metadataModule, playbackModule, settingsModule)
