package com.berryos.music.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class LogtoExchangeResponse(
    val token: String,
    val subsonicJwt: String? = null,
    val id: String,
    val username: String,
    val name: String = "",
    val avatar: String? = null,
    val isAdmin: Boolean = false,
)

@Serializable
data class ServerAccount(
    val id: String,
    val username: String,
    val name: String = "",
    val avatarUrl: String? = null,
    val isAdmin: Boolean = false,
    val token: String,
    val subsonicJwt: String? = null,
) {
    val displayName: String
        get() = name.ifBlank { username }
}

data class AuthState(
    val accounts: List<ServerAccount> = emptyList(),
    val activeAccountId: String? = null,
    val signingIn: Boolean = false,
    val error: String? = null,
) {
    val activeAccount: ServerAccount?
        get() = accounts.firstOrNull { it.id == activeAccountId }
}
