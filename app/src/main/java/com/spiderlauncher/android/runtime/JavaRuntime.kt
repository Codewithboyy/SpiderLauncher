package com.spiderlauncher.android.runtime

import java.io.File

data class JavaRuntime(
    val name: String,
    val version: Int,
    val javaBinary: File
)
