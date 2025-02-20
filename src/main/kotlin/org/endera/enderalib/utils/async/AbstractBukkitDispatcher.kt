package org.endera.enderalib.utils.async

import kotlinx.coroutines.CoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

abstract class AbstractBukkitDispatcher(
    private val plugin: Plugin
) : CoroutineDispatcher() {
    protected fun runFallback(block: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            Bukkit.getScheduler().runTask(plugin, block)
        }
    }
}
