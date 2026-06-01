package com.spiderlauncher.android.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadQueue {

    suspend fun runTask(
        task: suspend () -> Unit
    ) {

        withContext(
            Dispatchers.IO
        ) {
            task()
        }
    }
}
