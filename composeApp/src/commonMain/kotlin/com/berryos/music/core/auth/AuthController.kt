package com.berryos.music.core.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthController {
    val state: StateFlow<AuthState>

    fun signIn(forceLogin: Boolean = false)

    fun signOut()

    fun selectAccount(accountId: String)
}
