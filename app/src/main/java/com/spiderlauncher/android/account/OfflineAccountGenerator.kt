package com.spiderlauncher.android.account

import java.util.UUID

object OfflineAccountGenerator {

    fun create(
        username: String
    ): Account {

        return Account(
            username = username,
            uuid = UUID.randomUUID().toString(),
            accessToken = "offline",
            accountType = "offline"
        )
    }
}
