package com.spiderlauncher.android.profile

import android.content.Context

object ProfileManager {

    fun saveSelectedVersion(
        context: Context,
        version: String
    ) {

        context.getSharedPreferences(
            "spider_launcher",
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                "selected_version",
                version
            )
            .apply()
    }

    fun getSelectedVersion(
        context: Context
    ): String? {

        return context
            .getSharedPreferences(
                "spider_launcher",
                Context.MODE_PRIVATE
            )
            .getString(
                "selected_version",
                null
            )
    }
}
