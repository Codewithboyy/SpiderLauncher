package com.spiderlauncher.android.account

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AccountManager {

    private const val PREFS = "spider_accounts"

    fun saveAccount(
        context: Context,
        account: Account
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val accounts =
            JSONArray(
                prefs.getString(
                    "accounts",
                    "[]"
                )
            )

        val obj =
            JSONObject()

        obj.put(
            "username",
            account.username
        )

        obj.put(
            "uuid",
            account.uuid
        )

        obj.put(
            "token",
            account.accessToken
        )

        obj.put(
            "type",
            account.accountType
        )

        accounts.put(obj)

        prefs.edit()
            .putString(
                "accounts",
                accounts.toString()
            )
            .apply()
    }

    fun getAccounts(
        context: Context
    ): List<Account> {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val accounts =
            JSONArray(
                prefs.getString(
                    "accounts",
                    "[]"
                )
            )

        val result =
            mutableListOf<Account>()

        for (i in 0 until accounts.length()) {

            val obj =
                accounts.getJSONObject(i)

            result.add(
                Account(
                    username = obj.getString("username"),
                    uuid = obj.getString("uuid"),
                    accessToken = obj.getString("token"),
                    accountType = obj.getString("type")
                )
            )
        }

        return result
    }
}
