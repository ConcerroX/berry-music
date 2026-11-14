package com.berryos.music.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.berryos.music.R
import com.berryos.music.core.auth.BERRY_ACCOUNT_TYPE

internal class BerryAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?,
    ): Bundle {
        val intent = Intent(context, BerryAddAccountActivity::class.java)
            .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        return Bundle().apply { putParcelable(AccountManager.KEY_INTENT, intent) }
    }

    override fun getAuthTokenLabel(authTokenType: String): String = BERRY_ACCOUNT_TYPE

    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle?,
    ): Bundle = Bundle().apply {
        putString(AccountManager.KEY_ERROR_MESSAGE, context.getString(R.string.account_tokens_inside_app))
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String?,
        options: Bundle?,
    ): Bundle? = null

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        options: Bundle?,
    ): Bundle? = null

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle? = null

    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        features: Array<out String>,
    ): Bundle = Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }
}
