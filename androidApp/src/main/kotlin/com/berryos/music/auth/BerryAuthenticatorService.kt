package com.berryos.music.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BerryAuthenticatorService : Service() {
    private lateinit var authenticator: BerryAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = BerryAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder = authenticator.iBinder
}
