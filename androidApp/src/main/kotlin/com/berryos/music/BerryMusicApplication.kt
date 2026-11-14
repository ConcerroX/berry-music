package com.berryos.music

import android.app.Application
import com.berryos.music.app.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BerryMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BerryMusicApplication)
            modules(appModules)
        }
    }
}
