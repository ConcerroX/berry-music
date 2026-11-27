package com.berryos.music.core.auth

object AuthConfig {
    const val LOGTO_ENDPOINT = "https://auth.ninrican.xyz"
    const val LOGTO_APP_ID = "x8evc93i7eilcpvfcpo40"
    const val LOGTO_API_RESOURCE = "https://music.ninrican.xyz/api"
    const val LOGTO_REDIRECT_URI = "com.berryos.music://com.berryos.music/callback"
    const val SERVER_BASE_URL = "https://music.ninrican.xyz"

    val LOGTO_SCOPES = listOf("openid", "profile", "email", "roles")
}
