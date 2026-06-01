package com.spiderlauncher.android.instance

data class LauncherInstance(
    val name: String,
    val version: String,
    val ramMB: Int
)

object InstanceManager {

    private val instances =
        mutableListOf<LauncherInstance>()

    fun add(
        instance: LauncherInstance
    ) {
        instances.add(instance)
    }

    fun getAll():
            List<LauncherInstance> {

        return instances
    }

    fun remove(
        name: String
    ) {

        instances.removeAll {
            it.name == name
        }
    }
}
