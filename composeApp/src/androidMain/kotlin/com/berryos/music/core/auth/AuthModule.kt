package com.berryos.music.core.auth

import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule = module {
    single { AndroidLogtoAuthController(androidApplication()) } bind AuthController::class
}
