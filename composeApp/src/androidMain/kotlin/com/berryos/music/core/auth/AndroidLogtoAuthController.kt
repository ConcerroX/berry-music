package com.berryos.music.core.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.logto.sdk.android.LogtoClient
import io.logto.sdk.android.type.LogtoConfig
import io.logto.sdk.android.type.SignInOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.coroutines.resume

@Suppress("TooManyFunctions")
class AndroidLogtoAuthController(private val activity: Activity) : AuthController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val serverAuth = ServerAuthApi(httpClient, AuthConfig.SERVER_BASE_URL)
    private val appContext = activity.application
    private val accountStore = AccountStore(appContext)
    private val logtoClient = LogtoClient(
        LogtoConfig(
            endpoint = AuthConfig.LOGTO_ENDPOINT,
            appId = AuthConfig.LOGTO_APP_ID,
            scopes = AuthConfig.LOGTO_SCOPES,
            resources = listOf(AuthConfig.LOGTO_API_RESOURCE),
            usingPersistStorage = true,
        ),
        activity.application as Application,
    )

    private val _state = MutableStateFlow(restoreState())
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    private var refreshing = false

    init {
        refreshAccount()
    }

    fun refreshAccount() {
        if (refreshing || !logtoClient.isAuthenticated) return
        refreshing = true
        logtoClient.getAccessToken(AuthConfig.LOGTO_API_RESOURCE) { exception, accessToken ->
            if (exception != null || accessToken == null) {
                refreshing = false
                return@getAccessToken
            }
            scope.launch {
                val account = runCatching {
                    val (name, avatar) = idTokenProfile()
                    serverAuth.exchange(accessToken.token, name, avatar)
                }.getOrNull()
                if (account != null) storeAccount(account, makeActive = _state.value.accounts.isEmpty())
                refreshing = false
            }
        }
    }

    private suspend fun idTokenProfile(): Pair<String?, String?> = suspendCancellableCoroutine { continuation ->
        logtoClient.getIdTokenClaims { exception, claims ->
            if (continuation.isActive) {
                val profile = if (exception == null) claims?.name to claims?.picture else null to null
                continuation.resume(profile)
            }
        }
    }

    override fun signIn(forceLogin: Boolean) {
        if (_state.value.signingIn) return
        if (AuthConfig.LOGTO_APP_ID.isBlank()) {
            update(_state.value.copy(error = "Logto Native app ID is not configured"))
            return
        }
        update(_state.value.copy(signingIn = true, error = null))
        val options = SignInOptions(
            redirectUri = AuthConfig.LOGTO_REDIRECT_URI,
            prompt = if (forceLogin || _state.value.accounts.isNotEmpty()) PROMPT_LOGIN else null,
        )
        logtoClient.signIn(activity, options) { exception ->
            Log.d("BerryAuth", "signIn completed: $exception")
            if (exception != null) {
                update(_state.value.copy(signingIn = false, error = exception.message ?: "Logto sign-in failed"))
            } else {
                exchangeAccessToken()
            }
        }
    }

    override fun signOut() {
        val active = _state.value.activeAccount ?: return
        removeAccount(active.id)
    }

    override fun selectAccount(accountId: String) {
        val current = _state.value
        if (current.accounts.none { it.id == accountId }) return
        if (current.activeAccountId == accountId && !current.localMode) return
        update(current.copy(activeAccountId = accountId, localMode = false))
    }

    override fun selectLocal() {
        val current = _state.value
        if (current.localMode) return
        update(current.copy(localMode = true))
    }

    fun release() {
        scope.cancel()
        httpClient.close()
    }

    private fun removeAccount(accountId: String) {
        val current = _state.value
        val remaining = current.accounts.filterNot { it.id == accountId }
        if (remaining.size == current.accounts.size) return
        BerrySystemAccounts.remove(appContext, accountId)
        val activeId = if (current.activeAccountId == accountId) {
            remaining.firstOrNull()?.id
        } else {
            current.activeAccountId
        }
        update(current.copy(accounts = remaining, activeAccountId = activeId, localMode = remaining.isEmpty()))
        if (remaining.isEmpty()) {
            logtoClient.clearCredentials()
        }
    }

    private fun exchangeAccessToken() {
        logtoClient.getAccessToken(AuthConfig.LOGTO_API_RESOURCE) { exception, accessToken ->
            if (exception != null || accessToken == null) {
                update(
                    _state.value.copy(
                        signingIn = false,
                        error = exception?.message ?: "Logto access token unavailable",
                    ),
                )
                return@getAccessToken
            }
            scope.launch {
                val (name, avatar) = idTokenProfile()
                try {
                    storeAccount(serverAuth.exchange(accessToken.token, name, avatar), makeActive = true)
                    Log.d("BerryAuth", "exchange succeeded")
                } catch (error: ServerAuthException) {
                    Log.d("BerryAuth", "exchange failed: $error")
                    update(_state.value.copy(signingIn = false, error = error.message ?: "Server login failed"))
                } catch (error: IOException) {
                    Log.d("BerryAuth", "exchange IO failed: $error")
                    update(_state.value.copy(signingIn = false, error = error.message ?: "Unable to reach the server"))
                }
            }
        }
    }

    private fun storeAccount(account: ServerAccount, makeActive: Boolean) {
        val current = _state.value
        if (!makeActive && current.accounts.none { it.id == account.id }) return
        val accounts = if (current.accounts.any { it.id == account.id }) {
            current.accounts.map { if (it.id == account.id) account else it }
        } else {
            current.accounts + account
        }
        if (makeActive) BerrySystemAccounts.add(appContext, account)
        val activeId = if (makeActive) account.id else current.activeAccountId
        update(
            current.copy(
                accounts = accounts,
                activeAccountId = activeId,
                localMode = if (makeActive) false else current.localMode,
                signingIn = false,
                error = null,
            ),
        )
    }

    private fun restoreState(): AuthState {
        val stored = accountStore.load()
        if (stored.accounts.isEmpty()) return stored
        val systemIds = BerrySystemAccounts.ids(appContext)
        val accounts = if (systemIds.isEmpty()) {
            stored.accounts.forEach { BerrySystemAccounts.add(appContext, it) }
            stored.accounts
        } else {
            stored.accounts.filter { it.id in systemIds }
        }
        val activeId = stored.activeAccountId?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id
        return stored.copy(accounts = accounts, activeAccountId = activeId)
    }

    private fun update(newState: AuthState) {
        _state.value = newState
        accountStore.save(newState)
    }
}

private const val PROMPT_LOGIN = "login"

private class AccountStore(application: Application) {
    private val preferences = application.getSharedPreferences("berry_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AuthState {
        val stored = preferences.getString(KEY_STATE, null) ?: return AuthState()
        return runCatching { json.decodeFromString<StoredAuthState>(stored) }
            .map {
                AuthState(
                    accounts = it.accounts,
                    activeAccountId = it.activeAccountId,
                    localMode = it.localMode,
                )
            }
            .getOrDefault(AuthState())
    }

    fun save(state: AuthState) {
        val stored = StoredAuthState(state.accounts, state.activeAccountId, state.localMode)
        preferences.edit().putString(KEY_STATE, json.encodeToString(stored)).apply()
    }

    @Serializable
    private data class StoredAuthState(
        val accounts: List<ServerAccount> = emptyList(),
        val activeAccountId: String? = null,
        val localMode: Boolean = false,
    )

    private companion object {
        const val KEY_STATE = "state"
    }
}
