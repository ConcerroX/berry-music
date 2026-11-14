package com.berryos.music.core.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle

const val BERRY_ACCOUNT_TYPE = "Berry"

private const val KEY_ACCOUNT_ID = "accountId"

object BerrySystemAccounts {
    fun add(context: Context, account: ServerAccount) {
        val manager = AccountManager.get(context)
        val existing = find(manager, account.id)
        if (existing != null) {
            manager.setUserData(existing, KEY_ACCOUNT_ID, account.id)
            return
        }
        val systemAccount = Account(account.username, BERRY_ACCOUNT_TYPE)
        val userData = Bundle().apply { putString(KEY_ACCOUNT_ID, account.id) }
        manager.addAccountExplicitly(systemAccount, null, userData)
    }

    fun remove(context: Context, accountId: String) {
        val manager = AccountManager.get(context)
        find(manager, accountId)?.let { manager.removeAccountExplicitly(it) }
    }

    fun ids(context: Context): Set<String> {
        val manager = AccountManager.get(context)
        return manager.getAccountsByType(BERRY_ACCOUNT_TYPE).mapNotNullTo(mutableSetOf()) {
            manager.getUserData(it, KEY_ACCOUNT_ID)
        }
    }

    private fun find(manager: AccountManager, accountId: String): Account? =
        manager.getAccountsByType(BERRY_ACCOUNT_TYPE).firstOrNull {
            manager.getUserData(it, KEY_ACCOUNT_ID) == accountId
        }
}
