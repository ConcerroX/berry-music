package com.berryos.music.auth

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.berryos.music.R
import com.berryos.music.core.auth.AndroidLogtoAuthController
import com.berryos.music.core.auth.AuthState
import com.berryos.music.core.auth.BERRY_ACCOUNT_TYPE
import com.berryos.music.core.auth.ServerAccount
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BerryAddAccountActivity :
    ComponentActivity(),
    KoinComponent {
    private var response: AccountAuthenticatorResponse? = null
    private var started = false
    private var responded = false

    private val controller: AndroidLogtoAuthController
        get() = get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        response = IntentCompat.getParcelableExtra(
            intent,
            AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
            AccountAuthenticatorResponse::class.java,
        )
        controller.attach(this)
        lifecycleScope.launch { controller.state.collect(::onAuthState) }
        controller.signIn(forceLogin = true)
    }

    override fun onDestroy() {
        if (!responded && response != null) {
            response?.onError(AccountManager.ERROR_CODE_CANCELED, getString(R.string.account_canceled))
        }
        controller.attach(null)
        super.onDestroy()
    }

    private fun onAuthState(state: AuthState) {
        if (state.signingIn) {
            started = true
            return
        }
        val error = state.error
        if (!started && error == null) return
        val account = state.activeAccount
        if (error != null) {
            respondError(error)
        } else if (account != null) {
            respondWithAccount(account)
        }
    }

    private fun respondWithAccount(account: ServerAccount) {
        val result = Bundle().apply {
            putString(AccountManager.KEY_ACCOUNT_NAME, account.username)
            putString(AccountManager.KEY_ACCOUNT_TYPE, BERRY_ACCOUNT_TYPE)
        }
        responded = true
        response?.onResult(result)
        finish()
    }

    private fun respondError(message: String) {
        responded = true
        response?.onError(AccountManager.ERROR_CODE_INVALID_RESPONSE, message)
        finish()
    }
}
